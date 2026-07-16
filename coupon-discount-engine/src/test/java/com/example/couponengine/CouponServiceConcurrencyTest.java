package com.example.couponengine;

import com.example.couponengine.dto.ApplyCouponRequest;
import com.example.couponengine.dto.ApplyCouponResponse;
import com.example.couponengine.entity.Coupon;
import com.example.couponengine.entity.CouponType;
import com.example.couponengine.entity.Order;
import com.example.couponengine.repository.CouponRepository;
import com.example.couponengine.repository.OrderRepository;
import com.example.couponengine.service.CouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CouponServiceConcurrencyTest {

    @Autowired
    private CouponService couponService;
    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private OrderRepository orderRepository;

    private Coupon singleUseCoupon;
    private Order order;

    @BeforeEach
    void setUp() {
        singleUseCoupon = couponRepository.save(Coupon.builder()
                .code("TESTFLAT50-" + System.nanoTime())
                .type(CouponType.FLAT)
                .discountValue(BigDecimal.valueOf(50))
                .expiryDate(LocalDateTime.now().plusDays(1))
                .usageLimit(1) // exactly one global redemption allowed
                .usageCount(0)
                .singleUsePerUser(true)
                .stackable(false)
                .active(true)
                .build());

        order = orderRepository.save(Order.builder()
                .userId(1L)
                .totalAmount(BigDecimal.valueOf(500))
                .category("GENERAL")
                .build());
    }

    @Test
    void applyingSameCouponTwiceIsIdempotent() {
        ApplyCouponRequest request = new ApplyCouponRequest(1L, order.getId(), List.of(singleUseCoupon.getCode()));

        ApplyCouponResponse first = couponService.applyCoupons(request);
        ApplyCouponResponse second = couponService.applyCoupons(request);

        assertEquals(0, first.getTotalDiscount().compareTo(second.getTotalDiscount()));
        assertTrue(second.getAppliedCoupons().get(0).isAlreadyApplied());

        Coupon reloaded = couponRepository.findByCode(singleUseCoupon.getCode()).orElseThrow();
        assertEquals(1, reloaded.getUsageCount(), "usage count must only increment once despite two calls");
    }

    @Test
    void concurrentRedemptionsRespectGlobalUsageLimit() throws InterruptedException {
        // usageLimit = 1: only ONE of these concurrent orders should succeed in
        // redeeming the coupon; the rest must fail with a domain exception,
        // proving the pessimistic lock on the Coupon row prevents overselling.
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            final long userId = 100L + i;
            Order userOrder = orderRepository.save(Order.builder()
                    .userId(userId)
                    .totalAmount(BigDecimal.valueOf(500))
                    .category("GENERAL")
                    .build());

            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                    couponService.applyCoupons(new ApplyCouponRequest(userId, userOrder.getId(), List.of(singleUseCoupon.getCode())));
                    successes.incrementAndGet();
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                }
            });
        }

        ready.await();
        go.countDown();
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);

        assertEquals(1, successes.get(), "exactly one concurrent redemption should succeed given usageLimit=1");
        assertEquals(threads - 1, failures.get());

        Coupon reloaded = couponRepository.findByCode(singleUseCoupon.getCode()).orElseThrow();
        assertEquals(1, reloaded.getUsageCount());
    }
}
