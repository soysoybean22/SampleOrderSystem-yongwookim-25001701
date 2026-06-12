package org.example.repository;

import org.example.model.ProductionJob;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public final class ProductionJobRepository {

    private static final String FILE = "production_jobs.json";

    public List<ProductionJob> findAll() {
        return JsonFileStorage.parseArray(JsonFileStorage.read(FILE))
            .stream()
            .map(this::fromMap)
            .sorted(Comparator.comparing(ProductionJob::getEnqueuedAt))
            .collect(Collectors.toList());
    }

    public Optional<ProductionJob> findFirst() {
        return findAll().stream().findFirst();
    }

    public Optional<ProductionJob> findByOrderId(String orderId) {
        return findAll().stream().filter(j -> j.getOrderId().equals(orderId)).findFirst();
    }

    public void save(ProductionJob job) {
        List<Map<String, String>> rows = JsonFileStorage.parseArray(JsonFileStorage.read(FILE));
        rows.add(toMap(job));
        JsonFileStorage.write(FILE, JsonFileStorage.toJsonArray(rows));
    }

    public void deleteByOrderId(String orderId) {
        List<Map<String, String>> rows = JsonFileStorage.parseArray(JsonFileStorage.read(FILE));
        rows.removeIf(row -> row.get("orderId").equals(orderId));
        JsonFileStorage.write(FILE, JsonFileStorage.toJsonArray(rows));
    }

    private ProductionJob fromMap(Map<String, String> map) {
        return new ProductionJob(
            map.get("orderId"),
            map.get("sampleId"),
            Integer.parseInt(map.get("shortage")),
            Integer.parseInt(map.get("actualProductionQty")),
            Double.parseDouble(map.get("totalProductionTime")),
            LocalDateTime.parse(map.get("enqueuedAt"))
        );
    }

    private Map<String, String> toMap(ProductionJob job) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("orderId", job.getOrderId());
        map.put("sampleId", job.getSampleId());
        map.put("shortage", String.valueOf(job.getShortage()));
        map.put("actualProductionQty", String.valueOf(job.getActualProductionQty()));
        map.put("totalProductionTime", String.valueOf(job.getTotalProductionTime()));
        map.put("enqueuedAt", job.getEnqueuedAt().toString());
        return map;
    }
}
