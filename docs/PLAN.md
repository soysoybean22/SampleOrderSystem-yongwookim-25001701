# PLAN — 반도체 시료 생산주문관리 시스템 구현 계획

## 1. 아키텍처 개요

**패턴:** MVC (Model / Controller / View)  
**데이터 영속성:** JSON 파일 (`data/*.json`)  
**인터페이스:** 콘솔(CLI)

```
사용자 입력
    │
    ▼
  View  ──────── 출력 담당, 입력 수집
    │
    ▼
Controller ───── 비즈니스 로직, 상태 전이
    │
    ▼
Repository ───── JSON 파일 읽기/쓰기
    │
    ▼
  Model  ───────── 도메인 객체 (불변 or 최소 setter)
```

---

## 2. 패키지 구조

```
src/main/java/org/example/
├── Main.java                        # 진입점, 메인 루프
│
├── model/
│   ├── Sample.java                  # 시료 도메인
│   ├── Order.java                   # 주문 도메인
│   ├── OrderStatus.java             # 주문 상태 enum
│   └── ProductionJob.java           # 생산 작업 도메인
│
├── repository/
│   ├── SampleRepository.java        # 시료 CRUD
│   ├── OrderRepository.java         # 주문 CRUD
│   ├── ProductionJobRepository.java # 생산 작업 CRUD
│   └── JsonFileStorage.java         # JSON 파일 읽기/쓰기 공통 유틸
│
├── controller/
│   ├── SampleController.java        # 시료 관리 로직
│   ├── OrderController.java         # 주문 접수/승인/거절 로직
│   ├── MonitoringController.java    # 모니터링 조회 로직
│   ├── ProductionController.java    # 생산라인 관리 로직
│   └── ReleaseController.java       # 출고 처리 로직
│
├── view/
│   ├── MainView.java                # 메인 메뉴 출력
│   ├── SampleView.java              # 시료 관련 화면
│   ├── OrderView.java               # 주문 관련 화면
│   ├── MonitoringView.java          # 모니터링 화면
│   ├── ProductionView.java          # 생산라인 화면
│   ├── ReleaseView.java             # 출고 처리 화면
│   └── ConsoleHelper.java           # 공통 입력/출력 유틸
│
└── tool/
    ├── DataMonitorTool.java         # 관리자용 데이터 모니터링 도구
    └── DummyDataGenerator.java      # 테스트용 더미 데이터 생성 도구

src/test/java/org/example/
├── model/
│   ├── SampleTest.java
│   ├── OrderTest.java
│   └── OrderStatusTest.java
├── repository/
│   ├── JsonFileStorageTest.java
│   ├── SampleRepositoryTest.java
│   ├── OrderRepositoryTest.java
│   └── ProductionJobRepositoryTest.java
└── controller/
    ├── SampleControllerTest.java
    ├── OrderControllerTest.java
    ├── ProductionControllerTest.java
    └── ReleaseControllerTest.java

data/                                # JSON 데이터 저장 디렉터리 (자동 생성)
├── samples.json
├── orders.json
└── production_jobs.json
```

---

## 3. 도메인 모델 설계

### Sample

```java
public final class Sample {
    private final String sampleId;   // "S-001"
    private final String name;
    private final double avgProductionTime; // min/ea
    private final double yield;      // 0 < yield <= 1
    private int stock;               // 현재 재고
}
```

### Order

```java
public final class Order {
    private final String orderId;        // "ORD-20260416-0001"
    private final String sampleId;
    private final String customerName;
    private final int quantity;
    private OrderStatus status;
    private final LocalDateTime createdAt;
}
```

### OrderStatus

```java
public enum OrderStatus {
    RESERVED, REJECTED, PRODUCING, CONFIRMED, RELEASE
}
```

### ProductionJob

```java
public final class ProductionJob {
    private final String orderId;
    private final String sampleId;
    private final int shortage;
    private final int actualProductionQty;   // ceil(shortage / (yield * 0.9))
    private final double totalProductionTime; // avgProductionTime * actualProductionQty
    private final LocalDateTime enqueuedAt;
}
```

