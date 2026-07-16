package com.example.couponengine.repository;

import com.example.couponengine.entity.UserCoupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    /**
     * Fetch-join to pull the Coupon in the same query - avoids a second
     * lazy-load hit when the service checks coupon.isExpired() etc.
     */
    @Query("select uc from UserCoupon uc join fetch uc.coupon c where uc.userId = :userId and c.code = :code")
    Optional<UserCoupon> findByUserIdAndCouponCode(@Param("userId") Long userId, @Param("code") String code);

    /**
     * Pessimistic write lock on the specific user+coupon redemption row.
     * Locking this (narrow) row - rather than the whole Coupon - lets
     * different users redeem the same coupon concurrently while still
     * preventing the SAME user from double-redeeming it via a race.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select uc from UserCoupon uc where uc.userId = :userId and uc.coupon.id = :couponId")
    Optional<UserCoupon> findByUserIdAndCouponIdForUpdate(@Param("userId") Long userId, @Param("couponId") Long couponId);
}
