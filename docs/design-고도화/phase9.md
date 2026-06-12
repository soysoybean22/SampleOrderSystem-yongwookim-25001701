# Phase 9 설계 — CMD 가로 너비 확장

## 1. 목표

현재 64자 기준의 화면 너비를 **120자**로 확장한다.  
(64×2=128이지만 터미널 표준 너비인 120자를 채택한다.)

표·구분선·배너가 더 넓게 표시되어 데이터 가독성이 향상된다.

---

## 2. 변경 범위 요약

| 요소 | 현재 | 변경 후 |
|------|------|---------|
| `SEPARATOR` (`===...`) | 64 | 120 |
| `THIN_LINE` (`---...`) | 64 | 120 |
| 표 기본 너비 (`printTableTop()` 등) | 내부 62 (전체 64) | 내부 118 (전체 120) |
| `ReleaseView` 표 너비 | 내부 80 (전체 82) | 내부 118 (전체 120) — 기본으로 통일 |
| `printBanner()` | 좌측 고정 | 120자 기준 중앙 정렬 |
| 각 View 컬럼 너비 | 기존 | 여분 공간 배분 (아래 상세) |

---

## 3. ConsoleHelper 변경

### 3-1. 상수

```java
// 변경 전
private static final String SEPARATOR = "=".repeat(64);
private static final String THIN_LINE = "-".repeat(64);

// 변경 후
private static final String SEPARATOR = "=".repeat(120);
private static final String THIN_LINE = "-".repeat(120);
```

`printSeparator()`, `printThinLine()`, `printHeader()`는 자동 반영된다.

### 3-2. 표 기본 너비

```java
// 변경 전
public static void printTableTop()     { printTableTop(62); }
public static void printTableDivider() { printTableDivider(62); }
public static void printTableBottom()  { printTableBottom(62); }

// 변경 후
public static void printTableTop()     { printTableTop(118); }
public static void printTableDivider() { printTableDivider(118); }
public static void printTableBottom()  { printTableBottom(118); }
```

### 3-3. printBanner() — 중앙 정렬

ASCII 아트 최대 너비 ≈ 48자. 중앙 정렬 여백 = (120 - 48) / 2 ≈ 36자.  
텍스트 블록 내 각 줄 앞에 공백을 추가해 중앙 정렬한다.

