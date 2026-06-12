# Phase 3 설계 — 메인 메뉴 + 공통 색상 적용

## 1. 목표

메인 메뉴 요약·메뉴 번호·서브메뉴 헤더에 색상을 적용한다.  
Phase 1에서 완성한 `AnsiColor` 상수를 실제 출력 코드에 연결하는 단계다.

**이 Phase에서 상태 뱃지·성공/실패 메시지 색상은 다루지 않는다.** (→ Phase 4)

---

## 2. 구현 대상

| 클래스 | 변경 내용 |
|--------|-----------|
| `ConsoleHelper` | `printHeader()` — 제목 텍스트에 CYAN BOLD 적용 |
| `MainView` | `printSummary()` — 중복 헤더 제거, 숫자 색상 적용 |
| `MainView` | `printMenu()` — 메뉴 번호 색상 적용 |

**`printHeader()` 수정 1개로 6개 서브메뉴 헤더가 일괄 적용된다.**  
서브메뉴 파일(SampleView, OrderView 등)은 직접 수정하지 않는다.

---

## 3. 현재 코드 분석

### 3-1. 중복 헤더 문제

`printBanner()`가 이미 출력하는 내용을 `printSummary()`가 다시 출력하고 있다.

```
[printBanner 출력]
  S-Semi ASCII 아트
  반도체 시료 생산주문관리 시스템    ← ①
================================================================ ← ②

[printSummary 출력]

================================================================ ← ③ 중복
        반도체 시료 생산주문관리 시스템  ← ④ 중복
================================================================ ← ⑤ 중복
시스템 현황  2026-06-12 ...
```

Phase 3에서 `printSummary()`의 ③④⑤를 제거한다.

### 3-2. 색상이 없는 숫자 출력

```java
// 현재
System.out.printf("등록 시료 %3d종    총 재고 %6d ea%n", sampleCount, totalStock);
System.out.printf("전체 주문 %3d건    생산라인 %3d건 대기    승인 대기 %3d건%n",
    orderCount, productionCount, reservedCount);
```

### 3-3. 색상이 없는 메뉴 번호

```java
// 현재
ConsoleHelper.println("  [1] 시료 관리          [2] 시료 주문");
...
ConsoleHelper.println("  [0] 종료");
```

---

## 4. 상세 설계

### 4-1. ConsoleHelper.printHeader() — CYAN BOLD 적용

제목 텍스트에만 색상을 적용하고, 구분선(`===`)은 색상 없이 유지한다.

```java
// 변경 전
public static void printHeader(String title) {
    System.out.println(SEPARATOR);
    System.out.println("  " + title);
    System.out.println(SEPARATOR);
}

// 변경 후
public static void printHeader(String title) {
    System.out.println(SEPARATOR);
    System.out.println(AnsiColor.color("  " + title, AnsiColor.HEADER));
    System.out.println(SEPARATOR);
}
```

**적용 결과 (자동 전파):**

| 서브메뉴 | printHeader 호출 텍스트 |
|----------|------------------------|
| SampleView | `[1] 시료 관리` |
| OrderView | `[2] 시료 주문` |
| ApprovalView | `[3] 주문 승인/거절` |
| MonitoringView | `[4] 모니터링  2026-06-12 ...` |
| ProductionView | `[5] 생산라인 조회  FIFO 방식` |
| ReleaseView | `[6] 출고 처리` |

---

### 4-2. MainView.printSummary() — 중복 제거 + 숫자 색상

#### 중복 헤더 제거

`printBanner()`가 타이틀과 구분선을 이미 출력하므로 `printSummary()`에서 제거한다.

#### 숫자 색상 규칙

| 값 | 색상 | 이유 |
|----|------|------|
| 등록 시료 수 | GREEN BOLD (`SUCCESS`) | 시스템 현황 긍정 지표 |
| 총 재고 | GREEN BOLD (`SUCCESS`) | 시스템 현황 긍정 지표 |
| 전체 주문 수 | BOLD (`NUMBER`) | 중립 정보 |
| 생산라인 대기 수 | MAGENTA | 진행 중 상태 |
| 승인 대기 수 | YELLOW (`WARN`) | 처리 필요 주의 |

#### printf → 문자열 조합 방식 변경

ANSI 코드는 눈에 보이지 않는 문자를 포함하므로 `%s` 포맷 내부에서 너비 지정이
맞지 않는다. 각 숫자를 색상 적용 후 문자열로 조합한다.

```java
// 변경 전
private void printSummary() {
    int sampleCount = sampleController.findAll().size();
    int totalStock = sampleController.findAll().stream().mapToInt(s -> s.getStock()).sum();
    int orderCount = orderController.findAllOrders().size();
    int productionCount = productionController.getQueue().size();

    ConsoleHelper.println("");
    ConsoleHelper.printSeparator();
    ConsoleHelper.println("        반도체 시료 생산주문관리 시스템");
    ConsoleHelper.printSeparator();
    ConsoleHelper.println("시스템 현황  " + LocalDateTime.now().format(FMT));
    ConsoleHelper.println("");
    int reservedCount = orderController.findReservedOrders().size();
    System.out.printf("등록 시료 %3d종    총 재고 %6d ea%n", sampleCount, totalStock);
    System.out.printf("전체 주문 %3d건    생산라인 %3d건 대기    승인 대기 %3d건%n",
        orderCount, productionCount, reservedCount);
}

// 변경 후
private void printSummary() {
    int sampleCount   = sampleController.findAll().size();
    int totalStock    = sampleController.findAll().stream().mapToInt(s -> s.getStock()).sum();
    int orderCount    = orderController.findAllOrders().size();
    int productionCount = productionController.getQueue().size();
    int reservedCount = orderController.findReservedOrders().size();

    ConsoleHelper.println("시스템 현황  " + LocalDateTime.now().format(FMT));
    ConsoleHelper.println("");

    String sc  = AnsiColor.color(String.format("%3d", sampleCount),    AnsiColor.SUCCESS);
    String ts  = AnsiColor.color(String.format("%6d", totalStock),     AnsiColor.SUCCESS);
    String oc  = AnsiColor.boldNum(String.format("%3d", orderCount));
    String pc  = AnsiColor.color(String.format("%3d", productionCount), AnsiColor.MAGENTA);
    String rc  = AnsiColor.color(String.format("%3d", reservedCount),   AnsiColor.WARN);

    ConsoleHelper.println("등록 시료 " + sc + "종    총 재고 " + ts + " ea");
    ConsoleHelper.println("전체 주문 " + oc + "건    생산라인 " + pc + "건 대기    승인 대기 " + rc + "건");
}
```

