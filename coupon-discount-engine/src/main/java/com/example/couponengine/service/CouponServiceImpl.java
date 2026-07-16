package com.example.couponengine.service;

import com.example.couponengine.dto.ApplyCouponRequest;
import com.example.couponengine.dto.ApplyCouponResponse;
import com.example.couponengine.entity.*;
import com.example.couponengine.exception.*;
import com.example.couponengine.repository.*;
import com.example.couponengine.strategy.DiscountStrategy;
import com.example.couponengine.strategy.DiscountStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {
@Autowired
    private  OrderRepository orderRepository;
@Autowired
    private  CouponRepository couponRepository;
@Autowired
    private  UserCouponRepository userCouponRepository;
@Autowired
    private  OrderDiscountRepository orderDiscountRepository;
@Autowired
    private  DiscountStrategyFactory strategyFactory;

    @Override
    @Transactional
    public ApplyCouponResponse applyCoupons(ApplyCouponRequest request) {
       
        List<String> codes = request.getCouponCodes().stream()
                .distinct()
                .sorted()
                .toList();

        
        Order order = orderRepository.findByIdForUpdate(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + request.getOrderId()));

        if (!order.getUserId().equals(request.getUserId())) {
            throw new InvalidCouponException("Order does not belong to the requesting user");
        }

        
        if (codes.size() > 1) {
            validateCombination(codes);
        }

        List<ApplyCouponResponse.AppliedCouponDetail> details = new ArrayList<>();
        BigDecimal totalDiscount = BigDecimal.ZERO;

        for (String code : codes) {
            ApplyCouponResponse.AppliedCouponDetail detail = applySingleCoupon(order, request.getUserId(), code);
            details.add(detail);
            totalDiscount = totalDiscount.add(detail.getDiscountApplied());
        }

        order.setStatus(Order.OrderStatus.DISCOUNT_APPLIED);
        orderRepository.save(order);

        BigDecimal finalAmount = order.getTotalAmount().subtract(totalDiscount).max(BigDecimal.ZERO);

        return ApplyCouponResponse.builder()
                .orderId(order.getId())
                .originalAmount(order.getTotalAmount())
                .totalDiscount(totalDiscount)
                .finalAmount(finalAmount)
                .appliedCoupons(details)
                .build();
    }

   
    private ApplyCouponResponse.AppliedCouponDetail applySingleCoupon(Order order, Long userId, String code) {
      
        Coupon coupon = couponRepository.findByCodeForUpdate(code)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found: " + code));

       
        var existing = orderDiscountRepository.findByOrder_IdAndCoupon_Id(order.getId(), coupon.getId());
        if (existing.isPresent()) {
            log.info("Coupon {} already applied to order {} - returning existing result (idempotent)", code, order.getId());
            return ApplyCouponResponse.AppliedCouponDetail.builder()
                    .couponCode(code)
                    .discountApplied(existing.get().getDiscountApplied())
                    .alreadyApplied(true)
                    .build();
        }

       
        if (!coupon.isActive()) {
            throw new InvalidCouponException("Coupon is not active: " + code);
        }
        if (coupon.isExpired()) {
            throw new CouponExpiredException("Coupon has expired: " + code);
        }
        if (!coupon.hasUsageRemaining()) {
            throw new CouponUsageLimitExceededException("Coupon usage limit reached: " + code);
        }
        if (coupon.getApplicableCategory() != null
                && order.getCategory() != null
                && !coupon.getApplicableCategory().equalsIgnoreCase(order.getCategory())
                && coupon.getType() != CouponType.CONDITIONAL) {
         
            throw new InvalidCouponException("Coupon " + code + " is not applicable to category " + order.getCategory());
        }

    
        UserCoupon userCoupon = null;
        if (coupon.isSingleUsePerUser()) {
            userCoupon = getOrCreateUserCouponForUpdate(userId, coupon);
            if (userCoupon.isUsedFlag()) {
                throw new CouponAlreadyUsedException("Coupon " + code + " has already been used by this user");
            }
        }


        DiscountStrategy strategy = strategyFactory.resolve(coupon.getType());
        BigDecimal discount = strategy.calculateDiscount(coupon, order);

        OrderDiscount orderDiscount = OrderDiscount.builder()
                .order(order)
                .coupon(coupon)
                .discountApplied(discount)
                .appliedAt(LocalDateTime.now())
                .build();
        try {
            orderDiscountRepository.save(orderDiscount);
        } catch (DataIntegrityViolationException dup) {
           
            Coupon finalCoupon = coupon;
            OrderDiscount already = orderDiscountRepository.findByOrder_IdAndCoupon_Id(order.getId(), coupon.getId())
                    .orElseThrow(() -> dup);
            return ApplyCouponResponse.AppliedCouponDetail.builder()
                    .couponCode(code)
                    .discountApplied(already.getDiscountApplied())
                    .alreadyApplied(true)
                    .build();
        }

        coupon.setUsageCount(coupon.getUsageCount() + 1);
        couponRepository.save(coupon);

        if (userCoupon != null) {
            userCoupon.setUsedFlag(true);
            userCoupon.setUsedAt(LocalDateTime.now());
            userCouponRepository.save(userCoupon);
        }

        return ApplyCouponResponse.AppliedCouponDetail.builder()
                .couponCode(code)
                .discountApplied(discount)
                .alreadyApplied(false)
                .build();
    }

   
    private UserCoupon getOrCreateUserCouponForUpdate(Long userId, Coupon coupon) {
        var existing = userCouponRepository.findByUserIdAndCouponIdForUpdate(userId, coupon.getId());
        if (existing.isPresent()) {
            return existing.get();
        }
        try {
            UserCoupon created = UserCoupon.builder()
                    .userId(userId)
                    .coupon(coupon)
                    .usedFlag(false)
                    .build();
            return userCouponRepository.saveAndFlush(created);
        } catch (DataIntegrityViolationException race) {
            return userCouponRepository.findByUserIdAndCouponIdForUpdate(userId, coupon.getId())
                    .orElseThrow(() -> race);
        }
    }

    
    private void validateCombination(List<String> codes) {
        List<Coupon> coupons = couponRepository.findAllByCodeIn(codes);
        if (coupons.size() != codes.size()) {
            Set<String> found = coupons.stream().map(Coupon::getCode).collect(Collectors.toSet());
            List<String> missing = codes.stream().filter(c -> !found.contains(c)).toList();
            throw new ResourceNotFoundException("Coupon(s) not found: " + missing);
        }

        boolean allStackable = coupons.stream().allMatch(Coupon::isStackable);
        if (!allStackable) {
            throw new InvalidCouponCombinationException(
                    "One or more coupons in this request cannot be combined with others: " + codes);
        }

        long distinctTypes = coupons.stream().map(Coupon::getType).distinct().count();
        if (distinctTypes != coupons.size()) {
            throw new InvalidCouponCombinationException(
                    "Cannot combine two coupons of the same discount type in one request: " + codes);
        }
    }
}
