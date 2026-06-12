package org.example.repository;

import org.example.model.ProductionJob;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ProductionJobRepositoryTest {

    private ProductionJobRepository repository;

    @BeforeEach
    void setUp() throws IOException {
        JsonFileStorage.DATA_DIR = "test-data";
        Files.deleteIfExists(Path.of("test-data", "production_jobs.json"));
        repository = new ProductionJobRepository();
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(Path.of("test-data", "production_jobs.json"));
        JsonFileStorage.DATA_DIR = "data";
    }

    @Test
    @DisplayName("생산 작업을 저장하고 조회한다")
    void 생산작업을_저장하고_조회한다() {
        ProductionJob job = new ProductionJob(
            "ORD-0001", "S-001", 170, 206, 164.8,
            LocalDateTime.of(2026, 4, 16, 9, 30, 0), LocalDateTime.of(2026, 4, 16, 9, 30, 0));
        repository.save(job);

        ProductionJob found = repository.findByOrderId("ORD-0001").orElseThrow();
        assertEquals("ORD-0001", found.getOrderId());
        assertEquals(206, found.getActualProductionQty());
    }

    @Test
    @DisplayName("FIFO 순서로 조회한다")
    void FIFO_순서로_조회한다() {
        repository.save(new ProductionJob("ORD-0001", "S-001", 100, 120, 60.0,
            LocalDateTime.of(2026, 4, 16, 9, 0, 0), LocalDateTime.of(2026, 4, 16, 9, 0, 0)));
        repository.save(new ProductionJob("ORD-0002", "S-002", 50, 60, 18.0,
            LocalDateTime.of(2026, 4, 16, 10, 0, 0), null));

        assertEquals("ORD-0001", repository.findFirst().orElseThrow().getOrderId());
    }

    @Test
    @DisplayName("작업을 삭제한다")
    void 작업을_삭제한다() {
        repository.save(new ProductionJob("ORD-0001", "S-001", 100, 120, 60.0,
            LocalDateTime.of(2026, 4, 16, 9, 0, 0), LocalDateTime.of(2026, 4, 16, 9, 0, 0)));
        repository.deleteByOrderId("ORD-0001");

        assertTrue(repository.findByOrderId("ORD-0001").isEmpty());
    }
}
