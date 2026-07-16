package com.example.couponengine.service;

import com.example.couponengine.dto.ApplyCouponRequest;
import com.example.couponengine.dto.ApplyCouponResponse;

public interface CouponService {

    /**
     * Applies one or more coupons to an order. Idempotent: calling this
     * multiple times with the same (orderId, couponCode) pair will not
     * double-apply the discount.
     */
    ApplyCouponResponse applyCoupons(ApplyCouponRequest request);
}
