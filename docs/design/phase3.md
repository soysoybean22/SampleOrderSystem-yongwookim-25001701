# Phase 3 설계 — 주문 접수 (OrderController + OrderView)

## 1. 목표

이 Phase가 끝나면 메인 메뉴 `[2] 시료 주문`으로 진입해  
시료 ID·고객명·수량을 입력하고 주문을 접수(`RESERVED`)할 수 있다.

고객님 확인 포인트:
- `[2]` 입력 → 주문 입력 화면 진입
- 시료 ID / 고객명 / 수량 입력 → 입력 내용 확인 화면
- Y 확인 → `ORD-YYYYMMDD-NNNN` 형식 주문번호 발급 + `RESERVED` 상태 출력
- 존재하지 않는 시료 ID 입력 시 오류 메시지
- `DataMonitorTool`로 `orders.json`에 주문이 추가됐는지 확인

---

## 2. 구현 대상 목록

| 분류 | 클래스 | 역할 |
|------|--------|------|
| Controller | `OrderController` | 주문번호 생성 + 주문 접수 비즈니스 로직 |
| View | `OrderView` | 주문 접수 화면 입력·출력 |
| View | `MainView` 수정 | `[2]` 메뉴를 `OrderView`에 연결 |
| Entry | `Main.java` 수정 | `OrderController` 생성 및 `MainView`에 주입 |

---

## 3. 클래스 설계

### 3-1. OrderController

```java
package org.example.controller;

public final class OrderController {
    private final OrderRepository orderRepository;
    private final SampleRepository sampleRepository;

    public OrderController(OrderRepository orderRepository,
                           SampleRepository sampleRepository) {
        this.orderRepository = orderRepository;
        this.sampleRepository = sampleRepository;
    }

    // 주문 접수 — 시료 미존재 시 IllegalArgumentException
    public Order placeOrder(String sampleId, String customerName, int quantity) { ... }

    // 주문번호 생성: ORD-{YYYYMMDD}-{4자리 일련번호}
    private String generateOrderId() { ... }

    // RESERVED 상태 주문 목록 조회 (Phase 4에서 재사용)
    public List<Order> findReservedOrders() { ... }

    // 전체 주문 목록 조회 (MainView 요약에서 재사용)
    public List<Order> findAllOrders() { ... }
}
```

**주문번호 생성 규칙:**

```
ORD-{YYYYMMDD}-{4자리 일련번호}

날짜: 접수 당일 기준
일련번호: 전체 주문 수 + 1 을 4자리 0-패딩
예) 11번째 주문 → ORD-20260416-0011
```

**placeOrder 처리 흐름:**

```
1. sampleRepository.existsById(sampleId) → false 시 IllegalArgumentException
2. quantity <= 0 시 IllegalArgumentException
3. generateOrderId() 로 주문번호 생성
4. new Order(orderId, sampleId, customerName, quantity, RESERVED, LocalDateTime.now())
5. orderRepository.save(order)
6. order 반환
```

---

### 3-2. OrderView

```java
package org.example.view;

public final class OrderView {
    private final OrderController orderController;
    private final SampleController sampleController;

    public OrderView(OrderController orderController,
                     SampleController sampleController) { ... }

    // [2] 시료 주문 화면 진입점
    public void run() { ... }

    // 주문 입력 및 확인 처리
    private void placeOrder() { ... }
}
```

**화면 흐름:**

```
[2] 시료 주문
----------------------------------------------------------------
시료 ID   > S-003
고객명    > 삼성전자 파운드리
주문 수량 > 200

입력 내용 확인
  시료      SiC 파워기판-6인치 (S-003)
  고객      삼성전자 파운드리
  수량      200 ea

[Y] 예약 접수    [N] 취소
선택 > Y

예약 접수 완료.
  주문번호   ORD-20260416-0011
  현재 상태  RESERVED

※ 재고 확인 및 승인은 [3] 주문 승인/거절 메뉴에서 진행하세요.
```

**오류 케이스:**

