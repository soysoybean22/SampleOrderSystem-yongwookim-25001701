package org.example.controller;

import org.example.model.OrderStatus;
import org.example.model.SampleStockInfo;
import org.example.model.StockStatus;
import org.example.repository.JsonFileStorage;
import org.example.repository.OrderRepository;
import org.example.repository.ProductionJobRepository;
import org.example.repository.SampleRepository;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MonitoringControllerTest {

    private MonitoringController monitoringController;
    private SampleController sampleController;
    private OrderController orderController;

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
        monitoringController = new MonitoringController(orderRepo, sampleRepo);

        sampleController.register("S-001", "실리콘 웨이퍼", 0.5, 0.92, 480);
        sampleController.register("S-003", "SiC 파워기판", 0.8, 0.92, 30);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(Path.of("test-data", "orders.json"));
        Files.deleteIfExists(Path.of("test-data", "samples.json"));
        Files.deleteIfExists(Path.of("test-data", "production_jobs.json"));
        JsonFileStorage.setDataDir("data");
    }

    @Test
    @DisplayName("상태별 주문 건수를 반환한다")
    void 상태별_주문_건수를_반환한다() {
        orderController.placeOrder("S-001", "고객A", 100); // RESERVED
        orderController.placeOrder("S-001", "고객B", 50);  // RESERVED → approve → CONFIRMED
        String orderId = orderController.placeOrder("S-001", "고객B", 50).getOrderId();
        orderController.approveOrder(orderId); // CONFIRMED

        Map<OrderStatus, Integer> summary = monitoringController.getOrderSummary();

        assertEquals(2, summary.get(OrderStatus.RESERVED));
        assertEquals(1, summary.get(OrderStatus.CONFIRMED));
        assertEquals(0, summary.getOrDefault(OrderStatus.PRODUCING, 0));
        assertEquals(0, summary.getOrDefault(OrderStatus.RELEASE, 0));
    }

    @Test
    @DisplayName("REJECTED 주문은 집계에서 제외한다")
    void REJECTED_주문은_집계에서_제외한다() {
        String orderId = orderController.placeOrder("S-001", "고객A", 100).getOrderId();
        orderController.rejectOrder(orderId);

        Map<OrderStatus, Integer> summary = monitoringController.getOrderSummary();

        assertFalse(summary.containsKey(OrderStatus.REJECTED));
    }

    @Test
    @DisplayName("재고 충분 시 여유 상태를 반환한다")
    void 재고_충분_시_여유_상태를_반환한다() {
        // S-001: 재고 480, CONFIRMED 주문 100 → 여유
        String orderId = orderController.placeOrder("S-001", "고객A", 100).getOrderId();
        orderController.approveOrder(orderId);

        List<SampleStockInfo> stockList = monitoringController.getStockStatus();
        SampleStockInfo info = stockList.stream()
            .filter(s -> s.getSample().getSampleId().equals("S-001"))
            .findFirst().orElseThrow();

        assertEquals(StockStatus.여유, info.getStatus());
    }

    @Test
    @DisplayName("재고 부족 시 부족 상태를 반환한다")
    void 재고_부족_시_부족_상태를_반환한다() {
        // S-003: 재고 30, PRODUCING 주문 200 → 부족(재고 > 0이지만 미처리 주문보다 적음)
        String orderId = orderController.placeOrder("S-003", "고객A", 200).getOrderId();
        orderController.approveOrder(orderId); // 재고 부족 → PRODUCING

        List<SampleStockInfo> stockList = monitoringController.getStockStatus();
        SampleStockInfo info = stockList.stream()
            .filter(s -> s.getSample().getSampleId().equals("S-003"))
            .findFirst().orElseThrow();

        assertEquals(StockStatus.부족, info.getStatus());
    }

    @Test
    @DisplayName("재고 0 시 고갈 상태를 반환한다")
    void 재고_0_시_고갈_상태를_반환한다() {
        // S-001: 재고 480, CONFIRMED 주문 480 → 재고 0 → 고갈
        String orderId = orderController.placeOrder("S-001", "고객A", 480).getOrderId();
        orderController.approveOrder(orderId); // 재고 충분 → CONFIRMED, 재고 0으로 차감

        List<SampleStockInfo> stockList = monitoringController.getStockStatus();
        SampleStockInfo info = stockList.stream()
            .filter(s -> s.getSample().getSampleId().equals("S-001"))
            .findFirst().orElseThrow();

        assertEquals(StockStatus.고갈, info.getStatus());
    }
}
