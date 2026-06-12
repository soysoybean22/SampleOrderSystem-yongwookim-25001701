package org.example.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ProductionJobTest {

    private static final LocalDateTime ENQUEUED = LocalDateTime.of(2026, 1, 1, 9, 0, 0);
    private static final LocalDateTime STARTED  = LocalDateTime.of(2026, 1, 1, 9, 0, 0);

    @Test
    @DisplayName("생산 작업을 정상 생성한다")
    void 생산작업을_정상_생성한다() {
        ProductionJob job = new ProductionJob("ORD-001", "S-001", 170, 206, 164.8, ENQUEUED, STARTED);

        assertEquals("ORD-001", job.getOrderId());
        assertEquals("S-001", job.getSampleId());
        assertEquals(170, job.getShortage());
        assertEquals(206, job.getActualProductionQty());
        assertEquals(164.8, job.getTotalProductionTime());
        assertEquals(ENQUEUED, job.getEnqueuedAt());
        assertEquals(STARTED, job.getStartedAt());
    }

    @Test
    @DisplayName("progressRatio()는 경과 시간이 0이면 0을 반환한다")
    void progressRatio_경과시간_0() {
        ProductionJob job = new ProductionJob("ORD-001", "S-001", 10, 10, 60.0, ENQUEUED, STARTED);

        assertEquals(0, job.progressRatio(STARTED));
    }

    @Test
    @DisplayName("progressRatio()는 절반 경과 시 50을 반환한다")
    void progressRatio_절반경과_50() {
        ProductionJob job = new ProductionJob("ORD-001", "S-001", 10, 10, 60.0, ENQUEUED, STARTED);

        assertEquals(50, job.progressRatio(STARTED.plusMinutes(30)));
    }

    @Test
    @DisplayName("progressRatio()는 총 시간 초과 시 100을 반환한다")
    void progressRatio_시간초과_100() {
        ProductionJob job = new ProductionJob("ORD-001", "S-001", 10, 10, 60.0, ENQUEUED, STARTED);

        assertEquals(100, job.progressRatio(STARTED.plusMinutes(120)));
    }

    @Test
    @DisplayName("progressRatio()는 startedAt이 null이면 enqueuedAt을 사용한다")
    void progressRatio_startedAt_null_폴백() {
        ProductionJob job = new ProductionJob("ORD-001", "S-001", 10, 10, 60.0, ENQUEUED, null);

        assertEquals(50, job.progressRatio(ENQUEUED.plusMinutes(30)));
    }
}
