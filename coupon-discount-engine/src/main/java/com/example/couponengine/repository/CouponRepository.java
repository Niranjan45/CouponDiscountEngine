package com.example.couponengine.repository;

import com.example.couponengine.entity.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);

    /**
     * Pessimistic write lock on the coupon row. Used inside the redemption
     * transaction so that "check usageCount < usageLimit, then increment"
     * is atomic across concurrent requests for the SAME coupon. Competing
     * transactions block on this row until the first one commits/rolls back,
     * which is exactly what we want for a strict global usage cap.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Coupon c where c.code = :code")
    Optional<Coupon> findByCodeForUpdate(@Param("code") String code);

    /**
     * Fetch-join variant for the multi-coupon apply flow, to avoid N+1
     * selects when validating several coupon codes at once.
     */
    @Query("select c from Coupon c where c.code in :codes")
    List<Coupon> findAllByCodeIn(@Param("codes") List<String> codes);
}
