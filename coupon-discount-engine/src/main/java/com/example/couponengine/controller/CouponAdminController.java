package com.example.couponengine.controller;

import com.example.couponengine.entity.Coupon;
import com.example.couponengine.exception.ResourceNotFoundException;
import com.example.couponengine.repository.CouponRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/v1/admin/coupons")
@RequiredArgsConstructor
public class CouponAdminController {
@Autowired
    private  CouponRepository couponRepository;

    @PostMapping
    public ResponseEntity<Coupon> createCoupon(@Valid @RequestBody Coupon coupon) {
        Coupon saved = couponRepository.save(coupon);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public List<Coupon> listCoupons() {
        return couponRepository.findAll();
    }

    @GetMapping("/{code}")
    public Coupon getCoupon(@PathVariable String code) {
        return couponRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found: " + code));
    }
}
