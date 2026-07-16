package com.example.couponengine.service;

import com.example.couponengine.entity.Order;

public interface OrderService {
    Order createOrder(Order order);
    Order getOrderWithDiscounts(Long orderId);
}