---

## 4. JSON 데이터 저장 설계

### 저장 위치

실행 디렉터리 하위 `data/` 폴더. 없으면 최초 실행 시 자동 생성.

### samples.json

```json
[
  {
    "sampleId": "S-001",
    "name": "실리콘 웨이퍼-8인치",
    "avgProductionTime": 0.5,
    "yield": 0.92,
    "stock": 480
  }
]
```

### orders.json

```json
[
  {
    "orderId": "ORD-20260416-0001",
    "sampleId": "S-001",
    "customerName": "LG이노텍",
    "quantity": 300,
    "status": "RESERVED",
    "createdAt": "2026-04-16T09:30:00"
  }
]
```

### production_jobs.json

```json
[
  {
    "orderId": "ORD-20260416-0003",
    "sampleId": "S-003",
    "shortage": 170,
    "actualProductionQty": 206,
    "totalProductionTime": 164.8,
    "enqueuedAt": "2026-04-16T09:31:00"
  }
]
```

### JsonFileStorage 역할

- 파일 I/O(read/write)만 담당, 엔티티 변환은 각 Repository가 처리
- JSON 직렬화는 순수 Java로 구현 (외부 라이브러리 없음)

---

## 5. 핵심 비즈니스 로직

### 주문번호 생성

```
ORD-{YYYYMMDD}-{4자리 일련번호}
일련번호는 전체 주문 개수 기준으로 순차 증가
```

### 주문 승인 로직 (OrderController)

```
1. 해당 시료의 현재 재고 조회
2. if (재고 >= 주문 수량):
     재고 -= 주문 수량
     주문 상태 → CONFIRMED
3. else:
     부족분 = 주문 수량 - 재고
     실 생산량 = ceil(부족분 / (수율 * 0.9))
     총 생산 시간 = 평균 생산시간 * 실 생산량
     ProductionJob 생성 → 생산 큐 등록
     주문 상태 → PRODUCING
```

### 출고 처리 로직 (ReleaseController)

```
1. CONFIRMED 상태 주문 선택
2. 주문 상태 → RELEASE
3. 재고에서 해당 수량 차감
```

### 생산 완료 처리 (ProductionController)

```
1. 생산 큐에서 완료된 작업 확인
2. 시료 재고 += 실 생산량
3. 해당 주문 상태 PRODUCING → CONFIRMED
4. 생산 큐에서 해당 작업 제거
```

### 재고 상태 판단 (모니터링)

```
고갈: stock == 0
부족: 0 < stock < (해당 시료의 CONFIRMED + PRODUCING 주문 수량 합계)
여유: stock >= (해당 시료의 CONFIRMED + PRODUCING 주문 수량 합계)
```

---

## 6. 구현 단계 (TDD 적용)

> 각 단계는 **RED → GREEN → REFACTOR** 사이클로 진행한다.  
> 다음 단계로 넘어가기 전, 전체 테스트(`./gradlew test`)가 통과해야 한다.

---

### Phase 1. 기반 구조 (PoC 4종 포함)

**목표:** MVC 스켈레톤 + JSON 영속성 + 관리자 도구 2종 완성  
이 Phase가 끝나면 더미 데이터를 넣고 JSON 상태를 콘솔에서 조회할 수 있다.

#### 1-A. MVC 스켈레톤 + 도메인 모델

| # | 작업 | 테스트 포인트 |
|---|------|--------------|
| 1-A-1 | 패키지 구조 생성 (`model`, `repository`, `controller`, `view`, `tool`) | 컴파일 확인 |
| 1-A-2 | `OrderStatus` enum 정의 | 5개 상태값 존재 확인 |
| 1-A-3 | `Sample` 모델 (생성자, getter, 재고 변경 메서드) | 필드값, 재고 증감, 유효성 예외 |
| 1-A-4 | `Order` 모델 (생성자, getter, 상태 전이) | 허용/불허 전이 검증 |
| 1-A-5 | `ProductionJob` 모델 (불변 데이터 홀더) | 필드값 일치 |

