package org.example.controller;

import org.example.model.ApprovalResult;
import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.model.ProductionJob;
import org.example.model.Sample;
import org.example.repository.OrderRepository;
import org.example.repository.ProductionJobRepository;
import org.example.repository.SampleRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class OrderController {

    private final OrderRepository orderRepository;
    private final SampleRepository sampleRepository;
    private final ProductionJobRepository productionJobRepository;

    public OrderController(OrderRepository orderRepository,
                           SampleRepository sampleRepository,
                           ProductionJobRepository productionJobRepository) {
        this.orderRepository = orderRepository;
        this.sampleRepository = sampleRepository;
        this.productionJobRepository = productionJobRepository;
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

    public ApprovalResult approveOrder(String orderId) {
        Order order = findOrderOrThrow(orderId);
        if (order.getStatus() != OrderStatus.RESERVED) {
            throw new IllegalStateException("RESERVED 상태의 주문만 승인할 수 있습니다: " + order.getStatus());
        }

        Sample sample = sampleRepository.findById(order.getSampleId()).orElseThrow(
            () -> new IllegalArgumentException("시료를 찾을 수 없습니다: " + order.getSampleId()));

        if (sample.getStock() >= order.getQuantity()) {
            return approveWithSufficientStock(order, sample);
        } else {
            return approveWithInsufficientStock(order, sample);
        }
    }

    public Order rejectOrder(String orderId) {
        Order order = findOrderOrThrow(orderId);
        if (order.getStatus() != OrderStatus.RESERVED) {
            throw new IllegalStateException("RESERVED 상태의 주문만 거절할 수 있습니다: " + order.getStatus());
        }
        orderRepository.updateStatus(orderId, OrderStatus.REJECTED);
        order.changeStatus(OrderStatus.REJECTED);
        return order;
    }

    public List<Order> findReservedOrders() {
        return orderRepository.findByStatus(OrderStatus.RESERVED);
    }

    public List<Order> findAllOrders() {
        return orderRepository.findAll();
    }

    private ApprovalResult approveWithSufficientStock(Order order, Sample sample) {
        int newStock = sample.getStock() - order.getQuantity();
        sampleRepository.updateStock(sample.getSampleId(), newStock);
        orderRepository.updateStatus(order.getOrderId(), OrderStatus.CONFIRMED);
        order.changeStatus(OrderStatus.CONFIRMED);
        return new ApprovalResult(order, null);
    }

    private ApprovalResult approveWithInsufficientStock(Order order, Sample sample) {
        int shortage = order.getQuantity() - sample.getStock();
        int actualQty = (int) Math.ceil(shortage / (sample.getYield() * 0.9));
        double totalTime = sample.getAvgProductionTime() * actualQty;

        ProductionJob job = new ProductionJob(
            order.getOrderId(), sample.getSampleId(),
            shortage, actualQty, totalTime, LocalDateTime.now()
        );
        productionJobRepository.save(job);
        orderRepository.updateStatus(order.getOrderId(), OrderStatus.PRODUCING);
        order.changeStatus(OrderStatus.PRODUCING);
        return new ApprovalResult(order, job);
    }

    private Order findOrderOrThrow(String orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));
    }

    private String generateOrderId() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int seq = orderRepository.nextSequence();
        return String.format("ORD-%s-%04d", date, seq);
    }
}
