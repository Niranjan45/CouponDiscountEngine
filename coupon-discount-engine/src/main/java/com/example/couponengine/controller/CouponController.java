package com.example.couponengine.controller;

import com.example.couponengine.dto.ApplyCouponRequest;
import com.example.couponengine.dto.ApplyCouponResponse;
import com.example.couponengine.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private CouponService couponService;

  
    @PostMapping("/apply")
    public ResponseEntity<ApplyCouponResponse> applyCoupons(@Valid @RequestBody ApplyCouponRequest request) {
        return ResponseEntity.ok(couponService.applyCoupons(request));
    }
}
