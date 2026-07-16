package com.example.couponengine.strategy;

import com.example.couponengine.entity.Coupon;
import com.example.couponengine.entity.CouponType;
import com.example.couponengine.entity.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PercentageDiscountStrategy implements DiscountStrategy {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Override
    public boolean supports(CouponType type) {
        return type == CouponType.PERCENTAGE;
    }

    @Override
    public BigDecimal calculateDiscount(Coupon coupon, Order order) {
        BigDecimal raw = order.getTotalAmount()
                .multiply(coupon.getDiscountValue())
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);

        if (coupon.getMaxDiscountAmount() != null) {
            raw = raw.min(coupon.getMaxDiscountAmount());
        }
        return raw.min(order.getTotalAmount());
    }
}
