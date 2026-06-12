package org.example.controller;

import org.example.model.ApprovalResult;
import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.repository.JsonFileStorage;
import org.example.repository.OrderRepository;
import org.example.repository.ProductionJobRepository;
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
        Files.deleteIfExists(Path.of("test-data", "production_jobs.json"));
        sampleController = new SampleController(new SampleRepository());
        orderController = new OrderController(
            new OrderRepository(), new SampleRepository(), new ProductionJobRepository());
        sampleController.register("S-001", "실리콘 웨이퍼", 0.5, 0.92, 480);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(Path.of("test-data", "orders.json"));
        Files.deleteIfExists(Path.of("test-data", "samples.json"));
        Files.deleteIfExists(Path.of("test-data", "production_jobs.json"));
        JsonFileStorage.setDataDir("data");
    }

    // ── 주문 접수 ──────────────────────────────────────────────────

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

        assertEquals("0001", first.getOrderId().split("-")[2]);
        assertEquals("0002", second.getOrderId().split("-")[2]);
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

    // ── 주문 승인/거절 ─────────────────────────────────────────────

    @Test
    @DisplayName("재고 충분 시 CONFIRMED로 전환하고 재고를 차감한다")
    void 재고_충분_시_CONFIRMED로_전환한다() {
        Order order = orderController.placeOrder("S-001", "LG이노텍", 100);

        ApprovalResult result = orderController.approveOrder(order.getOrderId());

        assertEquals(OrderStatus.CONFIRMED, result.getOrder().getStatus());
        assertTrue(result.isConfirmed());
        assertNull(result.getProductionJob());

        int remaining = new SampleRepository().findById("S-001").orElseThrow().getStock();
        assertEquals(380, remaining); // 480 - 100
    }

    @Test
    @DisplayName("재고 부족 시 PRODUCING으로 전환하고 생산 작업을 등록한다")
    void 재고_부족_시_PRODUCING으로_전환한다() {
        // 재고 30인 시료 등록 후 200 주문
        sampleController.register("S-003", "SiC 파워기판", 0.8, 0.92, 30);
        Order order = orderController.placeOrder("S-003", "삼성전자", 200);

        ApprovalResult result = orderController.approveOrder(order.getOrderId());

        assertEquals(OrderStatus.PRODUCING, result.getOrder().getStatus());
        assertFalse(result.isConfirmed());
        assertNotNull(result.getProductionJob());
        assertEquals(170, result.getProductionJob().getShortage()); // 200 - 30
    }

    @Test
    @DisplayName("실 생산량 계산이 올바르다")
    void 실_생산량_계산이_올바르다() {
        // yield=0.92, shortage=170 → ceil(170 / (0.92 * 0.9)) = ceil(205.31) = 206
        sampleController.register("S-003", "SiC 파워기판", 0.8, 0.92, 30);
        Order order = orderController.placeOrder("S-003", "삼성전자", 200);

        ApprovalResult result = orderController.approveOrder(order.getOrderId());

        assertEquals(206, result.getProductionJob().getActualProductionQty());
        assertEquals(164.8, result.getProductionJob().getTotalProductionTime(), 0.01);
    }

    @Test
    @DisplayName("거절 시 REJECTED로 전환한다")
    void 거절_시_REJECTED로_전환한다() {
        Order order = orderController.placeOrder("S-001", "LG이노텍", 100);

        Order rejected = orderController.rejectOrder(order.getOrderId());

        assertEquals(OrderStatus.REJECTED, rejected.getStatus());
    }

    @Test
    @DisplayName("RESERVED가 아닌 주문 승인 시 예외가 발생한다")
    void RESERVED_아닌_주문_승인_시_예외() {
        Order order = orderController.placeOrder("S-001", "LG이노텍", 100);
        orderController.rejectOrder(order.getOrderId());

        assertThrows(IllegalStateException.class,
            () -> orderController.approveOrder(order.getOrderId()));
    }
}
