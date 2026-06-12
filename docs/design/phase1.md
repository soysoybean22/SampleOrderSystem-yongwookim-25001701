# Phase 1 설계 — 기반 구조 (MVC 스켈레톤 + JSON 영속성 + 관리자 도구 2종)

## 1. 목표

비즈니스 로직과 화면이 올라갈 **기반 레이어** 완성.  
이 Phase가 끝나면 아래 두 가지가 가능하다:

- `DummyDataGenerator`로 테스트 데이터를 `data/*.json`에 삽입
- `DataMonitorTool`로 현재 저장된 데이터 상태를 콘솔에서 조회

---

## 2. 구현 대상 목록

| 분류 | 클래스 | PoC 대응 항목 |
|------|--------|--------------|
| Model | `OrderStatus` | MVC 스켈레톤 |
| Model | `Sample` | MVC 스켈레톤 |
| Model | `Order` | MVC 스켈레톤 |
| Model | `ProductionJob` | MVC 스켈레톤 |
| Repository | `JsonFileStorage` | 데이터 영속성 처리 |
| Repository | `SampleRepository` | 데이터 영속성 처리 |
| Repository | `OrderRepository` | 데이터 영속성 처리 |
| Repository | `ProductionJobRepository` | 데이터 영속성 처리 |
| Tool | `DataMonitorTool` | 데이터 모니터링 Tool |
| Tool | `DummyDataGenerator` | Dummy 데이터 생성 Tool |

---

## 3. 1-A: MVC 스켈레톤 + 도메인 모델

### 패키지 구조

```
src/main/java/org/example/
├── model/          ← 도메인 객체, 순수 Java, 의존 없음
├── repository/     ← JSON 파일 읽기/쓰기
├── controller/     ← 비즈니스 로직 (Phase 2부터 구현)
├── view/           ← 화면 출력·입력 (Phase 2부터 구현)
└── tool/           ← 관리자 도구 (DataMonitorTool, DummyDataGenerator)
```

이번 Phase에서 `controller/`, `view/`는 빈 패키지로만 생성한다.

---

### 3-1. OrderStatus

```java
package org.example.model;

public enum OrderStatus {
    RESERVED,   // 주문 접수
    REJECTED,   // 주문 거절
    PRODUCING,  // 재고 부족으로 생산 중
    CONFIRMED,  // 출고 대기
    RELEASE     // 출고 완료
}
```

---

### 3-2. Sample

```java
package org.example.model;

public final class Sample {
    private final String sampleId;          // "S-001"
    private final String name;              // "실리콘 웨이퍼-8인치"
    private final double avgProductionTime; // 분/개 (예: 0.5)
    private final double yield;             // 수율 (0 < yield <= 1)
    private int stock;                      // 현재 재고 수량

    public Sample(String sampleId, String name, double avgProductionTime, double yield, int stock) { ... }

    // getters
    public String getSampleId() { ... }
    public String getName() { ... }
    public double getAvgProductionTime() { ... }
    public double getYield() { ... }
    public int getStock() { ... }

    // 재고 변경
    public void addStock(int amount) { ... }      // amount > 0 검증
    public void subtractStock(int amount) { ... } // amount > 0, amount <= stock 검증
}
```

**유효성 규칙:**
- `sampleId`, `name`: null 또는 빈 문자열 불가
- `avgProductionTime` > 0
- `0 < yield <= 1`
- `stock >= 0`

---

### 3-3. Order

```java
package org.example.model;

import java.time.LocalDateTime;

public final class Order {
    private final String orderId;           // "ORD-20260416-0001"
    private final String sampleId;
    private final String customerName;
    private final int quantity;             // > 0
    private OrderStatus status;
    private final LocalDateTime createdAt;

    public Order(String orderId, String sampleId, String customerName,
                 int quantity, OrderStatus status, LocalDateTime createdAt) { ... }

    // getters ...

    // 상태 전이 (허용된 전이만, 그 외 IllegalStateException)
    public void changeStatus(OrderStatus newStatus) { ... }
}
```

**허용 상태 전이:**

```
RESERVED  → CONFIRMED
RESERVED  → PRODUCING
RESERVED  → REJECTED
PRODUCING → CONFIRMED
CONFIRMED → RELEASE
```

그 외 전이 시도 시 `IllegalStateException` 발생.

---

### 3-4. ProductionJob

