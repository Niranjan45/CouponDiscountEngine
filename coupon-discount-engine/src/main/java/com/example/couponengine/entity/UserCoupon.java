package com.example.couponengine.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tracks per-user redemption state for a coupon. The unique constraint on
 * (user_id, coupon_id) is the DB-level guarantee that prevents a single-use
 * coupon from ever having two "used" rows for the same user, even if two
 * requests race past the application-level check at the exact same instant.
 */
@Entity
@Table(name = "user_coupons", uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_coupon", columnNames = {"user_id", "coupon_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Builder.Default
    @Column(name = "used_flag", nullable = false)
    private boolean usedFlag = false;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    /** Optimistic-locking safety net for concurrent redemption attempts by the same user. */
    @Version
    private Long version;
}
