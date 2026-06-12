package org.example.controller;

import org.example.model.Sample;
import org.example.repository.SampleRepository;

import java.util.List;

public final class SampleController {

    private final SampleRepository repository;

    public SampleController(SampleRepository repository) {
        this.repository = repository;
    }

    public void register(String sampleId, String name,
                         double avgProductionTime, double yield, int stock) {
        repository.save(new Sample(sampleId, name, avgProductionTime, yield, stock));
    }

    public List<Sample> findAll() {
        return repository.findAll();
    }

    public List<Sample> searchByName(String keyword) {
        return repository.findByNameContaining(keyword);
    }
}
