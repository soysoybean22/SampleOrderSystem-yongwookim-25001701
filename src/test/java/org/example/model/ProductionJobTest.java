package org.example.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ProductionJobTest {

    @Test
    @DisplayName("생산 작업을 정상 생성한다")
    void 생산작업을_정상_생성한다() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 16, 9, 31, 0);
        ProductionJob job = new ProductionJob("ORD-20260416-0001", "S-001", 170, 206, 164.8, now);

        assertEquals("ORD-20260416-0001", job.getOrderId());
        assertEquals("S-001", job.getSampleId());
        assertEquals(170, job.getShortage());
        assertEquals(206, job.getActualProductionQty());
        assertEquals(164.8, job.getTotalProductionTime());
        assertEquals(now, job.getEnqueuedAt());
    }
}
