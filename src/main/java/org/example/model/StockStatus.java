package org.example.model;

public enum StockStatus {
    여유,  // stock >= 미처리 주문 수량 합계
    부족,  // 0 < stock < 미처리 주문 수량 합계
    고갈   // stock == 0
}
