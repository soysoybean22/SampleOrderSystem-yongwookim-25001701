package org.example.controller;

import org.example.model.OrderStatus;
import org.example.model.ProductionJob;
import org.example.repository.JsonFileStorage;
import org.example.repository.OrderRepository;
import org.example.repository.ProductionJobRepository;
import org.example.repository.SampleRepository;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductionControllerTest {

    private ProductionController productionController;
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
        productionController = new ProductionController(jobRepo, sampleRepo, orderRepo);

        // 재고 부족 시료 등록 후 주문 → 승인 → PRODUCING 상태 생성
        sampleController.register("S-003", "SiC 파워기판", 0.8, 0.92, 30);
        String orderId = orderController.placeOrder("S-003", "삼성전자", 200).getOrderId();
        orderController.approveOrder(orderId);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(Path.of("test-data", "orders.json"));
        Files.deleteIfExists(Path.of("test-data", "samples.json"));
        Files.deleteIfExists(Path.of("test-data", "production_jobs.json"));
        JsonFileStorage.setDataDir("data");
    }

    @Test
    @DisplayName("생산 큐를 FIFO 순서로 반환한다")
    void 생산_큐를_FIFO_순서로_반환한다() throws IOException {
        sampleController.register("S-005", "산화막 웨이퍼", 0.6, 0.88, 0);
        String orderId2 = orderController.placeOrder("S-005", "LG이노텍", 150).getOrderId();
        orderController.approveOrder(orderId2);

        List<ProductionJob> queue = productionController.getQueue();

        assertEquals(2, queue.size());
        assertEquals("S-003", queue.get(0).getSampleId());
        assertEquals("S-005", queue.get(1).getSampleId());
    }

    @Test
    @DisplayName("생산 완료 시 재고가 증가한다")
    void 생산_완료_시_재고가_증가한다() {
        int beforeStock = sampleController.findAll().stream()
            .filter(s -> s.getSampleId().equals("S-003"))
            .findFirst().orElseThrow().getStock();

        ProductionJob completed = productionController.completeProduction();

        int afterStock = new SampleRepository().findById("S-003").orElseThrow().getStock();
        assertEquals(beforeStock + completed.getActualProductionQty(), afterStock);
    }

    @Test
    @DisplayName("생산 완료 시 주문 상태가 CONFIRMED로 전환된다")
    void 생산_완료_시_CONFIRMED로_전환된다() {
        ProductionJob completed = productionController.completeProduction();

        OrderStatus status = new OrderRepository()
            .findById(completed.getOrderId()).orElseThrow().getStatus();
        assertEquals(OrderStatus.CONFIRMED, status);
    }

    @Test
    @DisplayName("생산 완료 후 큐에서 제거된다")
    void 생산_완료_후_큐에서_제거된다() {
        assertEquals(1, productionController.getQueue().size());
        productionController.completeProduction();
        assertEquals(0, productionController.getQueue().size());
    }

    @Test
    @DisplayName("큐가 비어있을 때 완료 처리 시 예외가 발생한다")
    void 큐가_비어있을_때_완료_처리_시_예외() {
        productionController.completeProduction();
        assertThrows(IllegalStateException.class,
            () -> productionController.completeProduction());
    }
}
