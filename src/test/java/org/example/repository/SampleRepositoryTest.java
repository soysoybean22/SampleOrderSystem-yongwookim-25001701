package org.example.repository;

import org.example.model.Sample;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SampleRepositoryTest {

    private SampleRepository repository;

    @BeforeEach
    void setUp() throws IOException {
        JsonFileStorage.DATA_DIR = "test-data";
        Files.deleteIfExists(Path.of("test-data", "samples.json"));
        repository = new SampleRepository();
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(Path.of("test-data", "samples.json"));
        JsonFileStorage.DATA_DIR = "data";
    }

    @Test
    @DisplayName("시료를 저장하고 조회한다")
    void 시료를_저장하고_조회한다() {
        repository.save(new Sample("S-001", "실리콘 웨이퍼", 0.5, 0.92, 480));

        Sample found = repository.findById("S-001").orElseThrow();
        assertEquals("S-001", found.getSampleId());
        assertEquals("실리콘 웨이퍼", found.getName());
        assertEquals(480, found.getStock());
    }

    @Test
    @DisplayName("전체 목록을 조회한다")
    void 전체_목록을_조회한다() {
        repository.save(new Sample("S-001", "시료A", 0.5, 0.92, 100));
        repository.save(new Sample("S-002", "시료B", 0.3, 0.78, 200));

        assertEquals(2, repository.findAll().size());
    }

    @Test
    @DisplayName("이름으로 검색한다")
    void 이름으로_검색한다() {
        repository.save(new Sample("S-001", "실리콘 웨이퍼-8인치", 0.5, 0.92, 100));
        repository.save(new Sample("S-002", "GaN 에피택셜", 0.3, 0.78, 200));

        List<Sample> result = repository.findByNameContaining("웨이퍼");
        assertEquals(1, result.size());
        assertEquals("S-001", result.get(0).getSampleId());
    }

    @Test
    @DisplayName("중복 ID 저장 시 예외가 발생한다")
    void 중복_ID_저장시_예외() {
        repository.save(new Sample("S-001", "시료A", 0.5, 0.92, 100));
        assertThrows(IllegalArgumentException.class,
            () -> repository.save(new Sample("S-001", "시료B", 0.3, 0.78, 200)));
    }

    @Test
    @DisplayName("재고를 업데이트한다")
    void 재고를_업데이트한다() {
        repository.save(new Sample("S-001", "시료A", 0.5, 0.92, 100));
        repository.updateStock("S-001", 250);

        assertEquals(250, repository.findById("S-001").orElseThrow().getStock());
    }
}
