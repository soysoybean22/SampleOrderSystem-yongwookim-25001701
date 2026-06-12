# Phase 4 설계 — 상태 뱃지 + 메시지 색상 전체 적용

## 1. 목표

모든 View에서 출력하는 상태값·성공·오류·취소·안내 메시지에 색상을 적용한다.  
Phase 1에서 완성한 `AnsiColor` 메서드를 실제 출력 코드에 연결하는 단계다.

---

## 2. 색상 적용 규칙 (전체 공통)

| 유형 | 적용 방법 | 출력 예시 |
|------|-----------|-----------|
| 성공 메시지 | `AnsiColor.SUCCESS` + `✓` 접두어 | `✓ 등록 완료` |
| 오류 메시지 | `AnsiColor.ERROR` + `✗` 접두어 | `✗ [오류] 등록되지 않은 시료 ID` |
| 취소 메시지 | `AnsiColor.WARN` | `취소되었습니다.` |
| 안내사항 `※`·`*` | `AnsiColor.WARN` | `※ 재고 확인은 [3] 메뉴에서 진행하세요.` |
| 주문 상태 | `AnsiColor.statusBadge(status)` | `[CONFIRMED]` → GREEN |
| 재고 상태 | `AnsiColor.stockStatusColored(status)` | `부족` → YELLOW BOLD |
| 재고 충분 표시 | `AnsiColor.SUCCESS` | `← 충분` |
| 재고 부족 표시 | `AnsiColor.WARN` | `← 부족 (n ea 모자람)` |
| 생산 중 상태 표시 | `AnsiColor.MAGENTA` | `현재 상태: RUNNING` |

---

## 3. 구현 대상

| 클래스 | 변경 메서드 |
|--------|-------------|
| `SampleView` | `registerSample()` |
| `OrderView` | `placeOrder()` |
| `ApprovalView` | `processApproval()`, `handleApprovalConfirm()` |
| `ProductionView` | `run()`, `printCurrentJob()`, `printFootnote()`, `completeProduction()` |
| `MonitoringView` | `showOrderSummary()`, `showStockStatus()` |
| `ReleaseView` | `processRelease()` |

---

## 4. 뷰별 상세 변경

### 4-1. SampleView

#### registerSample()

```java
// 변경 전
ConsoleHelper.println("  등록 완료: " + name + " (" + sampleId + ")");
// ...
ConsoleHelper.println("  [오류] " + e.getMessage());

// 변경 후
ConsoleHelper.println(AnsiColor.color("  ✓ 등록 완료: " + name + " (" + sampleId + ")", AnsiColor.SUCCESS));
// ...
ConsoleHelper.println(AnsiColor.color("  ✗ [오류] " + e.getMessage(), AnsiColor.ERROR));
```

---

### 4-2. OrderView

#### placeOrder()

```java
// 변경 전
ConsoleHelper.println("  [오류] 등록되지 않은 시료 ID입니다: " + sampleId);
// ...
ConsoleHelper.println("  [오류] 수량은 1 이상이어야 합니다.");
// ...
ConsoleHelper.println("  주문이 취소되었습니다.");
// ...
ConsoleHelper.println("예약 접수 완료.");
System.out.printf("  현재 상태  %s%n", order.getStatus());
// ...
ConsoleHelper.println("  ※ 재고 확인 및 승인은 [3] 주문 승인/거절 메뉴에서 진행하세요.");

// 변경 후
ConsoleHelper.println(AnsiColor.color("  ✗ [오류] 등록되지 않은 시료 ID입니다: " + sampleId, AnsiColor.ERROR));
// ...
ConsoleHelper.println(AnsiColor.color("  ✗ [오류] 수량은 1 이상이어야 합니다.", AnsiColor.ERROR));
// ...
ConsoleHelper.println(AnsiColor.color("  주문이 취소되었습니다.", AnsiColor.WARN));
// ...
ConsoleHelper.println(AnsiColor.color("✓ 예약 접수 완료.", AnsiColor.SUCCESS));
ConsoleHelper.println("  현재 상태  " + AnsiColor.statusBadge(order.getStatus()));
// ...
ConsoleHelper.println(AnsiColor.color("  ※ 재고 확인 및 승인은 [3] 주문 승인/거절 메뉴에서 진행하세요.", AnsiColor.WARN));
```

---

### 4-3. ApprovalView

