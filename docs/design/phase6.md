# Phase 6 설계 — 모니터링 (MonitoringController + MonitoringView)

## 1. 목표

이 Phase가 끝나면 메인 메뉴 `[4] 모니터링`으로 진입해  
상태별 주문 현황과 시료별 재고 상태를 한눈에 파악할 수 있다.

고객님 확인 포인트:
- `[4]` 입력 → 모니터링 화면 진입
- `[1]` 주문량 확인 → 상태별 주문 건수 (REJECTED 제외)
- `[2]` 재고량 확인 → 시료별 재고 수량 + 여유/부족/고갈 상태
- 주문·승인·출고 후 숫자가 정확히 반영되는지 확인

---

## 2. 구현 대상 목록

| 분류 | 클래스 | 역할 |
|------|--------|------|
| Model | `StockStatus` | 재고 상태 enum (여유/부족/고갈) |
| Model | `SampleStockInfo` | 시료 + 재고 상태 묶음 전달 객체 |
| Controller | `MonitoringController` | 주문 요약, 재고 상태 조회 로직 |
| View | `MonitoringView` | 모니터링 서브메뉴 화면 |
| View | `MainView` 수정 | `[4]` 메뉴를 `MonitoringView`에 연결 |
| Entry | `Main.java` 수정 | `MonitoringController` 생성 및 주입 |

---

## 3. 클래스 설계

### 3-1. StockStatus

```java
package org.example.model;

public enum StockStatus {
    여유,  // stock >= 미처리 주문 수량 합계
    부족,  // 0 < stock < 미처리 주문 수량 합계
    고갈   // stock == 0
}
```

---

### 3-2. SampleStockInfo

View에서 시료 정보와 재고 상태를 함께 표시하기 위한 전달 객체.

```java
package org.example.model;

public final class SampleStockInfo {
    private final Sample sample;
    private final int pendingQuantity; // CONFIRMED + PRODUCING 주문 수량 합계
    private final StockStatus status;

    public SampleStockInfo(Sample sample, int pendingQuantity, StockStatus status) { ... }

    public Sample getSample() { ... }
    public int getPendingQuantity() { ... }
    public StockStatus getStatus() { ... }
}
```

---

### 3-3. MonitoringController

```java
package org.example.controller;

public final class MonitoringController {
    private final OrderRepository orderRepository;
    private final SampleRepository sampleRepository;

    public MonitoringController(OrderRepository orderRepository,
                                SampleRepository sampleRepository) { ... }

    // 상태별 주문 건수 반환 (REJECTED 제외)
    // 반환 순서: RESERVED → CONFIRMED → PRODUCING → RELEASE
    public Map<OrderStatus, Integer> getOrderSummary() { ... }

    // 시료별 재고 상태 목록 반환
    public List<SampleStockInfo> getStockStatus() { ... }
}
```

**getOrderSummary 로직:**

```
대상 상태: RESERVED, CONFIRMED, PRODUCING, RELEASE  (REJECTED 제외)
각 상태별로 orderRepository.findByStatus(status).size() 집계
반환: LinkedHashMap — 순서 보장 (RESERVED→CONFIRMED→PRODUCING→RELEASE)
```

**getStockStatus 로직:**

```
각 시료에 대해:
  1. CONFIRMED + PRODUCING 상태 주문 중 해당 시료 주문 수량 합산 → pendingQty
  2. stock == 0           → StockStatus.고갈
     stock < pendingQty   → StockStatus.부족
     stock >= pendingQty  → StockStatus.여유
  3. SampleStockInfo(sample, pendingQty, status) 생성
```

> **pendingQty가 0이고 stock도 0이면 고갈**, pendingQty가 0이고 stock > 0이면 여유.

---

### 3-4. MonitoringView

```java
package org.example.view;

public final class MonitoringView {
    private final MonitoringController monitoringController;

    public MonitoringView(MonitoringController monitoringController) { ... }

    public void run() { ... }

    private void showOrderSummary() { ... }

    private void showStockStatus() { ... }
}
```

**메인 모니터링 화면:**

```
[4] 모니터링  2026-04-16 09:32:15
----------------------------------------------------------------
  [1] 주문량 확인    [2] 재고량 확인    [0] 위로

선택 > _
```

**[1] 주문량 확인 화면:**

```
[4] 모니터링 > 주문량 확인
----------------------------------------------------------------
상태별 주문 현황

  RESERVED    3건
  CONFIRMED   8건
  PRODUCING   3건  ← 생산라인 대기
  RELEASE    18건
  ─────────────────
  합계        32건  (REJECTED 제외)
```

**[2] 재고량 확인 화면:**

```
[4] 모니터링 > 재고량 확인
----------------------------------------------------------------
시료별 재고 현황

  시료명                    재고      미처리주문   상태    잔여율
  실리콘 웨이퍼-8인치        480 ea    100 ea     여유     80%
  GaN 에피택셜-4인치         220 ea    500 ea     부족     44%
  SiC 파워기판-6인치           0 ea    200 ea     고갈      0%
  포토레지스트-PR7            910 ea      0 ea     여유    100%
  산화막 웨이퍼-SiO2            0 ea      0 ea     고갈      0%

* 미처리 주문 = CONFIRMED + PRODUCING 상태 주문 수량 합계
* 잔여율 = 재고 / (재고 + 미처리주문) × 100  (미처리주문 0이면 재고 > 0 → 100%)
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
       └── ConsoleHelper
```

---

## 5. TDD 테스트 계획

### MonitoringController

테스트 격리: `@BeforeEach`에서 `JsonFileStorage.setDataDir("test-data")` 설정

| 테스트 메서드 | 검증 내용 |
|--------------|----------|
| `상태별_주문_건수를_반환한다` | 각 상태별 건수 정확성 확인 |
| `REJECTED_주문은_집계에서_제외한다` | REJECTED 상태는 결과 Map에 없거나 집계 안 됨 |
| `재고_충분_시_여유_상태를_반환한다` | stock >= pendingQty → 여유 |
| `재고_부족_시_부족_상태를_반환한다` | 0 < stock < pendingQty → 부족 |
| `재고_0_시_고갈_상태를_반환한다` | stock == 0 → 고갈 |

---

## 6. 수동 테스트 시나리오

| 순서 | 행동 | 기대 결과 |
|------|------|-----------|
| 1 | `[4]` 입력 | 모니터링 서브메뉴 출력 |
| 2 | `[1]` 주문량 확인 | 상태별 건수 표시, REJECTED 없음 |
| 3 | `[2]` 재고량 확인 | 시료별 재고 + 여유/부족/고갈 표시 |
| 4 | 주문 접수 후 `[1]` | RESERVED 건수 1 증가 확인 |
| 5 | 주문 승인(충분) 후 `[1]` | RESERVED 감소, CONFIRMED 증가 확인 |
| 6 | 주문 승인(부족) 후 `[2]` | 해당 시료 상태 변화 확인 |

---

## 7. 완료 기준

- [ ] `MonitoringControllerTest` 전체 통과 (`./gradlew test`)
- [ ] `[4]` 진입 및 주문량/재고량 화면 출력 확인 (수동)
- [ ] REJECTED 주문 집계 제외 확인 (수동)
- [ ] 여유/부족/고갈 판단 정확성 확인 (수동)
- [ ] 컴파일 경고 없음
