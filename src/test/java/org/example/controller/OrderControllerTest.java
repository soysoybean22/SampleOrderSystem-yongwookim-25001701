package org.example.controller;

import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.repository.JsonFileStorage;
import org.example.repository.OrderRepository;
import org.example.repository.SampleRepository;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

class OrderControllerTest {

    private OrderController orderController;
    private SampleController sampleController;

    @BeforeEach
    void setUp() throws IOException {
        JsonFileStorage.setDataDir("test-data");
        Files.deleteIfExists(Path.of("test-data", "orders.json"));
        Files.deleteIfExists(Path.of("test-data", "samples.json"));
        sampleController = new SampleController(new SampleRepository());
        orderController = new OrderController(new OrderRepository(), new SampleRepository());
        sampleController.register("S-001", "실리콘 웨이퍼", 0.5, 0.92, 480);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(Path.of("test-data", "orders.json"));
        Files.deleteIfExists(Path.of("test-data", "samples.json"));
        JsonFileStorage.setDataDir("data");
    }

    @Test
    @DisplayName("주문을 정상 접수한다")
    void 주문을_정상_접수한다() {
        Order order = orderController.placeOrder("S-001", "LG이노텍", 100);

        assertEquals("S-001", order.getSampleId());
        assertEquals("LG이노텍", order.getCustomerName());
        assertEquals(100, order.getQuantity());
        assertEquals(OrderStatus.RESERVED, order.getStatus());
    }

    @Test
    @DisplayName("주문번호 형식이 올바르다")
    void 주문번호_형식이_올바르다() {
        Order order = orderController.placeOrder("S-001", "LG이노텍", 100);

        assertTrue(order.getOrderId().matches("ORD-\\d{8}-\\d{4}"),
            "주문번호 형식: " + order.getOrderId());
    }

    @Test
    @DisplayName("주문번호가 순차 증가한다")
    void 주문번호가_순차_증가한다() {
        Order first  = orderController.placeOrder("S-001", "고객A", 100);
        Order second = orderController.placeOrder("S-001", "고객B", 200);

        String firstSeq  = first.getOrderId().split("-")[2];
        String secondSeq = second.getOrderId().split("-")[2];
        assertEquals("0001", firstSeq);
        assertEquals("0002", secondSeq);
    }

    @Test
    @DisplayName("미등록 시료 주문 시 예외가 발생한다")
    void 미등록_시료_주문_시_예외() {
        assertThrows(IllegalArgumentException.class,
            () -> orderController.placeOrder("S-999", "고객A", 100));
    }

    @Test
    @DisplayName("수량 0 이하 시 예외가 발생한다")
    void 수량_0_이하_시_예외() {
        assertThrows(IllegalArgumentException.class,
            () -> orderController.placeOrder("S-001", "고객A", 0));
    }
}