#### processApproval() — 재고 충분/부족 표시

```java
// 변경 전
System.out.printf("  현재 재고  %d ea    ← 충분%n", stock);
// ...
System.out.printf("  현재 재고  %d ea    ← 부족 (%d ea 모자람)%n", stock, shortage);
ConsoleHelper.println("  재고 부족. 생산이 필요합니다.");

// 변경 후
ConsoleHelper.println("  현재 재고  " + stock + " ea    "
    + AnsiColor.color("← 충분", AnsiColor.SUCCESS));
// ...
ConsoleHelper.println("  현재 재고  " + stock + " ea    "
    + AnsiColor.color("← 부족 (" + shortage + " ea 모자람)", AnsiColor.WARN));
ConsoleHelper.println(AnsiColor.color("  재고 부족. 생산이 필요합니다.", AnsiColor.WARN));
```

#### handleApprovalConfirm() — 상태 전환 뱃지 + 완료/거절 메시지

```java
// 변경 전
ConsoleHelper.println("승인 완료.");
System.out.printf("  상태 변경  RESERVED → CONFIRMED%n");
// ...
ConsoleHelper.println("승인 완료.");
System.out.printf("  상태 변경  RESERVED → PRODUCING%n");
ConsoleHelper.println("  생산 큐에 등록되었습니다.");
// ...
ConsoleHelper.println("거절 완료.");
System.out.printf("  상태 변경  RESERVED → REJECTED%n");

// 변경 후
ConsoleHelper.println(AnsiColor.color("✓ 승인 완료.", AnsiColor.SUCCESS));
ConsoleHelper.println("  상태 변경  "
    + AnsiColor.statusBadge(OrderStatus.RESERVED) + " → " + AnsiColor.statusBadge(OrderStatus.CONFIRMED));
// ...
ConsoleHelper.println(AnsiColor.color("✓ 승인 완료.", AnsiColor.SUCCESS));
ConsoleHelper.println("  상태 변경  "
    + AnsiColor.statusBadge(OrderStatus.RESERVED) + " → " + AnsiColor.statusBadge(OrderStatus.PRODUCING));
ConsoleHelper.println(AnsiColor.color("  생산 큐에 등록되었습니다.", AnsiColor.WARN));
// ...
ConsoleHelper.println(AnsiColor.color("✗ 거절 완료.", AnsiColor.ERROR));
ConsoleHelper.println("  상태 변경  "
    + AnsiColor.statusBadge(OrderStatus.RESERVED) + " → " + AnsiColor.statusBadge(OrderStatus.REJECTED));
```

> **import 추가 필요:** `ApprovalView`에 `import org.example.model.OrderStatus;` 추가.

---

### 4-4. ProductionView

#### run() — 생산라인 상태 표시

```java
// 변경 전
ConsoleHelper.println("생산라인  단일 라인    현재 상태: IDLE");
ConsoleHelper.println("  현재 생산 대기 중인 작업이 없습니다.");
// ...
ConsoleHelper.println("생산라인  단일 라인    현재 상태: RUNNING");

// 변경 후
ConsoleHelper.println("생산라인  단일 라인    현재 상태: "
    + AnsiColor.color("IDLE", AnsiColor.SUCCESS));
ConsoleHelper.println(AnsiColor.color("  현재 생산 대기 중인 작업이 없습니다.", AnsiColor.WARN));
// ...
ConsoleHelper.println("생산라인  단일 라인    현재 상태: "
    + AnsiColor.color("RUNNING", AnsiColor.MAGENTA + AnsiColor.BOLD));
```

#### printCurrentJob() — 섹션 헤더

```java
// 변경 전
ConsoleHelper.println("[ 현재 처리 중 ]");

// 변경 후
ConsoleHelper.println(AnsiColor.color("[ 현재 처리 중 ]", AnsiColor.MAGENTA + AnsiColor.BOLD));
```

#### printFootnote() — 안내 주석

```java
// 변경 전
ConsoleHelper.println("  * 부족분 = 주문량 - 재고,  실생산량 = ceil(부족분 / (수율 × 0.9))");
ConsoleHelper.println("  * FIFO 방식으로 처리됩니다.");

// 변경 후
ConsoleHelper.println(AnsiColor.color("  * 부족분 = 주문량 - 재고,  실생산량 = ceil(부족분 / (수율 × 0.9))", AnsiColor.WARN));
ConsoleHelper.println(AnsiColor.color("  * FIFO 방식으로 처리됩니다.", AnsiColor.WARN));
```

