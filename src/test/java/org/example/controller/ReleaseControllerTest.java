package org.example.controller;

import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.repository.JsonFileStorage;
import org.example.repository.OrderRepository;
import org.example.repository.ProductionJobRepository;
import org.example.repository.SampleRepository;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReleaseControllerTest {

    private ReleaseController releaseController;
    private OrderController orderController;
    private SampleController sampleController;

    @BeforeEach
    void setUp() throws IOException {
        JsonFileStorage.setDataDir("test-data");
        Files.deleteIfExists(Path.of("test-data", "orders.json"));
        Files.deleteIfExists(Path.of("test-data", "samples.json"));
        Files.deleteIfExists(Path.of("test-data", "production_jobs.json"));

        SampleRepository sampleRepo = new SampleRepository();
        OrderRepository orderRepo = new OrderRepository();
        ProductionJobRepository jobRepo = new ProductionJobRepository();

        sampleController = new SampleController(sampleRepo);
        orderController = new OrderController(orderRepo, sampleRepo, jobRepo);
        releaseController = new ReleaseController(orderRepo);

        sampleController.register("S-001", "실리콘 웨이퍼", 0.5, 0.92, 480);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(Path.of("test-data", "orders.json"));
        Files.deleteIfExists(Path.of("test-data", "samples.json"));
        Files.deleteIfExists(Path.of("test-data", "production_jobs.json"));
        JsonFileStorage.setDataDir("data");
    }

    @Test
    @DisplayName("출고 처리 시 RELEASE로 전환한다")
    void 출고_처리_시_RELEASE로_전환한다() {
        String orderId = orderController.placeOrder("S-001", "LG이노텍", 100).getOrderId();
        orderController.approveOrder(orderId); // CONFIRMED

        Order released = releaseController.release(orderId);

        assertEquals(OrderStatus.RELEASE, released.getStatus());
    }

    @Test
    @DisplayName("CONFIRMED가 아닌 주문 출고 시 예외가 발생한다")
    void CONFIRMED_아닌_주문_출고_시_예외() {
        String orderId = orderController.placeOrder("S-001", "LG이노텍", 100).getOrderId();
        // RESERVED 상태에서 출고 시도
        assertThrows(IllegalStateException.class,
            () -> releaseController.release(orderId));
    }

    @Test
    @DisplayName("CONFIRMED 주문 목록을 반환한다")
    void CONFIRMED_주문_목록을_반환한다() {
        String id1 = orderController.placeOrder("S-001", "고객A", 100).getOrderId();
        String id2 = orderController.placeOrder("S-001", "고객B", 50).getOrderId();
        orderController.approveOrder(id1); // CONFIRMED
        // id2는 RESERVED 유지

        List<Order> confirmed = releaseController.findConfirmedOrders();

        assertEquals(1, confirmed.size());
        assertEquals(OrderStatus.CONFIRMED, confirmed.get(0).getStatus());
    }
}
