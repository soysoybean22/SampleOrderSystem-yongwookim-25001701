package org.example.repository;

import org.example.model.Order;
import org.example.model.OrderStatus;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public final class OrderRepository {

    private static final String FILE = "orders.json";

    public List<Order> findAll() {
        return JsonFileStorage.parseArray(JsonFileStorage.read(FILE))
            .stream().map(this::fromMap).collect(Collectors.toList());
    }

    public Optional<Order> findById(String orderId) {
        return findAll().stream().filter(o -> o.getOrderId().equals(orderId)).findFirst();
    }

    public List<Order> findByStatus(OrderStatus status) {
        return findAll().stream().filter(o -> o.getStatus() == status).collect(Collectors.toList());
    }

    public void save(Order order) {
        List<Map<String, String>> rows = JsonFileStorage.parseArray(JsonFileStorage.read(FILE));
        rows.add(toMap(order));
        JsonFileStorage.write(FILE, JsonFileStorage.toJsonArray(rows));
    }

    public void updateStatus(String orderId, OrderStatus newStatus) {
        List<Map<String, String>> rows = JsonFileStorage.parseArray(JsonFileStorage.read(FILE));
        for (Map<String, String> row : rows) {
            if (row.get("orderId").equals(orderId)) {
                row.put("status", newStatus.name());
                break;
            }
        }
        JsonFileStorage.write(FILE, JsonFileStorage.toJsonArray(rows));
    }

    public int nextSequence() {
        return findAll().size() + 1;
    }

    private Order fromMap(Map<String, String> map) {
        return new Order(
            map.get("orderId"),
            map.get("sampleId"),
            map.get("customerName"),
            Integer.parseInt(map.get("quantity")),
            OrderStatus.valueOf(map.get("status")),
            LocalDateTime.parse(map.get("createdAt"))
        );
    }

    private Map<String, String> toMap(Order order) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("orderId", order.getOrderId());
        map.put("sampleId", order.getSampleId());
        map.put("customerName", order.getCustomerName());
        map.put("quantity", String.valueOf(order.getQuantity()));
        map.put("status", order.getStatus().name());
        map.put("createdAt", order.getCreatedAt().toString());
        return map;
    }
}
