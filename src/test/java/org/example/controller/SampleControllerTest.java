package org.example.controller;

import org.example.model.Sample;
import org.example.repository.JsonFileStorage;
import org.example.repository.SampleRepository;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SampleControllerTest {

    private SampleController controller;

    @BeforeEach
    void setUp() throws IOException {
        JsonFileStorage.setDataDir("test-data");
        Files.deleteIfExists(Path.of("test-data", "samples.json"));
        controller = new SampleController(new SampleRepository());
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(Path.of("test-data", "samples.json"));
        JsonFileStorage.setDataDir("data");
    }

    @Test
    @DisplayName("시료를 정상 등록한다")
    void 시료를_정상_등록한다() {
        controller.register("S-001", "실리콘 웨이퍼", 0.5, 0.92, 100);

        List<Sample> all = controller.findAll();
        assertEquals(1, all.size());
        assertEquals("S-001", all.get(0).getSampleId());
        assertEquals("실리콘 웨이퍼", all.get(0).getName());
    }

    @Test
    @DisplayName("중복 ID 등록 시 예외가 발생한다")
    void 중복_ID_등록_시_예외() {
        controller.register("S-001", "시료A", 0.5, 0.92, 100);
        assertThrows(IllegalArgumentException.class,
            () -> controller.register("S-001", "시료B", 0.3, 0.78, 50));
    }

    @Test
    @DisplayName("전체 시료를 조회한다")
    void 전체_시료를_조회한다() {
        controller.register("S-001", "시료A", 0.5, 0.92, 100);
        controller.register("S-002", "시료B", 0.3, 0.78, 200);

        assertEquals(2, controller.findAll().size());
    }

    @Test
    @DisplayName("이름으로 시료를 검색한다")
    void 이름으로_시료를_검색한다() {
        controller.register("S-001", "실리콘 웨이퍼-8인치", 0.5, 0.92, 100);
        controller.register("S-002", "GaN 에피택셜", 0.3, 0.78, 200);

        List<Sample> result = controller.searchByName("웨이퍼");
        assertEquals(1, result.size());
        assertEquals("S-001", result.get(0).getSampleId());
    }

    @Test
    @DisplayName("검색 결과가 없으면 빈 리스트를 반환한다")
    void 검색_결과_없으면_빈_리스트() {
        controller.register("S-001", "실리콘 웨이퍼", 0.5, 0.92, 100);

        assertTrue(controller.searchByName("존재하지않는키워드").isEmpty());
    }
}
