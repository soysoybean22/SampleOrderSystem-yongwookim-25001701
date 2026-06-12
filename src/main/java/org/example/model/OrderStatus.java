package org.example.model;

public enum OrderStatus {
    RESERVED,   // 주문 접수
    REJECTED,   // 주문 거절
    PRODUCING,  // 재고 부족으로 생산 중
    CONFIRMED,  // 출고 대기
    RELEASE     // 출고 완료
}
