# Phase 4 설계 — 주문 승인/거절 (approveOrder + rejectOrder + ApprovalView)

## 1. 목표

이 Phase가 끝나면 메인 메뉴 `[3] 주문 승인/거절`로 진입해  
RESERVED 상태 주문 목록을 보고 개별 주문을 승인하거나 거절할 수 있다.

- **재고 충분** → 즉시 `CONFIRMED`, 재고 차감
- **재고 부족** → 생산 큐 자동 등록, `PRODUCING` 전환

고객님 확인 포인트:
- `[3]` 입력 → RESERVED 주문 목록 출력
- 주문 선택 → 재고 현황 + 승인 시 처리 경로 표시 (충분/부족)
- `[Y]` 승인 → 상태 변경 확인 (CONFIRMED 또는 PRODUCING)
- `[N]` 거절 → REJECTED 전환 확인
- `monitor` 실행으로 `orders.json`, `samples.json`, `production_jobs.json` 변경 확인

---

## 2. 구현 대상 목록

| 분류 | 클래스 | 역할 |
|------|--------|------|
| Model | `ApprovalResult` | 승인 결과 전달 객체 (Order + 선택적 ProductionJob) |
| Controller | `OrderController` 확장 | `approveOrder()`, `rejectOrder()` 추가 |
| View | `ApprovalView` | 승인/거절 서브메뉴 화면 |
| View | `MainView` 수정 | `[3]` 메뉴를 `ApprovalView`에 연결 |
| Entry | `Main.java` 수정 | `ProductionJobRepository` 주입 추가 |

---

## 3. 클래스 설계

### 3-1. ApprovalResult

승인 후 View에 필요한 정보를 하나의 객체로 전달한다.

```java
package org.example.model;

public final class ApprovalResult {
    private final Order order;
    private final ProductionJob productionJob; // 재고 부족 시 생성, 충분 시 null

    public ApprovalResult(Order order, ProductionJob productionJob) { ... }

    public Order getOrder() { ... }
    public ProductionJob getProductionJob() { ... }

    // CONFIRMED 경로였는지 여부
    public boolean isConfirmed() {
        return order.getStatus() == OrderStatus.CONFIRMED;
    }
}
```

---

### 3-2. OrderController 확장

기존 `OrderController`에 두 메서드를 추가한다.

```java
// 승인 — RESERVED가 아니면 IllegalStateException
public ApprovalResult approveOrder(String orderId) { ... }

// 거절 — RESERVED가 아니면 IllegalStateException
public Order rejectOrder(String orderId) { ... }
```

**approveOrder 처리 흐름:**

```
1. orderId로 주문 조회 → 없으면 IllegalArgumentException
2. 상태가 RESERVED가 아니면 IllegalStateException
3. 해당 시료 조회 (sampleId로)
4. 재고 비교:
   ┌ 재고 >= 주문 수량 (충분)
   │  → sample.subtractStock(quantity)
   │  → sampleRepository.updateStock(sampleId, newStock)
   │  → orderRepository.updateStatus(orderId, CONFIRMED)
   │  → return ApprovalResult(order, null)
   │
   └ 재고 < 주문 수량 (부족)
      → shortage = quantity - stock
      → actualQty = ceil(shortage / (yield * 0.9))
      → totalTime = avgProductionTime * actualQty
      → ProductionJob 생성 및 저장
      → orderRepository.updateStatus(orderId, PRODUCING)
      → return ApprovalResult(order, productionJob)
```

**rejectOrder 처리 흐름:**

```
1. orderId로 주문 조회 → 없으면 IllegalArgumentException
2. 상태가 RESERVED가 아니면 IllegalStateException
3. orderRepository.updateStatus(orderId, REJECTED)
4. 업데이트된 order 반환
```

**실 생산량 계산:**

```java
int actualQty = (int) Math.ceil(shortage / (sample.getYield() * 0.9));
double totalTime = sample.getAvgProductionTime() * actualQty;
```

---

### 3-3. ApprovalView

```java
package org.example.view;

public final class ApprovalView {
    private final OrderController orderController;
    private final SampleController sampleController;

    public ApprovalView(OrderController orderController,
                        SampleController sampleController) { ... }

    // [3] 주문 승인/거절 서브메뉴 진입점
    public void run() { ... }

    // RESERVED 주문 목록 표시 및 선택
    private void showReservedOrders() { ... }

    // 선택된 주문에 대해 재고 확인 및 승인/거절 처리
    private void processApproval(Order selectedOrder) { ... }
}
```

**화면 흐름:**

```
[3] 주문 승인/거절
----------------------------------------------------------------
승인 대기 중인 예약 목록 (RESERVED)

번호   주문번호               시료명                   수량     상태
[1]   ORD-20260416-0001    LG이노텍              300 ea   RESERVED
[2]   ORD-20260416-0002    SK하이닉스            150 ea   RESERVED
[3]   ORD-20260416-0003    삼성전자 파운드리      200 ea   RESERVED
[0]   위로

승인할 번호 > 3
```

