package com.example.couponengine.repository;

import com.example.couponengine.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * @EntityGraph eagerly loads the `discounts` collection (and their
     * nested `coupon`) in ONE query instead of the default N+1 lazy loads
     * that would otherwise occur when a controller/service iterates
     * order.getDiscounts() and touches discount.getCoupon().
     */
    @EntityGraph(attributePaths = {"discounts", "discounts.coupon"})
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdWithDiscounts(@Param("id") Long id);

    /**
     * Pessimistic write lock on the order row itself, taken while mutating
     * totalAmount/status during discount application, to serialize
     * concurrent "apply coupon" calls against the SAME order.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);
}
