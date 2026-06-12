# Phase 11 설계 — 생산 자동 완료 + 진행률 Progress Bar

## 1. 목표

생산 작업이 시작된 시각부터 총 생산시간이 경과하면 자동으로 완료 처리한다.  
생산라인 조회 화면의 [ 현재 처리 중 ] 섹션에 진행률 Progress Bar와 경과 시간을 표시한다.

---

## 2. 현재 구조 분석

### ProductionJob 현재 필드

| 필드 | 타입 | 의미 |
|------|------|------|
| `orderId` | String | 주문번호 |
| `sampleId` | String | 시료 ID |
| `shortage` | int | 부족 수량 |
| `actualProductionQty` | int | 실 생산량 |
| `totalProductionTime` | double | 총 생산시간 (분) |
| `enqueuedAt` | LocalDateTime | 큐 등록 시각 |

### 문제점

- `enqueuedAt`은 큐에 등록된 시각이지, 실제 생산이 시작된 시각이 아니다.
- FIFO 구조에서 2번째 이후 작업은 앞 작업이 완료된 후에야 생산이 시작된다.
- 자동 완료 기준 시각 = **실제 생산 시작 시각** + totalProductionTime.
- 현재는 생산 완료를 사용자가 수동으로만 처리한다.

---

## 3. 설계

### 3-1. 생산 시작 시각 추적 — `startedAt` 필드 추가

`ProductionJob`에 `startedAt` 필드를 추가한다.

| 상황 | `startedAt` 값 |
|------|---------------|
| 주문 승인 시 큐가 비어 있음 | `LocalDateTime.now()` — 즉시 생산 시작 |
| 주문 승인 시 큐에 선행 작업 존재 | `null` — 아직 시작 안 됨 |
| 앞 작업 완료(자동·수동) 후 다음 작업 활성화 | `LocalDateTime.now()` — 활성화 시점 |

`startedAt` 이 null이면 `enqueuedAt`을 폴백으로 사용한다 (기존 데이터 호환).

### 3-2. 진행률 계산 메서드

```java
// ProductionJob에 추가
public double elapsedMinutes(LocalDateTime now) {
    LocalDateTime start = (startedAt != null) ? startedAt : enqueuedAt;
    return Duration.between(start, now).toSeconds() / 60.0;
}

public int progressRatio(LocalDateTime now) {
    double elapsed = elapsedMinutes(now);
    return (int) Math.min(100, elapsed / totalProductionTime * 100);
}
```

순수 함수 — `now`를 주입받아 테스트 가능.

---

## 4. 레이어별 변경 상세

### 4-1. ProductionJob (Model)

```java
// 변경 전
public final class ProductionJob {
    private final String orderId;
    ...
    private final LocalDateTime enqueuedAt;

    public ProductionJob(String orderId, String sampleId, int shortage,
                         int actualProductionQty, double totalProductionTime,
                         LocalDateTime enqueuedAt) { ... }
}

// 변경 후
public final class ProductionJob {
    private final String orderId;
    ...
    private final LocalDateTime enqueuedAt;
    private final LocalDateTime startedAt;  // nullable

    public ProductionJob(String orderId, String sampleId, int shortage,
                         int actualProductionQty, double totalProductionTime,
                         LocalDateTime enqueuedAt, LocalDateTime startedAt) { ... }

    public LocalDateTime getStartedAt() { return startedAt; }

    public double elapsedMinutes(LocalDateTime now) {
        LocalDateTime start = (startedAt != null) ? startedAt : enqueuedAt;
        return Duration.between(start, now).toSeconds() / 60.0;
    }

    public int progressRatio(LocalDateTime now) {
        return (int) Math.min(100, elapsedMinutes(now) / totalProductionTime * 100);
    }
}
```

### 4-2. ProductionJobRepository

**직렬화 / 역직렬화** — `startedAt` 필드 추가. null이면 JSON에서 생략.

```java
private ProductionJob fromMap(Map<String, String> map) {
    LocalDateTime startedAt = map.containsKey("startedAt")
        ? LocalDateTime.parse(map.get("startedAt")) : null;
    return new ProductionJob(
        map.get("orderId"), map.get("sampleId"),
        Integer.parseInt(map.get("shortage")),
        Integer.parseInt(map.get("actualProductionQty")),
        Double.parseDouble(map.get("totalProductionTime")),
        LocalDateTime.parse(map.get("enqueuedAt")),
        startedAt
    );
}

private Map<String, String> toMap(ProductionJob job) {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("orderId", job.getOrderId());
    ...
    if (job.getStartedAt() != null) {
        map.put("startedAt", job.getStartedAt().toString());
    }
    return map;
}
```

**`updateStartedAt()` 추가** — 앞 작업 완료 후 다음 작업 활성화에 사용.

```java
public void updateStartedAt(String orderId, LocalDateTime startedAt) {
    List<Map<String, String>> rows = JsonFileStorage.parseArray(JsonFileStorage.read(FILE));
    for (Map<String, String> row : rows) {
        if (row.get("orderId").equals(orderId)) {
            row.put("startedAt", startedAt.toString());
            break;
        }
    }
    JsonFileStorage.write(FILE, JsonFileStorage.toJsonArray(rows));
}
```

