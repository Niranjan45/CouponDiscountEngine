package com.example.couponengine.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Handles both single- and multi-coupon application - a single coupon
 * request is just a couponCodes list of size 1.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApplyCouponRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Long orderId;

    @NotEmpty
    private List<@NotNull String> couponCodes;
}
