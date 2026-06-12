package org.example.repository;

import org.example.model.Order;
import org.example.model.OrderStatus;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderRepositoryTest {

    private OrderRepository repository;
    private final LocalDateTime NOW = LocalDateTime.of(2026, 4, 16, 9, 30, 0);

    @BeforeEach
    void setUp() throws IOException {
        JsonFileStorage.DATA_DIR = "test-data";
        Files.deleteIfExists(Path.of("test-data", "orders.json"));
        repository = new OrderRepository();
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(Path.of("test-data", "orders.json"));
        JsonFileStorage.DATA_DIR = "data";
    }

    private Order order(String id, String sampleId, String customer, int qty, OrderStatus status) {
        return new Order(id, sampleId, customer, qty, status, NOW);
    }

    @Test
    @DisplayName("주문을 저장하고 조회한다")
    void 주문을_저장하고_조회한다() {
        repository.save(order("ORD-20260416-0001", "S-001", "LG이노텍", 300, OrderStatus.RESERVED));

        Order found = repository.findById("ORD-20260416-0001").orElseThrow();
        assertEquals("LG이노텍", found.getCustomerName());
        assertEquals(OrderStatus.RESERVED, found.getStatus());
    }

    @Test
    @DisplayName("상태별 주문을 조회한다")
    void 상태별_주문을_조회한다() {
        repository.save(order("ORD-0001", "S-001", "고객A", 100, OrderStatus.RESERVED));
        repository.save(order("ORD-0002", "S-002", "고객B", 200, OrderStatus.CONFIRMED));
        repository.save(order("ORD-0003", "S-001", "고객C", 150, OrderStatus.RESERVED));

        List<Order> reserved = repository.findByStatus(OrderStatus.RESERVED);
        assertEquals(2, reserved.size());
    }

    @Test
    @DisplayName("상태를 업데이트한다")
    void 상태를_업데이트한다() {
        repository.save(order("ORD-0001", "S-001", "고객A", 100, OrderStatus.RESERVED));
        repository.updateStatus("ORD-0001", OrderStatus.CONFIRMED);

        assertEquals(OrderStatus.CONFIRMED, repository.findById("ORD-0001").orElseThrow().getStatus());
    }

    @Test
    @DisplayName("일련번호가 순차 증가한다")
    void 일련번호가_순차_증가한다() {
        assertEquals(1, repository.nextSequence());
        repository.save(order("ORD-0001", "S-001", "고객A", 100, OrderStatus.RESERVED));
        assertEquals(2, repository.nextSequence());
        repository.save(order("ORD-0002", "S-001", "고객B", 200, OrderStatus.RESERVED));
        assertEquals(3, repository.nextSequence());
    }
}
