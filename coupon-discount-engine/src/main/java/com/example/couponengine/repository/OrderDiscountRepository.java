package com.example.couponengine.repository;

import com.example.couponengine.entity.OrderDiscount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderDiscountRepository extends JpaRepository<OrderDiscount, Long> {

    /** Used for the idempotency check: has this exact coupon already been applied to this exact order? */
    Optional<OrderDiscount> findByOrder_IdAndCoupon_Id(Long orderId, Long couponId);

    /** Fetch-join to avoid N+1 when listing all discounts applied to an order. */
    @Query("select od from OrderDiscount od join fetch od.coupon where od.order.id = :orderId")
    List<OrderDiscount> findAllByOrderIdWithCoupon(@Param("orderId") Long orderId);
}
