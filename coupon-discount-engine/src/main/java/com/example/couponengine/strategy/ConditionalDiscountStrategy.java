package com.example.couponengine.strategy;

import com.example.couponengine.entity.Coupon;
import com.example.couponengine.entity.CouponType;
import com.example.couponengine.entity.Order;
import com.example.couponengine.exception.InvalidCouponException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;


@Component
public class ConditionalDiscountStrategy implements DiscountStrategy {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Override
    public boolean supports(CouponType type) {
        return type == CouponType.CONDITIONAL;
    }

    @Override
    public BigDecimal calculateDiscount(Coupon coupon, Order order) {
        if (coupon.getMinOrderValue() != null
                && order.getTotalAmount().compareTo(coupon.getMinOrderValue()) < 0) {
            throw new InvalidCouponException(
                    "Coupon " + coupon.getCode() + " requires a minimum order value of "
                            + coupon.getMinOrderValue());
        }

        if (coupon.getApplicableCategory() != null
                && !coupon.getApplicableCategory().equalsIgnoreCase(order.getCategory())) {
            throw new InvalidCouponException(
                    "Coupon " + coupon.getCode() + " is not applicable to category " + order.getCategory());
        }

        BigDecimal raw = order.getTotalAmount()
                .multiply(coupon.getDiscountValue())
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);

        if (coupon.getMaxDiscountAmount() != null) {
            raw = raw.min(coupon.getMaxDiscountAmount());
        }
        return raw.min(order.getTotalAmount());
    }
}
