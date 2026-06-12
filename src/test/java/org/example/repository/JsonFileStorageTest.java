package org.example.repository;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class JsonFileStorageTest {

    private static final String TEST_FILE = "storage_test.json";

    @BeforeEach
    void setUp() throws IOException {
        JsonFileStorage.DATA_DIR = "test-data";
        Files.deleteIfExists(Path.of("test-data", TEST_FILE));
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(Path.of("test-data", TEST_FILE));
        JsonFileStorage.DATA_DIR = "data";
    }

    @Test
    @DisplayName("데이터를 저장하고 불러온다")
    void 데이터를_저장하고_불러온다() {
        List<Map<String, String>> items = new ArrayList<>();
        Map<String, String> item = new LinkedHashMap<>();
        item.put("id", "T-001");
        item.put("name", "테스트 시료");
        item.put("count", "42");
        items.add(item);

        JsonFileStorage.write(TEST_FILE, JsonFileStorage.toJsonArray(items));
        List<Map<String, String>> result = JsonFileStorage.parseArray(JsonFileStorage.read(TEST_FILE));

        assertEquals(1, result.size());
        assertEquals("T-001", result.get(0).get("id"));
        assertEquals("테스트 시료", result.get(0).get("name"));
        assertEquals("42", result.get(0).get("count"));
    }

    @Test
    @DisplayName("파일이 없으면 빈 배열을 반환한다")
    void 파일이_없으면_빈_배열_반환() {
        String content = JsonFileStorage.read("nonexistent.json");
        assertEquals("[]", content);
        assertTrue(JsonFileStorage.parseArray(content).isEmpty());
    }
}
