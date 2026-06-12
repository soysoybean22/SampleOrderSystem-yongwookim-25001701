package org.example.controller;

import org.example.model.OrderStatus;
import org.example.model.ProductionJob;
import org.example.model.Sample;
import org.example.repository.OrderRepository;
import org.example.repository.ProductionJobRepository;
import org.example.repository.SampleRepository;

import java.util.List;
import java.util.Optional;

public final class ProductionController {

    private final ProductionJobRepository productionJobRepository;
    private final SampleRepository sampleRepository;
    private final OrderRepository orderRepository;

    public ProductionController(ProductionJobRepository productionJobRepository,
                                SampleRepository sampleRepository,
                                OrderRepository orderRepository) {
        this.productionJobRepository = productionJobRepository;
        this.sampleRepository = sampleRepository;
        this.orderRepository = orderRepository;
    }

    public List<ProductionJob> getQueue() {
        return productionJobRepository.findAll();
    }

    public Optional<ProductionJob> getCurrentJob() {
        return productionJobRepository.findFirst();
    }

    public ProductionJob completeProduction() {
        ProductionJob job = productionJobRepository.findFirst()
            .orElseThrow(() -> new IllegalStateException("처리할 생산 작업이 없습니다."));

        Sample sample = sampleRepository.findById(job.getSampleId()).orElseThrow(
            () -> new IllegalArgumentException("시료를 찾을 수 없습니다: " + job.getSampleId()));

        int newStock = sample.getStock() + job.getActualProductionQty();
        sampleRepository.updateStock(job.getSampleId(), newStock);
        orderRepository.updateStatus(job.getOrderId(), OrderStatus.CONFIRMED);
        productionJobRepository.deleteByOrderId(job.getOrderId());

        return job;
    }
}
