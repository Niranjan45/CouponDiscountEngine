package com.example.couponengine.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records that a specific coupon was applied to a specific order.
 *
 * The unique constraint on (order_id, coupon_id) is the backbone of
 * IDEMPOTENCY: if the same "apply coupon X to order Y" request is retried
 * (client timeout + retry, double-click, duplicate message from a queue,
 * etc.) the second attempt will find the existing row and short-circuit
 * instead of double-applying the discount.
 */
@Entity
@Table(name = "order_discounts", uniqueConstraints = {
        @UniqueConstraint(name = "uq_order_coupon", columnNames = {"order_id", "coupon_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDiscount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Column(name = "discount_applied", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountApplied;

    @Builder.Default
    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt = LocalDateTime.now();
}
