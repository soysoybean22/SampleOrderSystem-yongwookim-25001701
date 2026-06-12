# Phase 5 설계 — 생산 라인 (ProductionController + ProductionView)

## 1. 목표

이 Phase가 끝나면 메인 메뉴 `[5] 생산라인 조회`로 진입해  
현재 생산 중인 작업과 대기 큐를 확인하고, 생산 완료를 처리할 수 있다.

생산 완료 처리 시:
- 실 생산량이 시료 재고에 추가된다
- 해당 주문이 `PRODUCING → CONFIRMED`로 전환된다
- 생산 큐에서 해당 작업이 제거된다

고객님 확인 포인트:
- `[5]` 입력 → 현재 처리 중인 작업 + 대기 목록 출력
- `[1]` 생산 완료 처리 → PRODUCING → CONFIRMED 전환, 재고 증가 확인
- `monitor` 실행으로 `production_jobs.json` 감소, `samples.json` 재고 증가 확인

---

## 2. 구현 대상 목록

| 분류 | 클래스 | 역할 |
|------|--------|------|
| Controller | `ProductionController` | 생산 큐 조회, 생산 완료 처리 |
| View | `ProductionView` | 생산라인 현황 화면 |
| View | `MainView` 수정 | `[5]` 메뉴를 `ProductionView`에 연결 |
| Entry | `Main.java` 수정 | `ProductionController` 생성 및 주입 |

---

## 3. 클래스 설계

### 3-1. ProductionController

```java
package org.example.controller;

public final class ProductionController {
    private final ProductionJobRepository productionJobRepository;
    private final SampleRepository sampleRepository;
    private final OrderRepository orderRepository;

    public ProductionController(ProductionJobRepository productionJobRepository,
                                SampleRepository sampleRepository,
                                OrderRepository orderRepository) { ... }

    // 전체 생산 큐 반환 (enqueuedAt 오름차순 — FIFO)
    public List<ProductionJob> getQueue() { ... }

    // 현재 처리 중인 작업 (큐 첫 번째)
    public Optional<ProductionJob> getCurrentJob() { ... }

    // 생산 완료 처리 — 큐가 비어있으면 IllegalStateException
    public ProductionJob completeProduction() { ... }
}
```

**completeProduction 처리 흐름:**

```
1. getCurrentJob() → 없으면 IllegalStateException("처리할 생산 작업이 없습니다")
2. job.getSampleId()로 시료 조회
3. 재고 += actualProductionQty
4. sampleRepository.updateStock(sampleId, newStock)
5. orderRepository.updateStatus(orderId, CONFIRMED)
6. productionJobRepository.deleteByOrderId(orderId)
7. 완료된 job 반환
```

---

### 3-2. ProductionView

```java
package org.example.view;

public final class ProductionView {
    private final ProductionController productionController;
    private final SampleController sampleController;

    public ProductionView(ProductionController productionController,
                          SampleController sampleController) { ... }

    // [5] 생산라인 조회 진입점
    public void run() { ... }

    // 생산 현황 표시
    private void printProductionStatus() { ... }

    // 생산 완료 처리
    private void completeProduction() { ... }
}
```

**화면 흐름 — 작업 있을 때:**

```
[5] 생산라인 조회  FIFO 방식
----------------------------------------------------------------
생산라인  단일 라인    현재 상태: RUNNING

[ 현재 처리 중 ]
  주문번호    ORD-20260416-0007
  시료        SiC 파워기판-6인치 (S-003)
  주문량      200 ea    현재 재고 30 ea → 부족 170 ea
  실 생산량   206 ea    총 생산시간 164.8 min

[ 대기 중인 주문 (FIFO 순) ]
  순서  주문번호               시료명                 부족분   실생산량   총생산시간
  1     ORD-20260416-0008    산화막 웨이퍼-SiO2      150 ea   190 ea    114.0 min

* 부족분 = 주문량 - 재고,  실생산량 = ceil(부족분 / (수율 × 0.9))
* FIFO 방식으로 처리됩니다.

[1] 생산 완료 처리    [0] 위로
선택 > _
```

