package org.example.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SampleTest {

    @Test
    @DisplayName("시료를 정상 생성한다")
    void 시료를_정상_생성한다() {
        Sample sample = new Sample("S-001", "실리콘 웨이퍼", 0.5, 0.92, 480);

        assertEquals("S-001", sample.getSampleId());
        assertEquals("실리콘 웨이퍼", sample.getName());
        assertEquals(0.5, sample.getAvgProductionTime());
        assertEquals(0.92, sample.getYield());
        assertEquals(480, sample.getStock());
    }

    @Test
    @DisplayName("재고를 추가한다")
    void 재고를_추가한다() {
        Sample sample = new Sample("S-001", "실리콘 웨이퍼", 0.5, 0.92, 100);
        sample.addStock(50);
        assertEquals(150, sample.getStock());
    }

    @Test
    @DisplayName("재고를 차감한다")
    void 재고를_차감한다() {
        Sample sample = new Sample("S-001", "실리콘 웨이퍼", 0.5, 0.92, 100);
        sample.subtractStock(30);
        assertEquals(70, sample.getStock());
    }

    @Test
    @DisplayName("재고보다 많이 차감하면 예외가 발생한다")
    void 재고보다_많이_차감하면_예외() {
        Sample sample = new Sample("S-001", "실리콘 웨이퍼", 0.5, 0.92, 10);
        assertThrows(IllegalArgumentException.class, () -> sample.subtractStock(20));
    }

    @Test
    @DisplayName("수율이 범위 밖이면 예외가 발생한다")
    void 수율이_범위_밖이면_예외() {
        assertThrows(IllegalArgumentException.class,
            () -> new Sample("S-001", "test", 0.5, 0.0, 0));
        assertThrows(IllegalArgumentException.class,
            () -> new Sample("S-001", "test", 0.5, 1.1, 0));
    }
}