```
시료 ID   > S-999
  [오류] 등록되지 않은 시료 ID입니다: S-999

주문 수량 > 0
  [오류] 수량은 1 이상이어야 합니다.

선택 > N
  주문이 취소되었습니다.
```

---

### 3-3. MainView 수정

`[2]` 케이스에 `OrderView.run()` 연결.

```java
// 기존
case "2", "3", "4", "5", "6" -> ConsoleHelper.println("  준비 중인 기능입니다.");

// 변경
case "2" -> orderView.run();
case "3", "4", "5", "6" -> ConsoleHelper.println("  준비 중인 기능입니다.");
```

`MainView` 생성자에 `OrderController` 추가:

```java
public MainView(SampleController sampleController, OrderController orderController) {
    this.sampleController = sampleController;
    this.orderController = orderController;
    this.sampleView = new SampleView(sampleController);
    this.orderView = new OrderView(orderController, sampleController);
}
```

요약 정보의 전체 주문 수도 `OrderController.findAllOrders()` 로 조회.

---

### 3-4. Main.java 수정

```java
SampleController sampleController = new SampleController(new SampleRepository());
OrderController orderController = new OrderController(
    new OrderRepository(), new SampleRepository());
new MainView(sampleController, orderController).run();
```

---

## 4. 레이어 의존 방향

```
Main
 └── MainView
       ├── SampleView  ── SampleController ── SampleRepository
       ├── OrderView   ── OrderController  ── OrderRepository
       │                                   └─ SampleRepository (시료 존재 확인)
       └── ConsoleHelper
```

`OrderController`는 시료 존재 여부 확인을 위해 `SampleRepository`에 의존한다.

---

## 5. TDD 테스트 계획

### OrderController

테스트 격리: `@BeforeEach`에서 `JsonFileStorage.setDataDir("test-data")` 설정  
`orders.json`, `samples.json` 모두 초기화

| 테스트 메서드 | 검증 내용 |
|--------------|----------|
| `주문을_정상_접수한다` | 반환된 Order의 상태가 `RESERVED`, sampleId·customerName·quantity 일치 |
| `주문번호_형식이_올바르다` | `ORD-` + 8자리 날짜 + `-` + 4자리 번호 형식 확인 |
| `주문번호가_순차_증가한다` | 2건 접수 시 일련번호 0001, 0002 순서 확인 |
| `미등록_시료_주문_시_예외` | 존재하지 않는 sampleId로 접수 시 `IllegalArgumentException` |
| `수량_0_이하_시_예외` | quantity = 0 시 `IllegalArgumentException` |

---

## 6. 수동 테스트 시나리오

| 순서 | 행동 | 기대 결과 |
|------|------|-----------|
| 1 | `./gradlew run` 실행 | 메인 메뉴 출력 |
| 2 | `2` 입력 | 주문 입력 화면 |
| 3 | S-003 / 삼성전자 파운드리 / 200 입력 | 입력 확인 화면 표시 |
| 4 | `Y` 입력 | 주문번호 + RESERVED 출력 |
| 5 | `2` 재입력 후 존재하지 않는 시료 ID 입력 | 오류 메시지 + 재입력 화면 |
| 6 | `2` 재입력 후 수량 0 입력 | 오류 메시지 + 재입력 화면 |
| 7 | `2` 재입력 후 N 입력 | 취소 메시지 출력 |
| 8 | `./gradlew run --args="monitor"` | orders.json에 방금 접수한 주문 확인 |
| 9 | 재실행 후 메인 메뉴 요약 | 전체 주문 수 증가 확인 |

---

## 7. 완료 기준

- [ ] `OrderControllerTest` 전체 통과 (`./gradlew test`)
- [ ] 메인 메뉴 `[2]` 진입 및 주문 접수 동작 확인 (수동)
- [ ] 주문번호 `ORD-YYYYMMDD-NNNN` 형식 출력 확인 (수동)
- [ ] 재시작 후 주문 유지 확인 (수동)
- [ ] 잘못된 입력(미등록 시료, 0 이하 수량) 오류 처리 확인 (수동)
- [ ] 컴파일 경고 없음
