package com.example.couponengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyCouponResponse {

    private Long orderId;
    private BigDecimal originalAmount;
    private BigDecimal totalDiscount;
    private BigDecimal finalAmount;
    private List<AppliedCouponDetail> appliedCoupons;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppliedCouponDetail {
        private String couponCode;
        private BigDecimal discountApplied;
        private boolean alreadyApplied; // true when idempotent short-circuit kicked in
    }
}
