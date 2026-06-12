# Phase 7 설계 — 출고 처리 (ReleaseController + ReleaseView)

## 1. 목표

이 Phase가 끝나면 메인 메뉴 `[6] 출고 처리`로 진입해  
`CONFIRMED` 상태의 주문을 선택하고 출고를 실행해 `RELEASE`로 전환할 수 있다.

고객님 확인 포인트:
- `[6]` 입력 → CONFIRMED 주문 목록 출력
- 주문 선택 → 출고 확인 화면
- `Y` 확인 → `CONFIRMED → RELEASE` 전환 확인
- `monitor` 실행 → `orders.json` 상태 변경 확인
- `[4] 모니터링 → [1]` → CONFIRMED 건수 감소, RELEASE 건수 증가 확인

---

## 2. 구현 대상 목록

| 분류 | 클래스 | 역할 |
|------|--------|------|
| Controller | `ReleaseController` | 출고 처리 비즈니스 로직 |
| View | `ReleaseView` | 출고 처리 화면 |
| View | `MainView` 수정 | `[6]` 메뉴를 `ReleaseView`에 연결 |
| Entry | `Main.java` 수정 | `ReleaseController` 생성 및 주입 |

---

## 3. 클래스 설계

### 3-1. ReleaseController

```java
package org.example.controller;

public final class ReleaseController {
    private final OrderRepository orderRepository;

    public ReleaseController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // CONFIRMED 주문 목록 조회
    public List<Order> findConfirmedOrders() { ... }

    // 출고 처리 — CONFIRMED가 아니면 IllegalStateException
    public Order release(String orderId) { ... }
}
```

**release 처리 흐름:**

```
1. orderId로 주문 조회 → 없으면 IllegalArgumentException
2. 상태가 CONFIRMED가 아니면 IllegalStateException
3. orderRepository.updateStatus(orderId, RELEASE)
4. 업데이트된 order 반환
```

**재고 처리 설계 결정:**

출고 시 별도의 재고 차감을 하지 않는다.

- **재고 충분 경로 (CONFIRMED):** Phase 4 승인 시 이미 재고가 차감됨
- **생산 완료 경로 (PRODUCING → CONFIRMED):** Phase 5 생산 완료 시 실생산량이 재고에 추가됨. 이 경우 release 시 order.quantity만큼 차감이 필요하나, Phase 4/5 구현과의 일관성을 위해 출고 시점의 차감은 생략함

---

### 3-2. ReleaseView

```java
package org.example.view;

public final class ReleaseView {
    private final ReleaseController releaseController;
    private final SampleController sampleController;

    public ReleaseView(ReleaseController releaseController,
                       SampleController sampleController) { ... }

    // [6] 출고 처리 진입점
    public void run() { ... }

    // CONFIRMED 목록 표시 및 선택
    private void showConfirmedOrders() { ... }

    // 출고 확인 및 처리
    private void processRelease(Order selectedOrder) { ... }
}
```

**화면 흐름 — CONFIRMED 주문 있을 때:**

```
[6] 출고 처리
----------------------------------------------------------------
출고 가능 주문 (CONFIRMED)

  번호  주문번호               시료명                   고객명          수량
  [1]  ORD-20260416-0004    포토레지스트-PR7          DB하이텍        400 ea
  [2]  ORD-20260416-0005    실리콘 웨이퍼-8인치       인텔코리아       100 ea
  [3]  ORD-20260416-0006    GaN 에피택셜-4인치        퀄컴코리아        80 ea
  [0]  위로

출고할 번호 > 1
```

```
출고 처리하겠습니까?
  주문번호   ORD-20260416-0004
  시료        포토레지스트-PR7 (S-004)
  고객        DB하이텍
  수량        400 ea

[Y] 확인    [N] 취소
선택 > Y

출고 처리 완료.
  주문번호    ORD-20260416-0004
  상태 변경   CONFIRMED → RELEASE
  처리 일시   2026-04-16 09:34:02
```

**화면 흐름 — CONFIRMED 주문 없을 때:**

```
[6] 출고 처리
----------------------------------------------------------------
출고 가능한 주문이 없습니다.
```

---

## 4. 레이어 의존 방향

```
Main
 └── MainView
       ├── SampleView      ── SampleController    ── SampleRepository
       ├── OrderView       ── OrderController     ── OrderRepository
       ├── ApprovalView    ── OrderController        SampleRepository
       │                                          └─ ProductionJobRepository
       ├── ProductionView  ── ProductionController ── ProductionJobRepository
       │                                           ── SampleRepository
       │                                           └─ OrderRepository
       ├── MonitoringView  ── MonitoringController ── OrderRepository
       │                                           └─ SampleRepository
       ├── ReleaseView     ── ReleaseController   ── OrderRepository
       │               └──── SampleController    (시료명 표시용)
       └── ConsoleHelper
```

`ReleaseController`는 `OrderRepository`만 의존한다. (재고 차감 없으므로 `SampleRepository` 불필요)

---

## 5. TDD 테스트 계획

### ReleaseController

테스트 격리: `@BeforeEach`에서 `JsonFileStorage.setDataDir("test-data")` 설정

| 테스트 메서드 | 검증 내용 |
|--------------|----------|
| `출고_처리_시_RELEASE로_전환한다` | CONFIRMED 주문 release() 후 상태 RELEASE 확인 |
| `CONFIRMED_아닌_주문_출고_시_예외` | RESERVED/PRODUCING/RELEASE 상태 주문 시도 시 `IllegalStateException` |
| `CONFIRMED_주문_목록을_반환한다` | findConfirmedOrders() 결과 건수 및 상태 확인 |

---

## 6. 수동 테스트 시나리오

| 순서 | 행동 | 기대 결과 |
|------|------|-----------|
| 1 | `[6]` 입력 | CONFIRMED 주문 목록 출력 |
| 2 | 주문 선택 → `Y` | CONFIRMED → RELEASE 전환, 처리 일시 출력 |
| 3 | `[6]` 재진입 | 방금 처리한 주문 목록에서 사라짐 확인 |
| 4 | `[4] → [1]` | CONFIRMED 감소, RELEASE 증가 확인 |
| 5 | `monitor` 실행 | orders.json 상태 변경 확인 |
| 6 | CONFIRMED 없을 때 `[6]` | "출고 가능한 주문 없음" 메시지 확인 |

---

## 7. 완료 기준

- [ ] `ReleaseControllerTest` 전체 통과 (`./gradlew test`)
- [ ] `[6]` 진입 및 출고 처리 동작 확인 (수동)
- [ ] CONFIRMED → RELEASE 전환 확인 (수동)
- [ ] CONFIRMED 없을 때 안내 메시지 확인 (수동)
- [ ] 컴파일 경고 없음