#### 1-B. 데이터 영속성 (JSON CRUD)

| # | 작업 | 테스트 포인트 |
|---|------|--------------|
| 1-B-1 | `JsonFileStorage` — save/load, `data/` 자동 생성 | 저장→로드 동일 내용, 파일 없으면 빈 리스트 |
| 1-B-2 | `SampleRepository` CRUD | 저장, 조회, 이름 검색, 중복 ID 예외, 재고 업데이트 |
| 1-B-3 | `OrderRepository` CRUD | 저장, 상태별 조회, 상태 업데이트, 일련번호 증가 |
| 1-B-4 | `ProductionJobRepository` CRUD | 저장, FIFO 순서 조회, 삭제 |

#### 1-C. 데이터 모니터링 Tool

별도 실행 가능한 관리자 도구. `DataMonitorTool.main()`으로 실행하면  
현재 `data/*.json`에 저장된 모든 데이터를 테이블 형태로 콘솔에 출력한다.

```
$ java -cp ... org.example.tool.DataMonitorTool

=== [시료 목록] samples.json ===
ID      이름                   생산시간   수율   재고
S-001   실리콘 웨이퍼-8인치    0.5 min   0.92   480
...

=== [주문 목록] orders.json ===
주문번호              시료ID   고객명     수량   상태       접수일시
ORD-20260416-0001   S-001    LG이노텍   300    RESERVED   2026-04-16T09:30:00
...

=== [생산 작업 큐] production_jobs.json (FIFO 순) ===
주문번호              시료ID   부족분   실생산량   생산시간    등록일시
ORD-20260416-0003   S-003    170      206       164.8 min  2026-04-16T09:31:00
...
```

| # | 작업 | 확인 방법 |
|---|------|-----------|
| 1-C-1 | `DataMonitorTool` 구현 | 직접 실행 후 출력 확인 (수동) |

#### 1-D. Dummy 데이터 생성 Tool

별도 실행 가능한 테스트 데이터 도구. `DummyDataGenerator.main()`으로 실행하면  
미리 정의된 시료·주문·생산 작업 더미 데이터를 `data/*.json`에 저장한다.

생성되는 더미 데이터 (예시):

| 항목 | 내용 |
|------|------|
| 시료 | 5종 (실리콘 웨이퍼, GaN 에피택셜, SiC 파워기판, 포토레지스트, 산화막 웨이퍼) |
| 주문 | 10건 (RESERVED 3건, CONFIRMED 3건, PRODUCING 2건, RELEASE 2건) |
| 생산 작업 | PRODUCING 주문에 연결된 2건 |

```
$ java -cp ... org.example.tool.DummyDataGenerator

더미 데이터 생성 완료.
- 시료 5종 → data/samples.json
- 주문 10건 → data/orders.json
- 생산 작업 2건 → data/production_jobs.json
```

| # | 작업 | 확인 방법 |
|---|------|-----------|
| 1-D-1 | `DummyDataGenerator` 구현 | 실행 후 `DataMonitorTool`로 생성 데이터 확인 (수동) |

---

### Phase 2. 시료 관리

**목표:** 시료 등록, 조회, 검색 기능 (메인 메뉴 연결)

| # | 작업 | 테스트 포인트 |
|---|------|--------------|
| 2-1 | `SampleController.register()` | 정상 등록, ID 중복 시 예외 |
| 2-2 | `SampleController.findAll()` | 전체 목록 반환 |
| 2-3 | `SampleController.searchByName()` | 이름 포함 검색, 결과 없음 처리 |
| 2-4 | `SampleView` + `Main` 메뉴 연결 | (수동 확인) |

---

### Phase 3. 주문 접수

**목표:** 고객 주문 생성 (RESERVED)

| # | 작업 | 테스트 포인트 |
|---|------|--------------|
| 3-1 | 주문번호 생성 로직 | 형식 `ORD-YYYYMMDD-NNNN` 검증 |
| 3-2 | `OrderController.placeOrder()` | 정상 주문 생성, 미등록 시료 주문 시 예외 |
| 3-3 | `OrderView` + `Main` 메뉴 연결 | (수동 확인) |