### 4-3. OrderController.approveOrder()

주문 승인 시 ProductionJob 생성 코드에서, **큐가 비어 있으면** `startedAt = now`로 설정.

```java
// approveOrder() 내 ProductionJob 생성 부분
boolean queueEmpty = productionJobRepository.findAll().isEmpty();
LocalDateTime startedAt = queueEmpty ? LocalDateTime.now() : null;
ProductionJob job = new ProductionJob(
    order.getOrderId(), order.getSampleId(),
    shortage, actualQty, totalTime, LocalDateTime.now(), startedAt
);
productionJobRepository.save(job);
```

### 4-4. ProductionController

#### autoCompleteIfReady() 추가

```java
public boolean autoCompleteIfReady() {
    return autoCompleteIfReady(LocalDateTime.now());
}

// 테스트용 오버로드 (package-visible)
boolean autoCompleteIfReady(LocalDateTime now) {
    Optional<ProductionJob> jobOpt = productionJobRepository.findFirst();
    if (jobOpt.isEmpty()) return false;

    ProductionJob job = jobOpt.get();
    if (job.progressRatio(now) >= 100) {
        completeProduction(now);
        return true;
    }
    return false;
}
```

#### completeProduction() — 다음 작업 startedAt 설정

```java
public ProductionJob completeProduction() {
    return completeProduction(LocalDateTime.now());
}

// 테스트용 오버로드
ProductionJob completeProduction(LocalDateTime now) {
    ProductionJob job = productionJobRepository.findFirst()
        .orElseThrow(() -> new IllegalStateException("처리할 생산 작업이 없습니다."));

    // 기존 완료 처리
    Sample sample = sampleRepository.findById(job.getSampleId()).orElseThrow(...);
    sampleRepository.updateStock(job.getSampleId(), sample.getStock() + job.getActualProductionQty());
    orderRepository.updateStatus(job.getOrderId(), OrderStatus.CONFIRMED);
    productionJobRepository.deleteByOrderId(job.getOrderId());

    // 다음 작업 startedAt 설정
    productionJobRepository.findFirst().ifPresent(next ->
        productionJobRepository.updateStartedAt(next.getOrderId(), now)
    );

    return job;
}
```

### 4-5. MainView.run()

루프 매 반복 시작 시 자동 완료 확인.

```java
public void run() {
    while (true) {
        productionController.autoCompleteIfReady();  // ← 추가
        ConsoleHelper.clearScreen();
        ConsoleHelper.printBanner();
        printSummary();
        printMenu();
        ...
    }
}
```

`MainView`에 `productionController` 필드가 이미 존재하므로 별도 의존성 추가 불필요.

### 4-6. ProductionView.printCurrentJob()

진행률 Progress Bar와 경과 시간 행을 추가한다.

#### 변경 전

```
[ 현재 처리 중 ]
  주문번호    ORD-20260416-0007
  시료        SiC 파워기판-6인치 (S-003)
  주문량      200 ea    현재 재고 30 ea → 부족 170 ea
  실 생산량   206 ea    총 생산시간 164.8 min
```

#### 변경 후

```
[ 현재 처리 중 ]
  주문번호    ORD-20260416-0007
  시료        SiC 파워기판-6인치 (S-003)
  주문량      200 ea    현재 재고 30 ea → 부족 170 ea
  실 생산량   206 ea    총 생산시간 164.8 min
  진행률      [████████░░]  80%    (131.8 / 164.8 min 경과)
```

#### 코드 변경

```java
// 변경 후 printCurrentJob() 마지막에 추가
LocalDateTime now = LocalDateTime.now();
int ratio = job.progressRatio(now);
double elapsed = job.elapsedMinutes(now);
String bar = ConsoleHelper.progressBar(ratio);
System.out.printf("  진행률      %s  %3d%%    (%.1f / %.1f min 경과)%n",
    bar, ratio, elapsed, job.getTotalProductionTime());
```

---

## 5. 화면 흐름

```
[MainView 루프 진입]
  └─ productionController.autoCompleteIfReady()
       ├─ 큐 비어 있음 → false 반환
       ├─ 첫 작업 진행률 < 100% → false 반환
       └─ 첫 작업 진행률 >= 100%
            ├─ completeProduction() 호출
            │    ├─ 재고 추가
            │    ├─ 주문 상태 CONFIRMED 변경
            │    ├─ ProductionJob 삭제
            │    └─ 다음 작업 있으면 startedAt = now 설정
            └─ true 반환 (루프 계속 → 다음 작업도 확인 가능)
  └─ clearScreen() + printBanner() + printSummary() + printMenu()
       (메인 메뉴에서 생산라인 대기 건수가 자동으로 감소)
```

---

## 6. TDD 테스트 계획

`ProductionJob`의 순수 함수는 자동 테스트 대상이다.  
`ProductionController.autoCompleteIfReady(now)` 도 주입된 시각으로 테스트 가능하다.

### ProductionJobTest 추가

