package org.example.model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

public final class Order {

    private final String orderId;
    private final String sampleId;
    private final String customerName;
    private final int quantity;
    private OrderStatus status;
    private final LocalDateTime createdAt;

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
        OrderStatus.RESERVED,  Set.of(OrderStatus.CONFIRMED, OrderStatus.PRODUCING, OrderStatus.REJECTED),
        OrderStatus.PRODUCING, Set.of(OrderStatus.CONFIRMED),
        OrderStatus.CONFIRMED, Set.of(OrderStatus.RELEASE)
    );

    public Order(String orderId, String sampleId, String customerName,
                 int quantity, OrderStatus status, LocalDateTime createdAt) {
        this.orderId = orderId;
        this.sampleId = sampleId;
        this.customerName = customerName;
        this.quantity = quantity;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getOrderId() { return orderId; }
    public String getSampleId() { return sampleId; }
    public String getCustomerName() { return customerName; }
    public int getQuantity() { return quantity; }
    public OrderStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void changeStatus(OrderStatus newStatus) {
        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(this.status, Set.of());
        if (!allowed.contains(newStatus)) {
            throw new IllegalStateException(
                String.format("전이 불가: %s → %s", this.status, newStatus));
        }
        this.status = newStatus;
    }
}