---

### Phase 4. 주문 승인/거절

**목표:** 재고 기반 자동 분기 처리

| # | 작업 | 테스트 포인트 |
|---|------|--------------|
| 4-1 | `OrderController.approveOrder()` — 재고 충분 경로 | RESERVED → CONFIRMED, 재고 차감 |
| 4-2 | `OrderController.approveOrder()` — 재고 부족 경로 | RESERVED → PRODUCING, ProductionJob 생성 |
| 4-3 | 실 생산량 계산 | `ceil(shortage / (yield * 0.9))` 정확성 |
| 4-4 | `OrderController.rejectOrder()` | RESERVED → REJECTED |
| 4-5 | RESERVED가 아닌 주문 승인 시도 | 예외 발생 |
| 4-6 | `OrderView` 승인/거절 화면 + `Main` 연결 | (수동 확인) |

---

### Phase 5. 생산 라인

**목표:** FIFO 생산 큐 관리 및 생산 완료 처리

| # | 작업 | 테스트 포인트 |
|---|------|--------------|
| 5-1 | `ProductionController.getQueue()` | FIFO 순서로 반환 |
| 5-2 | `ProductionController.completeProduction()` | 재고 증가, PRODUCING → CONFIRMED |
| 5-3 | 총 생산 시간 계산 | `avgTime * actualQty` 검증 |
| 5-4 | `ProductionView` + `Main` 메뉴 연결 | (수동 확인) |

---

### Phase 6. 모니터링

**목표:** 상태별 주문 수 및 재고 상태 표시

| # | 작업 | 테스트 포인트 |
|---|------|--------------|
| 6-1 | `MonitoringController.getOrderSummary()` | 상태별 건수 정확성, REJECTED 제외 |
| 6-2 | `MonitoringController.getStockStatus()` | 여유/부족/고갈 판단 로직 |
| 6-3 | `MonitoringView` + `Main` 메뉴 연결 | (수동 확인) |

---

### Phase 7. 출고 처리

**목표:** CONFIRMED 주문 출고 실행

| # | 작업 | 테스트 포인트 |
|---|------|--------------|
| 7-1 | `ReleaseController.release()` | CONFIRMED → RELEASE |
| 7-2 | CONFIRMED가 아닌 주문 출고 시도 | 예외 발생 |
| 7-3 | `ReleaseView` + `Main` 메뉴 연결 | (수동 확인) |

---

### Phase 8. 통합 및 메인 루프

**목표:** 전체 메뉴 연결 및 종료 처리

| # | 작업 | 테스트 포인트 |
|---|------|--------------|
| 8-1 | `MainView` 요약 정보 출력 (시료 수, 총 재고, 주문 수, 생산 대기) | (수동 확인) |
| 8-2 | 잘못된 메뉴 입력 처리 | 오류 메시지 후 재입력 |
| 8-3 | 전체 흐름 통합 시나리오 테스트 | (수동 시나리오 테스트) |

---

## 7. build.gradle.kts 의존성

```kotlin
dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
```

JSON 처리는 외부 라이브러리 없이 `java.io`, `java.nio`, 표준 문자열 파싱으로 구현한다.

---

## 8. 구현 순서 체크리스트

- [ ] Phase 1-A — MVC 스켈레톤 + 도메인 모델
- [ ] Phase 1-B — 데이터 영속성 (JSON CRUD)
- [ ] Phase 1-C — 데이터 모니터링 Tool
- [ ] Phase 1-D — Dummy 데이터 생성 Tool
- [ ] Phase 2 — 시료 관리
- [ ] Phase 3 — 주문 접수
- [ ] Phase 4 — 주문 승인/거절
- [ ] Phase 5 — 생산 라인
- [ ] Phase 6 — 모니터링
- [ ] Phase 7 — 출고 처리
- [ ] Phase 8 — 통합 및 메인 루프
