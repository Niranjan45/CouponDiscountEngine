package com.example.couponengine.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Master coupon definition.
 *
 * `usageCount` + `usageLimit` enforce a GLOBAL cap on how many times a coupon
 * can be redeemed across all users. `@Version` gives us optimistic-locking
 * as a safety net; the service layer additionally takes a pessimistic write
 * lock on this row during redemption to make the check-then-increment
 * sequence atomic under high concurrency (see CouponRepository +
 * CouponServiceImpl).
 */
@Entity
@Table(name = "coupons", indexes = {
        @Index(name = "idx_coupon_code", columnList = "code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponType type;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    /** Only relevant for PERCENTAGE coupons - caps the absolute discount. Nullable = no cap. */
    @Column(name = "max_discount_amount", precision = 12, scale = 2)
    private BigDecimal maxDiscountAmount;

    /** Only relevant for CONDITIONAL coupons - minimum order value required. */
    @Column(name = "min_order_value", precision = 12, scale = 2)
    private BigDecimal minOrderValue;

    /** Optional category restriction, e.g. "ELECTRONICS". Null = applies to all categories. */
    @Column(name = "applicable_category", length = 50)
    private String applicableCategory;

    @NotNull
    @Future
    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    /** Global usage cap across all users. Null/<=0 usage_limit is treated as unlimited via isUnlimited(). */
    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Builder.Default
    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 0;

    /** Whether a single user may only redeem this coupon once. */
    @Builder.Default
    @Column(name = "single_use_per_user", nullable = false)
    private boolean singleUsePerUser = true;

    /** Whether this coupon may be combined with other coupons on the same order. */
    @Builder.Default
    @Column(name = "stackable", nullable = false)
    private boolean stackable = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    /** Optimistic-locking safety net in addition to the pessimistic lock taken explicitly in the service. */
    @Version
    private Long version;

    @Transient
    public boolean isUnlimited() {
        return usageLimit == null || usageLimit <= 0;
    }

    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDateTime.now());
    }

    public boolean hasUsageRemaining() {
        return isUnlimited() || usageCount < usageLimit;
    }
}