```java
// 변경 전
String art = """
      ____       ____                 _
     / ___|     / ___|  ___ _ __ ___ (_)
     ...
    """;
System.out.println("        반도체 시료 생산주문관리 시스템");

// 변경 후 (약 30자 추가 들여쓰기)
String art = """
                                    ____       ____                 _
                                   / ___|     / ___|  ___ _ __ ___ (_)
                                   \\___ \\ ____\\___ \\ / _ \\ '_ ` _ \\| |
                                    ___) |_____|__) |  __/ | | | | | |
                                   |____/     |____/ \\___|_| |_| |_|_|
                                  """;
System.out.println("                                        반도체 시료 생산주문관리 시스템");
```

---

## 4. 뷰별 컬럼 너비 변경

모든 표는 내부 118자 (전체 120자). 표 행의 기본 구조:
`│` (1) + 내용 (118) + `│` (1) = 120자

### 4-1. SampleView — 시료 목록 / 검색 결과

여분 56자를 이름 컬럼에 집중 배분한다.

| 컬럼 | 현재 표시 너비 | 변경 후 |
|------|--------------|---------|
| ID | 8 | 8 (유지) |
| 이름 | 22 display | **70 display** |
| 생산시간 | 13 | 13 (유지) |
| 수율 | 5 | 5 (유지) |
| 재고 | 8 | 8 (유지) |
| 여백 | — | 8 trailing |

**데이터 행:**
```java
// 변경 전
System.out.printf("│  %-8s %s %7.1f min  %5.2f  %5d ea│%n",
    s.getSampleId(), padRight(s.getName(), 22), ...);

// 변경 후
System.out.printf("│  %-8s %s %7.1f min  %5.2f  %5d ea        │%n",
    s.getSampleId(), padRight(s.getName(), 70), ...);
```

검증: 1+2+8+1+70+1+7+6+5+2+5+3+8+1 = **120** ✓

**헤더 행:**
```java
// 변경 후
ConsoleHelper.println("│  " + padRight("ID", 8)
    + " " + padRight("이름", 70)
    + " " + padRight("생산시간", 13)
    + padRight("수율", 5)
    + "  " + padRight("재고", 8) + "        │");
```

---

### 4-2. ApprovalView — 승인 대기 목록

| 컬럼 | 현재 | 변경 후 |
|------|------|---------|
| 번호 | 5 display | 5 (유지) |
| 주문번호 | 22 | 22 (유지) |
| 고객명 | 18 display | **54 display** |
| 수량 | 13 | 13 (유지) |
| trailing | — | 25 |

**데이터 행 1:**  
검증: 1+2+5+22+1+54+1+8+25+1 = **120** ✓

**데이터 행 2 (시료명):**  
`│        시료: ` (15) + `padRight(sampleName, 104)` + `│`  
검증: 1+8+6+104+1 = **120** ✓

---

### 4-3. ReleaseView — 출고 처리 목록

기존 `printTableTop(80)` → `printTableTop()` (기본 118)으로 통일.

| 컬럼 | 현재 | 변경 후 |
|------|------|---------|
| 번호 | 5 display | 5 (유지) |
| 주문번호 | 22 | 22 (유지) |
| 시료명 | 22 display | **36 display** |
| 고객명 | 18 display | **36 display** |
| 수량 | 8 | 8 (유지) |
| trailing | — | 7 |

검증: 1+2+5+22+1+36+1+36+1+8+6+1 = **120** ✓

---

### 4-4. MonitoringView — 재고량 확인

| 컬럼 | 현재 표시 너비 | 변경 후 |
|------|--------------|---------|
| 시료명 | 24 display | **38 display** |
| 재고 | 6+3 = 9 | 9 (유지) |
| 미처리주문 | 8+3 = 11 | 11 (유지) |
| 상태 | 10 display | 10 (유지) |
| Progress Bar | 12 | 12 (유지) |
| 잔여율 | 4+1 = 5 | 5 (유지) |
| trailing | — | 27 |

검증: 2+38+1+9+1+11+2+10+2+12+2+5+27 = **122**... 조정 필요.

> **구현 시 정확한 너비를 재측정해 trailing을 조정한다.**

---

### 4-5. MainView — 메인 메뉴

`printMenu()`의 메뉴 항목 간격을 120자에 맞춰 좌우 균형 있게 재배치한다.

```java
// 변경 전 (64자 기준, 두 메뉴 항목이 한 줄)
"  [1] 시료 관리          [2] 시료 주문"

// 변경 후 (120자 기준, 간격 확대)
"  [1] 시료 관리                        [2] 시료 주문"
```

`printSummary()` 데이터 행은 현재 길이 그대로 유지한다 (숫자·상태 표시).

---

### 4-6. ProductionView — 대기 큐

| 컬럼 | 현재 | 변경 후 |
|------|------|---------|
| 순서 | 4 | 4 (유지) |
| 주문번호 | 22 | 22 (유지) |
| 시료명 | 22 display | **40 display** |
| 부족분 | 7 | 7 (유지) |
| 실생산량 | 8 | 8 (유지) |
| 총생산시간 | 10 | 10 (유지) |
| trailing | — | 확장 공간 배분 |

---

## 5. TDD 테스트 계획

`SEPARATOR`, `THIN_LINE`, `printTableTop()` 출력 너비는 자동 테스트로 검증한다.

### ConsoleHelperTest 변경·추가

기존 printTableTop/Divider/Bottom 테스트는 문자(`┌`,`┐` 등)만 검증해 너비와 무관하므로 그대로 유지.  
너비 검증 테스트를 추가한다.

```java
@Test
@DisplayName("printSeparator()는 120자 = 구분선을 출력한다")
void printSeparator_120자() {
    ConsoleHelper.printSeparator();
    String output = outContent.toString(StandardCharsets.UTF_8).trim();
    assertEquals(120, output.length());
    assertTrue(output.chars().allMatch(c -> c == '='));
}

@Test
@DisplayName("printThinLine()은 120자 - 구분선을 출력한다")
void printThinLine_120자() {
    ConsoleHelper.printThinLine();
    String output = outContent.toString(StandardCharsets.UTF_8).trim();
    assertEquals(120, output.length());
    assertTrue(output.chars().allMatch(c -> c == '-'));
}

@Test
@DisplayName("printTableTop()은 기본 내부 너비 118을 사용한다")
void printTableTop_기본너비_118() {
    ConsoleHelper.printTableTop();
    String output = outContent.toString(StandardCharsets.UTF_8).trim();
    assertEquals(120, output.length()); // ┌ + 118×─ + ┐
}
```

View 레이어 컬럼 너비 변경은 수동 확인.

---

## 6. 작업 목록

| # | 작업 | 파일 | TDD |
|---|------|------|-----|
| 9-1 | `ConsoleHelperTest` — 너비 검증 3개 추가 | `ConsoleHelperTest.java` | RED |
| 9-2 | `ConsoleHelper` — `SEPARATOR`, `THIN_LINE` 64→120 | `ConsoleHelper.java` | GREEN |
| 9-3 | `ConsoleHelper` — 기본 표 너비 62→118 | `ConsoleHelper.java` | GREEN |
| 9-4 | `ConsoleHelper.printBanner()` — 120자 중앙 정렬 | `ConsoleHelper.java` | 수동 |
| 9-5 | `SampleView` — 표 컬럼 너비 확장 | `SampleView.java` | 수동 |
| 9-6 | `ApprovalView` — 표 컬럼 너비 확장 | `ApprovalView.java` | 수동 |
| 9-7 | `ReleaseView` — `printTableTop(80)` → 기본으로 통일, 컬럼 확장 | `ReleaseView.java` | 수동 |
| 9-8 | `MonitoringView` — `showStockStatus()` 컬럼 확장 | `MonitoringView.java` | 수동 |
| 9-9 | `MainView.printMenu()` — 120자 기준 재배치 | `MainView.java` | 수동 |
| 9-10 | `ProductionView.printWaitingQueue()` — 컬럼 확장 | `ProductionView.java` | 수동 |

---

## 7. 완료 기준

- [ ] `ConsoleHelperTest` 너비 검증 3개 통과
- [ ] 구분선(`===`/`---`)이 120자로 출력 확인
- [ ] 배너 중앙 정렬 확인
- [ ] 시료 목록 이름 컬럼이 더 넓게 표시 확인
- [ ] 승인 대기·출고 처리 목록 컬럼 확장 확인
- [ ] 기존 테스트 91개 전체 통과
- [ ] 컴파일 경고 없음

---

## 8. 논의 포인트

### 8-1. MonitoringView 재고량 확인 컬럼 정확도

섹션 4-4에서 trailing 계산이 맞지 않는다고 표시했다.  
이 컬럼에는 ANSI 색상 코드(`stockStatusColored`, `progressBar`)가 포함되어  
Java 문자열 길이와 화면 표시 너비가 다르다.  
구현 시 ANSI 코드 길이를 제외한 표시 너비 기준으로 재측정해 trailing을 결정한다.

### 8-2. Phase 10과의 순서

Phase 10 (추가 표 테두리)은 Phase 9 완료 후 진행한다.  
`MonitoringView.showStockStatus()`와 `ProductionView.printWaitingQueue()`에  
Phase 10에서 테두리가 추가되므로, Phase 9에서는 컬럼 너비만 조정하고 테두리는 다음 단계에서 추가한다.
