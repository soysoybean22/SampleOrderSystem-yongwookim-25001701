package org.example.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderStatusTest {

    @Test
    @DisplayName("모든 상태값이 존재한다")
    void 모든_상태값이_존재한다() {
        OrderStatus[] values = OrderStatus.values();
        assertEquals(5, values.length);
        assertArrayEquals(
            new OrderStatus[]{
                OrderStatus.RESERVED,
                OrderStatus.REJECTED,
                OrderStatus.PRODUCING,
                OrderStatus.CONFIRMED,
                OrderStatus.RELEASE
            },
            values
        );
    }
}