```java
package org.example.model;

import java.time.LocalDateTime;

public final class ProductionJob {
    private final String orderId;
    private final String sampleId;
    private final int shortage;               // 부족분
    private final int actualProductionQty;    // ceil(shortage / (yield * 0.9))
    private final double totalProductionTime; // avgProductionTime * actualProductionQty
    private final LocalDateTime enqueuedAt;

    public ProductionJob(String orderId, String sampleId, int shortage,
                         int actualProductionQty, double totalProductionTime,
                         LocalDateTime enqueuedAt) { ... }

    // getters (setter 없음 — 불변)
}
```

실 생산량 계산은 Controller에서 수행 후 주입. `ProductionJob`은 순수 데이터 홀더.

---

## 4. 1-B: 데이터 영속성 (JSON CRUD)

### 4-1. JsonFileStorage

```java
package org.example.repository;

public final class JsonFileStorage {
    private static final String DATA_DIR = "data";

    private JsonFileStorage() {}

    // filename 기준으로 data/ 하위 파일 전체 내용을 문자열로 반환
    public static String read(String filename) { ... }

    // 문자열을 data/filename 에 덮어쓰기 저장
    public static void write(String filename, String content) { ... }

    // data/ 디렉터리 없으면 자동 생성
    private static void ensureDataDir() { ... }
}
```

**역할 분리:**
- `JsonFileStorage`: 파일 I/O만 담당 (read/write)
- 각 Repository: 엔티티 ↔ JSON 문자열 변환 직접 담당

**JSON 파싱 전략 (순수 Java):**
- 저장: 각 필드를 직접 `"key": "value"` 형태로 문자열 조합
- 로드: `split`, `trim`, `replace` 등으로 키-값 추출
- 배열 형태: `[{ ... }, { ... }]`

---

### 4-2. SampleRepository

```java
package org.example.repository;

public final class SampleRepository {
    private static final String FILE = "samples.json";

    public List<Sample> findAll() { ... }
    public Optional<Sample> findById(String sampleId) { ... }
    public List<Sample> findByNameContaining(String keyword) { ... }
    public void save(Sample sample) { ... }          // 중복 ID 시 IllegalArgumentException
    public void updateStock(String sampleId, int newStock) { ... }
    public boolean existsById(String sampleId) { ... }
}
```

---

### 4-3. OrderRepository

```java
package org.example.repository;

public final class OrderRepository {
    private static final String FILE = "orders.json";

    public List<Order> findAll() { ... }
    public Optional<Order> findById(String orderId) { ... }
    public List<Order> findByStatus(OrderStatus status) { ... }
    public void save(Order order) { ... }
    public void updateStatus(String orderId, OrderStatus newStatus) { ... }
    public int nextSequence() { ... }   // 전체 주문 수 + 1
}
```

---

### 4-4. ProductionJobRepository

```java
package org.example.repository;

public final class ProductionJobRepository {
    private static final String FILE = "production_jobs.json";

    public List<ProductionJob> findAll() { ... }             // enqueuedAt 오름차순
    public Optional<ProductionJob> findFirst() { ... }       // 큐 앞단
    public Optional<ProductionJob> findByOrderId(String orderId) { ... }
    public void save(ProductionJob job) { ... }
    public void deleteByOrderId(String orderId) { ... }
}
```

---

### JSON 파일 형식

#### data/samples.json

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

#### data/orders.json

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

#### data/production_jobs.json

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

---

## 5. 1-C: 데이터 모니터링 Tool

### 목적

개발·운영 중 `data/*.json`에 저장된 실제 데이터를 빠르게 확인하기 위한 관리자 도구.  
별도 `main()` 메서드로 실행하며, 메인 앱(`Main.java`)과 독립적으로 동작한다.

### 실행 방법

```bash
./gradlew run --args="monitor"
# 또는
java -cp build/classes/java/main org.example.tool.DataMonitorTool
```

### 출력 형식

