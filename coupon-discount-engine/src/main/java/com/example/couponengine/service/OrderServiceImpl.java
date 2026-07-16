package com.example.couponengine.service;

import com.example.couponengine.entity.Order;
import com.example.couponengine.exception.ResourceNotFoundException;
import com.example.couponengine.repository.OrderRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
@Autowired
    private OrderRepository orderRepository;

    @Override
    @Transactional
    public Order createOrder(Order order) {
        order.setStatus(Order.OrderStatus.CREATED);
        return orderRepository.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrderWithDiscounts(Long orderId) {
        // Uses the @EntityGraph fetch-join query - loads discounts + their coupons
        // in a single query instead of triggering N+1 lazy loads.
        return orderRepository.findByIdWithDiscounts(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }
}