#### completeProduction() — 완료/취소 메시지 + 상태 전환 뱃지

```java
// 변경 전
ConsoleHelper.println("  취소되었습니다.");
// ...
ConsoleHelper.println("생산 완료.");
ConsoleHelper.println("  상태 변경   PRODUCING → CONFIRMED");

// 변경 후
ConsoleHelper.println(AnsiColor.color("  취소되었습니다.", AnsiColor.WARN));
// ...
ConsoleHelper.println(AnsiColor.color("✓ 생산 완료.", AnsiColor.SUCCESS));
ConsoleHelper.println("  상태 변경  "
    + AnsiColor.statusBadge(OrderStatus.PRODUCING) + " → " + AnsiColor.statusBadge(OrderStatus.CONFIRMED));
```

> **import 추가 필요:** `ProductionView`에 `import org.example.model.OrderStatus;` 추가.

---

### 4-5. MonitoringView

#### showOrderSummary() — 상태 뱃지 + 안내

```java
// 변경 전
String suffix = entry.getKey() == OrderStatus.PRODUCING ? "  ← 생산라인 대기" : "";
System.out.printf("  %-12s %4d건%s%n", entry.getKey(), entry.getValue(), suffix);
// ...
System.out.printf("  * 거절된 주문  %d건  (참고용)%n", ...);

// 변경 후
String suffix = entry.getKey() == OrderStatus.PRODUCING
    ? AnsiColor.color("  ← 생산라인 대기", AnsiColor.MAGENTA) : "";
ConsoleHelper.println("  " + AnsiColor.statusBadge(entry.getKey())
    + String.format(" %4d건", entry.getValue()) + suffix);
// ...
ConsoleHelper.println(AnsiColor.color(
    String.format("  * 거절된 주문  %d건  (참고용)", monitoringController.getRejectedCount()),
    AnsiColor.WARN));
```

#### showStockStatus() — 재고 상태 색상 + 안내

```java
// 변경 전
System.out.printf("  %-24s %6d ea %8d ea  %-4s  %4d%%%n",
    info.getSample().getName(), stock, pending, info.getStatus(), ratio);
// ...
ConsoleHelper.println("  * 미처리 주문 = CONFIRMED + PRODUCING 상태 주문 수량 합계");
ConsoleHelper.println("  * 잔여율 = 재고 / (재고 + 미처리 주문) × 100");

// 변경 후
ConsoleHelper.println(String.format("  %-24s %6d ea %8d ea  %-10s  %4d%%",
    info.getSample().getName(), stock, pending,
    AnsiColor.stockStatusColored(info.getStatus()), ratio));
// ...
ConsoleHelper.println(AnsiColor.color("  * 미처리 주문 = CONFIRMED + PRODUCING 상태 주문 수량 합계", AnsiColor.WARN));
ConsoleHelper.println(AnsiColor.color("  * 잔여율 = 재고 / (재고 + 미처리 주문) × 100", AnsiColor.WARN));
```

> **printf → println 전환 이유:** `stockStatusColored()`가 ANSI 코드를 포함하므로  
> `%-4s` 너비 지정이 맞지 않는다. `println` + `String.format`으로 교체한다.

---

### 4-6. ReleaseView

#### processRelease() — 완료/취소 메시지 + 상태 전환 뱃지

```java
// 변경 전
ConsoleHelper.println("  취소되었습니다.");
// ...
ConsoleHelper.println("출고 처리 완료.");
ConsoleHelper.println("  상태 변경   CONFIRMED → RELEASE");

// 변경 후
ConsoleHelper.println(AnsiColor.color("  취소되었습니다.", AnsiColor.WARN));
// ...
ConsoleHelper.println(AnsiColor.color("✓ 출고 처리 완료.", AnsiColor.SUCCESS));
ConsoleHelper.println("  상태 변경  "
    + AnsiColor.statusBadge(OrderStatus.CONFIRMED) + " → " + AnsiColor.statusBadge(OrderStatus.RELEASE));
```

> **import 추가 필요:** `ReleaseView`에 `import org.example.model.OrderStatus;` 추가.

---

## 5. TDD 테스트 계획

`AnsiColor.statusBadge()`와 `stockStatusColored()`는 Phase 1에서 이미 테스트 완료.  
View 레이어 출력 메서드는 콘솔 출력 특성상 자동 테스트 대상이 아니다.