```java
@Test
@DisplayName("progressRatio()는 경과 시간이 0이면 0을 반환한다")
void progressRatio_경과시간_0() {
    ProductionJob job = new ProductionJob("ORD-001", "S-001", 10, 10, 60.0,
        LocalDateTime.of(2026, 1, 1, 9, 0), LocalDateTime.of(2026, 1, 1, 9, 0));
    assertEquals(0, job.progressRatio(LocalDateTime.of(2026, 1, 1, 9, 0)));
}

@Test
@DisplayName("progressRatio()는 절반 경과 시 50을 반환한다")
void progressRatio_절반경과_50() {
    ProductionJob job = new ProductionJob("ORD-001", "S-001", 10, 10, 60.0,
        LocalDateTime.of(2026, 1, 1, 9, 0), LocalDateTime.of(2026, 1, 1, 9, 0));
    assertEquals(50, job.progressRatio(LocalDateTime.of(2026, 1, 1, 9, 30)));
}

@Test
@DisplayName("progressRatio()는 총 시간 초과 시 100을 반환한다")
void progressRatio_시간초과_100() {
    ProductionJob job = new ProductionJob("ORD-001", "S-001", 10, 10, 60.0,
        LocalDateTime.of(2026, 1, 1, 9, 0), LocalDateTime.of(2026, 1, 1, 9, 0));
    assertEquals(100, job.progressRatio(LocalDateTime.of(2026, 1, 1, 11, 0)));
}

@Test
@DisplayName("progressRatio()는 startedAt이 null이면 enqueuedAt을 사용한다")
void progressRatio_startedAt_null_폴백() {
    ProductionJob job = new ProductionJob("ORD-001", "S-001", 10, 10, 60.0,
        LocalDateTime.of(2026, 1, 1, 9, 0), null);
    assertEquals(50, job.progressRatio(LocalDateTime.of(2026, 1, 1, 9, 30)));
}
```

### ProductionControllerTest 추가

```java
@Test
@DisplayName("autoCompleteIfReady()는 진행률이 100% 이상이면 완료 처리 후 true를 반환한다")
void autoCompleteIfReady_완료조건_true() { ... }

@Test
@DisplayName("autoCompleteIfReady()는 진행률이 100% 미만이면 false를 반환한다")
void autoCompleteIfReady_미완료_false() { ... }

@Test
@DisplayName("autoCompleteIfReady()는 큐가 비어 있으면 false를 반환한다")
void autoCompleteIfReady_빈큐_false() { ... }
```

---

## 7. 작업 목록

| # | 작업 | 파일 | TDD |
|---|------|------|-----|
| 11-1 | `ProductionJobTest` — `progressRatio()` 테스트 4개 추가 | `ProductionJobTest.java` | RED |
| 11-2 | `ProductionJob` — `startedAt` 필드 + `elapsedMinutes()` + `progressRatio()` 추가 | `ProductionJob.java` | GREEN |
| 11-3 | `ProductionJobRepository` — `startedAt` 직렬화 + `updateStartedAt()` 추가 | `ProductionJobRepository.java` | 컴파일 |
| 11-4 | `ProductionControllerTest` — `autoCompleteIfReady()` 테스트 3개 추가 | `ProductionControllerTest.java` | RED |
| 11-5 | `ProductionController` — `autoCompleteIfReady()` + `completeProduction(now)` 추가 | `ProductionController.java` | GREEN |
| 11-6 | `OrderController` — `approveOrder()` 에서 `startedAt` 설정 | `OrderController.java` | 기존 테스트 |
| 11-7 | `MainView.run()` — `autoCompleteIfReady()` 호출 추가 | `MainView.java` | 수동 |
| 11-8 | `ProductionView.printCurrentJob()` — 진행률 Progress Bar 추가 | `ProductionView.java` | 수동 |

---

## 8. 완료 기준

- [ ] `ProductionJobTest` 신규 4개 통과
- [ ] `ProductionControllerTest` 신규 3개 통과
- [ ] 총 생산시간 경과 시 메인 메뉴 진입만으로 자동 완료 확인
- [ ] 생산라인 조회 화면에서 진행률 Progress Bar + 경과 시간 표시 확인
- [ ] 기존 테스트 전체 통과
- [ ] 컴파일 경고 없음

---

## 9. 논의 포인트

### 9-1. 더미 데이터의 과거 시각 문제

`DummyDataGenerator`의 `ProductionJob`은 `enqueuedAt = 2026-04-16 09:30`으로 고정되어 있다.  
현재(2026-06-12)와의 차이 ≈ 57일 >> 164.8분이므로, dummy 실행 직후 메인 메뉴 진입 시 즉시 자동 완료된다.  
이는 기능 동작 확인에 유리하다. 진행률 Progress Bar를 눈으로 확인하려면
더미 데이터를 최근 시각으로 생성하거나, 짧은 `totalProductionTime` 값을 사용한다.

### 9-2. DummyDataGenerator 업데이트 필요 여부

현재 `DummyDataGenerator`가 `ProductionJob` 생성 시 `startedAt`을 전달하지 않는다.  
Phase 11 구현 후 생성자 시그니처가 바뀌므로 `startedAt = enqueuedAt` 또는 `null`로 업데이트해야 한다.
