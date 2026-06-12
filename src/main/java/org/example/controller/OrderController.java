package org.example.controller;

import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.repository.OrderRepository;
import org.example.repository.SampleRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class OrderController {

    private final OrderRepository orderRepository;
    private final SampleRepository sampleRepository;

    public OrderController(OrderRepository orderRepository, SampleRepository sampleRepository) {
        this.orderRepository = orderRepository;
        this.sampleRepository = sampleRepository;
    }

    public Order placeOrder(String sampleId, String customerName, int quantity) {
        if (!sampleRepository.existsById(sampleId)) {
            throw new IllegalArgumentException("등록되지 않은 시료 ID입니다: " + sampleId);
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
        }
        Order order = new Order(
            generateOrderId(), sampleId, customerName,
            quantity, OrderStatus.RESERVED, LocalDateTime.now()
        );
        orderRepository.save(order);
        return order;
    }

    public List<Order> findReservedOrders() {
        return orderRepository.findByStatus(OrderStatus.RESERVED);
    }

    public List<Order> findAllOrders() {
        return orderRepository.findAll();
    }

    private String generateOrderId() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int seq = orderRepository.nextSequence();
        return String.format("ORD-%s-%04d", date, seq);
    }
}