신규 자동 테스트 없음. 기존 68개 테스트 전체 통과로 완료 확인.

수동 확인 항목:

| View | 확인 항목 |
|------|-----------|
| `SampleView` | 등록 완료(GREEN), 오류(RED) |
| `OrderView` | 접수 완료(GREEN), 상태 뱃지, 취소(YELLOW), 안내(YELLOW) |
| `ApprovalView` | 충분(GREEN)/부족(YELLOW), 상태 전환 뱃지, 승인(GREEN), 거절(RED) |
| `ProductionView` | IDLE(GREEN)/RUNNING(MAGENTA), 생산 완료(GREEN), 주석(YELLOW) |
| `MonitoringView` | 상태 뱃지, 재고 상태 색상, 안내(YELLOW) |
| `ReleaseView` | 출고 완료(GREEN), 상태 전환 뱃지, 취소(YELLOW) |

---

## 6. 작업 목록

| # | 작업 | 변경 파일 |
|---|------|-----------|
| 4-1 | `SampleView.registerSample()` 색상 적용 | `SampleView.java` |
| 4-2 | `OrderView.placeOrder()` 색상 적용 | `OrderView.java` |
| 4-3 | `ApprovalView.processApproval()` 색상 적용 | `ApprovalView.java` |
| 4-4 | `ApprovalView.handleApprovalConfirm()` 상태 뱃지 + 색상 | `ApprovalView.java` |
| 4-5 | `ProductionView.run()` 상태 표시 색상 | `ProductionView.java` |
| 4-6 | `ProductionView.printCurrentJob()` 섹션 헤더 색상 | `ProductionView.java` |
| 4-7 | `ProductionView.printFootnote()` 안내 색상 | `ProductionView.java` |
| 4-8 | `ProductionView.completeProduction()` 완료/취소/뱃지 | `ProductionView.java` |
| 4-9 | `MonitoringView.showOrderSummary()` 상태 뱃지 + 안내 | `MonitoringView.java` |
| 4-10 | `MonitoringView.showStockStatus()` 재고 상태 색상 + 안내 | `MonitoringView.java` |
| 4-11 | `ReleaseView.processRelease()` 완료/취소/뱃지 | `ReleaseView.java` |

---

## 7. 완료 기준

- [ ] 모든 상태값이 색상 뱃지로 표시됨
- [ ] 성공/오류/취소/안내 메시지 색상 구분 확인
- [ ] 기존 테스트 68개 전체 통과
- [ ] 컴파일 경고 없음

---

## 8. 버그 수정 — OrderView Enter 안내 없음

### 원인

`OrderView.placeOrder()` 완료 후 `run()`이 반환되면 `MainView` 루프가 즉시
`clearScreen()`을 호출해 결과 화면이 사라진다. 사용자는 Enter를 눌러야 한다는
안내가 없어서 상위 메뉴로 이동하는 방법을 알기 어렵다.

### 수정

`placeOrder()` 의 세 종료 지점에 `[Enter] 메뉴로 돌아가기` 프롬프트 추가.

| 종료 지점 | 추가 위치 |
|-----------|-----------|
| 존재하지 않는 시료 ID 오류 → `return` 직전 | `✗ [오류]` 출력 후 |
| `[N]` 취소 → `return` 직전 | `취소되었습니다.` 출력 후 |
| 주문 접수 완료 → 메서드 끝 | `※ 재고 확인 및 승인은...` 출력 후 |

```java
// 오류 종료
ConsoleHelper.println(AnsiColor.color("  ✗ [오류] 등록되지 않은 시료 ID입니다: " + sampleId, AnsiColor.ERROR));
ConsoleHelper.readLine("\n  [Enter] 메뉴로 돌아가기");
return;

// 취소 종료
ConsoleHelper.println(AnsiColor.color("  주문이 취소되었습니다.", AnsiColor.WARN));
ConsoleHelper.readLine("\n  [Enter] 메뉴로 돌아가기");
return;

// 완료 종료
ConsoleHelper.println(AnsiColor.color("  ※ 재고 확인 및 승인은 [3] 주문 승인/거절 메뉴에서 진행하세요.", AnsiColor.WARN));
ConsoleHelper.readLine("\n  [Enter] 메뉴로 돌아가기");
```
