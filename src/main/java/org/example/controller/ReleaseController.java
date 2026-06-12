package org.example.controller;

import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.repository.OrderRepository;

import java.util.List;

public final class ReleaseController {

    private final OrderRepository orderRepository;

    public ReleaseController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<Order> findConfirmedOrders() {
        return orderRepository.findByStatus(OrderStatus.CONFIRMED);
    }

    public Order release(String orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("CONFIRMED 상태의 주문만 출고할 수 있습니다: " + order.getStatus());
        }

        orderRepository.updateStatus(orderId, OrderStatus.RELEASE);
        order.changeStatus(OrderStatus.RELEASE);
        return order;
    }
}