---

### 4-3. MainView.printMenu() — 메뉴 번호 색상

`[1]`~`[6]`은 `AnsiColor.menuNum()` (CYAN BOLD), `[0]`은 RED.

```java
// 변경 전
private void printMenu() {
    ConsoleHelper.printThinLine();
    ConsoleHelper.println("  [1] 시료 관리          [2] 시료 주문");
    ConsoleHelper.println("  [3] 주문 승인/거절     [4] 모니터링");
    ConsoleHelper.println("  [5] 생산라인 조회      [6] 출고 처리");
    ConsoleHelper.println("  [0] 종료");
    ConsoleHelper.printThinLine();
}

// 변경 후
private void printMenu() {
    String n0 = AnsiColor.color("[0]", AnsiColor.RED);
    ConsoleHelper.printThinLine();
    ConsoleHelper.println("  " + AnsiColor.menuNum("1") + " 시료 관리          " + AnsiColor.menuNum("2") + " 시료 주문");
    ConsoleHelper.println("  " + AnsiColor.menuNum("3") + " 주문 승인/거절     " + AnsiColor.menuNum("4") + " 모니터링");
    ConsoleHelper.println("  " + AnsiColor.menuNum("5") + " 생산라인 조회      " + AnsiColor.menuNum("6") + " 출고 처리");
    ConsoleHelper.println("  " + n0 + " 종료");
    ConsoleHelper.printThinLine();
}
```

---

## 5. 변경 후 화면 예시

```
  ____       ____                 _
 / ___|     / ___|  ___ _ __ ___ (_)
 \___ \ ____\___ \ / _ \ '_ ` _ \| |
  ___) |_____|__) |  __/ | | | | | |
 |____/     |____/ \___|_| |_| |_|_|

        반도체 시료 생산주문관리 시스템
================================================================
시스템 현황  2026-06-12 14:32:07

등록 시료   5종    총 재고   1640 ea     ← 5, 1640: GREEN BOLD
전체 주문  10건    생산라인   2건 대기    승인 대기   3건
                              ↑ MAGENTA              ↑ YELLOW
----------------------------------------------------------------
  [1] 시료 관리          [2] 시료 주문   ← [1]~[6]: CYAN BOLD
  [3] 주문 승인/거절     [4] 모니터링
  [5] 생산라인 조회      [6] 출고 처리
  [0] 종료                              ← [0]: RED
----------------------------------------------------------------
선택 >
```

---

## 6. TDD 테스트 계획

`printHeader()`는 `ConsoleHelper`의 순수 출력 메서드로, stdout 캡처로 검증 가능하다.  
`printSummary()`와 `printMenu()`는 View 레이어 동작으로 수동 확인한다.

### ConsoleHelperTest 추가 테스트

```java
@Test
@DisplayName("printHeader()는 제목 텍스트에 CYAN 색상을 적용한다")
void printHeader_제목에_CYAN_색상을_적용한다() {
    ConsoleHelper.printHeader("테스트 헤더");

    String output = outContent.toString(StandardCharsets.UTF_8);
    assertTrue(output.contains(AnsiColor.CYAN));
}

@Test
@DisplayName("printHeader()는 제목 텍스트에 BOLD를 적용한다")
void printHeader_제목에_BOLD를_적용한다() {
    ConsoleHelper.printHeader("테스트 헤더");

    String output = outContent.toString(StandardCharsets.UTF_8);
    assertTrue(output.contains(AnsiColor.BOLD));
}

@Test
@DisplayName("printHeader()는 제목 텍스트를 포함한다")
void printHeader_제목_텍스트를_포함한다() {
    ConsoleHelper.printHeader("테스트 헤더");

    String output = outContent.toString(StandardCharsets.UTF_8);
    assertTrue(output.contains("테스트 헤더"));
}
```

---

## 7. 작업 목록

| # | 작업 | 변경 파일 | TDD |
|---|------|-----------|-----|
| 3-1 | `ConsoleHelperTest` — `printHeader()` 테스트 3개 추가 | `ConsoleHelperTest.java` | RED |
| 3-2 | `ConsoleHelper.printHeader()` — CYAN BOLD 적용 | `ConsoleHelper.java` | GREEN |
| 3-3 | `MainView.printSummary()` — 중복 헤더 제거 + 숫자 색상 | `MainView.java` | 수동 확인 |
| 3-4 | `MainView.printMenu()` — 메뉴 번호 색상 | `MainView.java` | 수동 확인 |

---

## 8. 완료 기준

- [ ] `ConsoleHelperTest` 신규 3개 통과
- [ ] 메인 메뉴 숫자 색상 출력 확인
- [ ] 메인 메뉴 번호 CYAN BOLD / `[0]` RED 확인
- [ ] 모든 서브메뉴 헤더 CYAN BOLD 통일 확인
- [ ] 기존 테스트 전체 통과 (65개 이상)
- [ ] 컴파일 경고 없음