**[1] 생산 완료 처리 선택 시:**

```
생산 완료 처리하겠습니까?
  주문번호    ORD-20260416-0007
  시료        SiC 파워기판-6인치
  실 생산량   206 ea → 재고에 추가됩니다

[Y] 확인    [N] 취소
선택 > Y

생산 완료.
  주문번호    ORD-20260416-0007
  상태 변경   PRODUCING → CONFIRMED
  재고 추가   30 ea + 206 ea = 236 ea
  잔여 큐     1건 대기 중
```

**화면 흐름 — 작업 없을 때:**

```
[5] 생산라인 조회  FIFO 방식
----------------------------------------------------------------
생산라인  단일 라인    현재 상태: IDLE

현재 생산 대기 중인 작업이 없습니다.
```

---

## 4. 레이어 의존 방향

```
Main
 └── MainView
       ├── SampleView    ── SampleController    ── SampleRepository
       ├── OrderView     ── OrderController     ── OrderRepository
       ├── ApprovalView  ── OrderController        SampleRepository
       │                                        └─ ProductionJobRepository
       ├── ProductionView ── ProductionController ── ProductionJobRepository
       │                                          ── SampleRepository
       │                                          └─ OrderRepository
       └── ConsoleHelper
```

---

## 5. TDD 테스트 계획

### ProductionController

테스트 격리: `@BeforeEach`에서 `JsonFileStorage.setDataDir("test-data")` 설정  
`orders.json`, `samples.json`, `production_jobs.json` 모두 초기화

| 테스트 메서드 | 검증 내용 |
|--------------|----------|
| `생산_큐를_FIFO_순서로_반환한다` | 두 작업 저장 후 `getQueue()` 등록 순서 확인 |
| `생산_완료_시_재고가_증가한다` | `completeProduction()` 후 시료 재고 = 기존 + 실생산량 |
| `생산_완료_시_CONFIRMED로_전환된다` | `completeProduction()` 후 주문 상태 CONFIRMED |
| `생산_완료_후_큐에서_제거된다` | `completeProduction()` 후 `getQueue()` 크기 감소 |
| `큐가_비어있을_때_완료_처리_시_예외` | `IllegalStateException` 발생 |

**테스트 시나리오 예시 (재고 계산 검증):**

```
준비: 시료 S-003 (yield=0.92, avgTime=0.8, stock=30)
     주문 200 → 승인 → PRODUCING
     productionJob.shortage=170, actualQty=206, totalTime=164.8

completeProduction() 실행 후:
  → 재고: 30 + 206 = 236
  → 주문 상태: CONFIRMED
  → 큐: 비어있음
```

---

## 6. 수동 테스트 시나리오

| 순서 | 행동 | 기대 결과 |
|------|------|-----------|
| 1 | `[5]` 입력 | 현재 생산 중 작업 + 대기 큐 출력 |
| 2 | `[1]` 생산 완료 처리 → `Y` | PRODUCING → CONFIRMED, 재고 증가 확인 |
| 3 | `monitor` 실행 | production_jobs.json 1건 감소, samples.json 재고 증가 확인 |
| 4 | 큐 비어있을 때 `[5]` | IDLE 상태 + "작업 없음" 메시지 확인 |
| 5 | `[4]` 모니터링 | CONFIRMED 건수 증가 확인 (Phase 6 미리 확인) |

---

## 7. 완료 기준

- [ ] `ProductionControllerTest` 전체 통과 (`./gradlew test`)
- [ ] `[5]` 진입 및 생산 현황 출력 확인 (수동)
- [ ] 생산 완료 처리 후 재고 증가 + CONFIRMED 전환 확인 (수동)
- [ ] 큐 비어있을 때 IDLE 화면 확인 (수동)
- [ ] 컴파일 경고 없음