```
============================================================
  데이터 모니터링 Tool  —  2026-04-16 09:32:15
============================================================

[시료 목록]  (총 5종)
ID       이름                    생산시간    수율    재고
--------------------------------------------------------------
S-001    실리콘 웨이퍼-8인치     0.5 min    0.92    480 ea
S-002    GaN 에피택셜-4인치      0.3 min    0.78    220 ea
S-003    SiC 파워기판-6인치      0.8 min    0.92     30 ea
S-004    포토레지스트-PR7        0.2 min    0.95    910 ea
S-005    산화막 웨이퍼-SiO2      0.6 min    0.88      0 ea

[주문 목록]  (총 10건)
주문번호               시료ID    고객명          수량     상태        접수일시
----------------------------------------------------------------------
ORD-20260416-0001    S-001     LG이노텍        300 ea   RESERVED    2026-04-16T09:30
ORD-20260416-0002    S-002     SK하이닉스      150 ea   CONFIRMED   2026-04-16T09:31
...

[생산 작업 큐]  (총 2건, FIFO 순)
순서   주문번호               시료ID    부족분    실생산량    총생산시간    등록일시
------------------------------------------------------------------------
1      ORD-20260416-0003    S-003     170 ea    206 ea     164.8 min    2026-04-16T09:31
2      ORD-20260416-0004    S-005     150 ea    190 ea     114.0 min    2026-04-16T09:32
============================================================
```

### 클래스 설계

```java
package org.example.tool;

public final class DataMonitorTool {

    public static void main(String[] args) {
        printSamples();
        printOrders();
        printProductionJobs();
    }

    private static void printSamples() { ... }
    private static void printOrders() { ... }
    private static void printProductionJobs() { ... }
}
```

Repository를 직접 사용하여 데이터를 읽고 출력한다.

---

## 6. 1-D: Dummy 데이터 생성 Tool

### 목적

테스트 시나리오를 빠르게 구성하기 위한 도구.  
실행 시 미리 정의된 더미 데이터를 `data/*.json`에 저장한다.  
**기존 데이터를 모두 초기화하고 덮어쓴다.**

### 실행 방법

```bash
./gradlew run --args="dummy"
# 또는
java -cp build/classes/java/main org.example.tool.DummyDataGenerator
```

### 생성 데이터 명세

#### 시료 (5종)

| ID | 이름 | 생산시간 | 수율 | 재고 |
|----|------|---------|------|------|
| S-001 | 실리콘 웨이퍼-8인치 | 0.5 | 0.92 | 480 |
| S-002 | GaN 에피택셜-4인치 | 0.3 | 0.78 | 220 |
| S-003 | SiC 파워기판-6인치 | 0.8 | 0.92 | 30 |
| S-004 | 포토레지스트-PR7 | 0.2 | 0.95 | 910 |
| S-005 | 산화막 웨이퍼-SiO2 | 0.6 | 0.88 | 0 |

#### 주문 (10건)

| 주문번호 | 시료 | 고객 | 수량 | 상태 |
|---------|------|------|------|------|
| ORD-20260416-0001 | S-001 | LG이노텍 | 300 | RESERVED |
| ORD-20260416-0002 | S-002 | SK하이닉스 | 150 | RESERVED |
| ORD-20260416-0003 | S-003 | 삼성전자 파운드리 | 200 | RESERVED |
| ORD-20260416-0004 | S-004 | DB하이텍 | 400 | CONFIRMED |
| ORD-20260416-0005 | S-001 | 인텔코리아 | 100 | CONFIRMED |
| ORD-20260416-0006 | S-002 | 퀄컴코리아 | 80 | CONFIRMED |
| ORD-20260416-0007 | S-003 | 삼성전자 파운드리 | 200 | PRODUCING |
| ORD-20260416-0008 | S-005 | LG이노텍 | 150 | PRODUCING |
| ORD-20260416-0009 | S-004 | SK하이닉스 | 500 | RELEASE |
| ORD-20260416-0010 | S-001 | DB하이텍 | 200 | RELEASE |

#### 생산 작업 (2건 — PRODUCING 주문 대응)

| 주문번호 | 시료 | 부족분 | 실생산량 | 총생산시간 |
|---------|------|------|---------|---------|
| ORD-20260416-0007 | S-003 | 170 | 206 | 164.8 |
| ORD-20260416-0008 | S-005 | 150 | 190 | 114.0 |

### 클래스 설계

```java
package org.example.tool;

public final class DummyDataGenerator {

    public static void main(String[] args) {
        generateSamples();
        generateOrders();
        generateProductionJobs();
        System.out.println("더미 데이터 생성 완료.");
    }

    private static void generateSamples() { ... }
    private static void generateOrders() { ... }
    private static void generateProductionJobs() { ... }
}
```

Repository의 `save()`를 사용하되, 먼저 기존 파일을 삭제하고 새로 저장한다.

---

## 7. 레이어 의존 방향

```
Model          ← 의존 없음 (순수 Java 객체)
Repository     ← Model + JsonFileStorage
Tool           ← Repository (DataMonitorTool, DummyDataGenerator)
Controller     ← Repository + Model  (Phase 2 이후 구현)
View           ← Controller          (Phase 2 이후 구현)
```