```
재고 확인 중...

시료      SiC 파워기판-6인치 (S-003)
주문 수량  200 ea
현재 재고  30 ea    ← 부족 (170 ea 모자람)

재고 부족. 생산이 필요합니다.
  부족분      170 ea
  실 생산량   206 ea  (수율 0.92 / 오차 보정 0.9 적용)
  총 생산시간 164.8 min

[Y] 승인 (생산 후 출고)    [N] 주문 거절
선택 > Y

승인 완료.
  상태 변경  RESERVED → PRODUCING
  주문번호   ORD-20260416-0003
  생산 큐에 등록되었습니다.
```

**재고 충분 케이스:**

```
재고 확인 중...

시료      실리콘 웨이퍼-8인치 (S-001)
주문 수량  100 ea
현재 재고  480 ea    ← 충분

[Y] 승인 (즉시 출고 대기)    [N] 주문 거절
선택 > Y

승인 완료.
  상태 변경  RESERVED → CONFIRMED
  주문번호   ORD-20260416-0001
  재고 차감  480 ea → 380 ea
```

**거절 케이스:**

```
[Y] 승인    [N] 주문 거절
선택 > N

거절 완료.
  상태 변경  RESERVED → REJECTED
  주문번호   ORD-20260416-0002
```

**RESERVED 주문 없는 경우:**

```
[3] 주문 승인/거절
----------------------------------------------------------------
승인 대기 중인 주문이 없습니다.
```

---

### 3-4. MainView 수정

```java
// 기존
case "3", "4", "5", "6" -> ConsoleHelper.println("  준비 중인 기능입니다.");

// 변경
case "3" -> approvalView.run();
case "4", "5", "6" -> ConsoleHelper.println("  준비 중인 기능입니다.");
```

생성자에 `ProductionJobRepository` 의존 제거 — `OrderController`에서 주문 수 조회.  
`ApprovalView` 생성 및 필드 추가.

---

### 3-5. Main.java 수정

```java
OrderController orderController = new OrderController(
    new OrderRepository(), new SampleRepository(),
    new ProductionJobRepository());  // ProductionJobRepository 추가
```

`OrderController` 생성자에 `ProductionJobRepository` 추가.

---

## 4. 레이어 의존 방향

```
Main
 └── MainView
       ├── SampleView    ── SampleController  ── SampleRepository
       ├── OrderView     ── OrderController   ── OrderRepository
       ├── ApprovalView  ── OrderController      SampleRepository
       │                                      └─ ProductionJobRepository
       └── ConsoleHelper
```

`OrderController`가 `ProductionJobRepository`까지 의존하는 구조.  
승인 처리 시 생산 큐 등록까지 Controller 책임.

---

## 5. TDD 테스트 계획

기존 `OrderControllerTest`에 메서드를 추가한다.

### 추가 테스트

| 테스트 메서드 | 검증 내용 |
|--------------|----------|
| `재고_충분_시_CONFIRMED로_전환한다` | 상태 CONFIRMED, 재고 차감 확인 |
| `재고_부족_시_PRODUCING으로_전환한다` | 상태 PRODUCING, ProductionJob 생성 확인 |
| `실_생산량_계산이_올바르다` | `ceil(shortage / (yield * 0.9))` 값 검증 |
| `거절_시_REJECTED로_전환한다` | 상태 REJECTED 확인 |
| `RESERVED_아닌_주문_승인_시_예외` | `IllegalStateException` 발생 |

### 실 생산량 계산 검증 예시

```
시료: yield=0.92, avgProductionTime=0.8
주문 수량: 200, 현재 재고: 30
부족분: 170
실 생산량: ceil(170 / (0.92 * 0.9)) = ceil(170 / 0.828) = ceil(205.31) = 206
총 생산시간: 0.8 * 206 = 164.8 min
```

---

## 6. 수동 테스트 시나리오

| 순서 | 행동 | 기대 결과 |
|------|------|-----------|
| 1 | `[3]` 입력 | RESERVED 주문 목록 출력 |
| 2 | 재고 충분한 주문 선택 → `Y` | RESERVED → CONFIRMED, 재고 차감 확인 |
| 3 | 재고 부족한 주문 선택 → `Y` | RESERVED → PRODUCING, 생산 큐 등록 확인 |
| 4 | 주문 선택 → `N` | RESERVED → REJECTED 확인 |
| 5 | RESERVED 없을 때 `[3]` | "대기 주문 없음" 메시지 |
| 6 | `monitor` 실행 | orders.json 상태, samples.json 재고, production_jobs.json 큐 변경 확인 |

---

## 7. 완료 기준

- [ ] `OrderControllerTest` 추가 테스트 전체 통과 (`./gradlew test`)
- [ ] `[3]` 진입 및 승인/거절 동작 확인 (수동)
- [ ] 재고 충분 → CONFIRMED + 재고 차감 확인 (수동)
- [ ] 재고 부족 → PRODUCING + 생산 큐 등록 확인 (수동)
- [ ] 거절 → REJECTED 확인 (수동)
- [ ] 컴파일 경고 없음
