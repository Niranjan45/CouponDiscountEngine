package com.example.couponengine.strategy;

import com.example.couponengine.entity.Coupon;
import com.example.couponengine.entity.CouponType;
import com.example.couponengine.entity.Order;

import java.math.BigDecimal;

/**
 * Strategy Pattern contract for discount calculation.
 *
 * Adding a new coupon type is a matter of:
 *   1. Adding a value to {@link CouponType}
 *   2. Writing a new @Component implementing this interface
 * No existing code needs to change (Open/Closed Principle) - the
 * DiscountStrategyFactory auto-discovers all beans of this type.
 */
public interface DiscountStrategy {

    boolean supports(CouponType type);

    /**
     * Calculates the discount amount for the given coupon + order.
     * Implementations must also validate any type-specific business rules
     * (e.g. minimum order value for CONDITIONAL) and throw
     * InvalidCouponException if they are not met.
     */
    BigDecimal calculateDiscount(Coupon coupon, Order order);
}