---

## 8. TDD 테스트 계획

### OrderStatus

| 테스트 메서드 | 검증 내용 |
|--------------|----------|
| `모든_상태값이_존재한다` | 5개 상태 모두 존재 확인 |

### Sample

| 테스트 메서드 | 검증 내용 |
|--------------|----------|
| `시료를_정상_생성한다` | 필드값 일치 |
| `재고를_추가한다` | `addStock` 후 증가 확인 |
| `재고를_차감한다` | `subtractStock` 후 감소 확인 |
| `재고보다_많이_차감하면_예외` | `IllegalArgumentException` |
| `수율이_범위_밖이면_예외` | 0 이하 또는 1 초과 시 예외 |

### Order

| 테스트 메서드 | 검증 내용 |
|--------------|----------|
| `주문을_정상_생성한다` | 필드값 일치 |
| `RESERVED에서_CONFIRMED로_전이한다` | 상태 변경 확인 |
| `RESERVED에서_PRODUCING으로_전이한다` | 상태 변경 확인 |
| `RESERVED에서_REJECTED로_전이한다` | 상태 변경 확인 |
| `PRODUCING에서_CONFIRMED로_전이한다` | 상태 변경 확인 |
| `CONFIRMED에서_RELEASE로_전이한다` | 상태 변경 확인 |
| `허용되지_않은_전이는_예외` | `REJECTED → CONFIRMED` 시도 시 예외 |

### ProductionJob

| 테스트 메서드 | 검증 내용 |
|--------------|----------|
| `생산작업을_정상_생성한다` | 필드값 일치 |

### JsonFileStorage

| 테스트 메서드 | 검증 내용 |
|--------------|----------|
| `데이터를_저장하고_불러온다` | 저장 후 로드 시 동일 내용 반환 |
| `파일이_없으면_빈_문자열_반환` | 존재하지 않는 파일 로드 시 빈 값 |

### SampleRepository

| 테스트 메서드 | 검증 내용 |
|--------------|----------|
| `시료를_저장하고_조회한다` | `save` → `findById` 동일 객체 |
| `전체_목록을_조회한다` | `findAll` 결과 수 확인 |
| `이름으로_검색한다` | 키워드 포함 시료만 반환 |
| `중복_ID_저장시_예외` | 동일 ID 재등록 시 예외 |
| `재고를_업데이트한다` | `updateStock` 후 조회 시 반영 |

### OrderRepository

| 테스트 메서드 | 검증 내용 |
|--------------|----------|
| `주문을_저장하고_조회한다` | `save` → `findById` 반환 확인 |
| `상태별_주문을_조회한다` | `findByStatus(RESERVED)` 필터링 |
| `상태를_업데이트한다` | `updateStatus` 후 조회 시 반영 |
| `일련번호가_순차_증가한다` | 저장 건수에 따라 `nextSequence` 값 확인 |

### ProductionJobRepository

| 테스트 메서드 | 검증 내용 |
|--------------|----------|
| `생산작업을_저장하고_조회한다` | `save` → `findByOrderId` 반환 확인 |
| `FIFO_순서로_조회한다` | 먼저 저장한 작업이 `findFirst` 로 반환 |
| `작업을_삭제한다` | `deleteByOrderId` 후 조회 시 없음 |

---

## 9. 테스트 격리 전략

Repository 테스트는 실제 파일 I/O를 사용한다.  
`@BeforeEach`에서 테스트용 임시 파일 경로를 주입하거나 기존 파일을 삭제하고,  
`@AfterEach`에서 생성된 테스트 파일을 삭제하여 테스트 간 격리를 보장한다.

```java
@BeforeEach
void setUp() throws IOException {
    // 테스트 전 data/ 디렉터리의 json 파일 삭제
    Files.deleteIfExists(Path.of("data/samples.json"));
}

@AfterEach
void tearDown() throws IOException {
    Files.deleteIfExists(Path.of("data/samples.json"));
}
```

---

## 10. 완료 기준

- [ ] 전체 테스트 통과 (`./gradlew test`)
- [ ] `DummyDataGenerator` 실행 후 `data/*.json` 생성 확인
- [ ] `DataMonitorTool` 실행 후 더미 데이터 테이블 출력 확인
- [ ] 프로그램 재시작 후 데이터 유지 확인
- [ ] 컴파일 경고 없음
