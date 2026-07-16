package com.example.couponengine.strategy;

import com.example.couponengine.entity.Coupon;
import com.example.couponengine.entity.CouponType;
import com.example.couponengine.entity.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FlatDiscountStrategy implements DiscountStrategy {

    @Override
    public boolean supports(CouponType type) {
        return type == CouponType.FLAT;
    }

    @Override
    public BigDecimal calculateDiscount(Coupon coupon, Order order) {
        BigDecimal discount = coupon.getDiscountValue();
       
        return discount.min(order.getTotalAmount());
    }
}
