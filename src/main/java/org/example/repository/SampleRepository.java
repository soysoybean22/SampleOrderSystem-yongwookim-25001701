package org.example.repository;

import org.example.model.Sample;

import java.util.*;
import java.util.stream.Collectors;

public final class SampleRepository {

    private static final String FILE = "samples.json";

    public List<Sample> findAll() {
        return JsonFileStorage.parseArray(JsonFileStorage.read(FILE))
            .stream().map(this::fromMap).collect(Collectors.toList());
    }

    public Optional<Sample> findById(String sampleId) {
        return findAll().stream().filter(s -> s.getSampleId().equals(sampleId)).findFirst();
    }

    public List<Sample> findByNameContaining(String keyword) {
        return findAll().stream()
            .filter(s -> s.getName().toLowerCase().contains(keyword.toLowerCase()))
            .collect(Collectors.toList());
    }

    public void save(Sample sample) {
        if (existsById(sample.getSampleId())) {
            throw new IllegalArgumentException("이미 존재하는 시료 ID: " + sample.getSampleId());
        }
        List<Map<String, String>> rows = JsonFileStorage.parseArray(JsonFileStorage.read(FILE));
        rows.add(toMap(sample));
        JsonFileStorage.write(FILE, JsonFileStorage.toJsonArray(rows));
    }

    public void updateStock(String sampleId, int newStock) {
        List<Map<String, String>> rows = JsonFileStorage.parseArray(JsonFileStorage.read(FILE));
        for (Map<String, String> row : rows) {
            if (row.get("sampleId").equals(sampleId)) {
                row.put("stock", String.valueOf(newStock));
                break;
            }
        }
        JsonFileStorage.write(FILE, JsonFileStorage.toJsonArray(rows));
    }

    public boolean existsById(String sampleId) {
        return findAll().stream().anyMatch(s -> s.getSampleId().equals(sampleId));
    }

    private Sample fromMap(Map<String, String> map) {
        return new Sample(
            map.get("sampleId"),
            map.get("name"),
            Double.parseDouble(map.get("avgProductionTime")),
            Double.parseDouble(map.get("yield")),
            Integer.parseInt(map.get("stock"))
        );
    }

    private Map<String, String> toMap(Sample sample) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("sampleId", sample.getSampleId());
        map.put("name", sample.getName());
        map.put("avgProductionTime", String.valueOf(sample.getAvgProductionTime()));
        map.put("yield", String.valueOf(sample.getYield()));
        map.put("stock", String.valueOf(sample.getStock()));
        return map;
    }
}
