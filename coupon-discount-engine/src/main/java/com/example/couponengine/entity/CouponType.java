package com.example.couponengine.entity;

/**
 * Supported discount types. Each maps to exactly one DiscountStrategy
 * implementation (Strategy Pattern) - see the `strategy` package.
 */
public enum CouponType {
    FLAT,          // fixed amount off, e.g. ₹100 off
    PERCENTAGE,    // percentage off, e.g. 10% off (optionally capped)
    CONDITIONAL    // percentage/flat off, only if extra business conditions hold
}
