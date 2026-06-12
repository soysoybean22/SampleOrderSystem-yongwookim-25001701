package org.example.controller;

import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.model.Sample;
import org.example.model.SampleStockInfo;
import org.example.model.StockStatus;
import org.example.repository.OrderRepository;
import org.example.repository.SampleRepository;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class MonitoringController {

    private final OrderRepository orderRepository;
    private final SampleRepository sampleRepository;

    public MonitoringController(OrderRepository orderRepository,
                                SampleRepository sampleRepository) {
        this.orderRepository = orderRepository;
        this.sampleRepository = sampleRepository;
    }

    public Map<OrderStatus, Integer> getOrderSummary() {
        List<Order> all = orderRepository.findAll();
        Map<OrderStatus, Integer> summary = new LinkedHashMap<>();
        for (OrderStatus status : Arrays.asList(
                OrderStatus.RESERVED, OrderStatus.CONFIRMED,
                OrderStatus.PRODUCING, OrderStatus.RELEASE)) {
            int count = (int) all.stream().filter(o -> o.getStatus() == status).count();
            summary.put(status, count);
        }
        return summary;
    }

    public List<SampleStockInfo> getStockStatus() {
        List<Order> activeOrders = orderRepository.findAll().stream()
            .filter(o -> o.getStatus() == OrderStatus.CONFIRMED
                      || o.getStatus() == OrderStatus.PRODUCING)
            .collect(Collectors.toList());

        return sampleRepository.findAll().stream()
            .map(sample -> buildStockInfo(sample, activeOrders))
            .collect(Collectors.toList());
    }

    private SampleStockInfo buildStockInfo(Sample sample, List<Order> activeOrders) {
        int pendingQty = activeOrders.stream()
            .filter(o -> o.getSampleId().equals(sample.getSampleId()))
            .mapToInt(Order::getQuantity)
            .sum();

        StockStatus status;
        if (sample.getStock() == 0) {
            status = StockStatus.고갈;
        } else if (sample.getStock() < pendingQty) {
            status = StockStatus.부족;
        } else {
            status = StockStatus.여유;
        }
        return new SampleStockInfo(sample, pendingQty, status);
    }
}
