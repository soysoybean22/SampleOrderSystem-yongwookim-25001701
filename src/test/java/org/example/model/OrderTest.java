package org.example.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    private Order createOrder(OrderStatus status) {
        return new Order("ORD-20260416-0001", "S-001", "LG이노텍", 100, status, LocalDateTime.now());
    }

    @Test
    @DisplayName("주문을 정상 생성한다")
    void 주문을_정상_생성한다() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 16, 9, 30, 0);
        Order order = new Order("ORD-20260416-0001", "S-001", "LG이노텍", 100, OrderStatus.RESERVED, now);

        assertEquals("ORD-20260416-0001", order.getOrderId());
        assertEquals("S-001", order.getSampleId());
        assertEquals("LG이노텍", order.getCustomerName());
        assertEquals(100, order.getQuantity());
        assertEquals(OrderStatus.RESERVED, order.getStatus());
        assertEquals(now, order.getCreatedAt());
    }

    @Test
    @DisplayName("RESERVED에서 CONFIRMED로 전이한다")
    void RESERVED에서_CONFIRMED로_전이한다() {
        Order order = createOrder(OrderStatus.RESERVED);
        order.changeStatus(OrderStatus.CONFIRMED);
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }

    @Test
    @DisplayName("RESERVED에서 PRODUCING으로 전이한다")
    void RESERVED에서_PRODUCING으로_전이한다() {
        Order order = createOrder(OrderStatus.RESERVED);
        order.changeStatus(OrderStatus.PRODUCING);
        assertEquals(OrderStatus.PRODUCING, order.getStatus());
    }

    @Test
    @DisplayName("RESERVED에서 REJECTED로 전이한다")
    void RESERVED에서_REJECTED로_전이한다() {
        Order order = createOrder(OrderStatus.RESERVED);
        order.changeStatus(OrderStatus.REJECTED);
        assertEquals(OrderStatus.REJECTED, order.getStatus());
    }

    @Test
    @DisplayName("PRODUCING에서 CONFIRMED로 전이한다")
    void PRODUCING에서_CONFIRMED로_전이한다() {
        Order order = createOrder(OrderStatus.PRODUCING);
        order.changeStatus(OrderStatus.CONFIRMED);
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }

    @Test
    @DisplayName("CONFIRMED에서 RELEASE로 전이한다")
    void CONFIRMED에서_RELEASE로_전이한다() {
        Order order = createOrder(OrderStatus.CONFIRMED);
        order.changeStatus(OrderStatus.RELEASE);
        assertEquals(OrderStatus.RELEASE, order.getStatus());
    }

    @Test
    @DisplayName("허용되지 않은 전이는 예외가 발생한다")
    void 허용되지_않은_전이는_예외() {
        Order order = createOrder(OrderStatus.REJECTED);
        assertThrows(IllegalStateException.class, () -> order.changeStatus(OrderStatus.CONFIRMED));
    }
}
