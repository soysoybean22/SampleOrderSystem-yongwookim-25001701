# Phase 7 설계 — 표 테두리 개선

## 1. 목표

핵심 목록 화면의 단순 구분선(`─`)을 박스 드로잉 문자로 교체해 시각적 구분을 강화한다.

---

## 2. 구현 대상

| 클래스 | 변경 내용 |
|--------|-----------|
| `ConsoleHelper` | `printTableTop()`, `printTableDivider()`, `printTableBottom()` 추가 |
| `SampleView` | `listSamples()`, `searchSample()` 표 테두리 적용 |
| `ApprovalView` | `printReservedList()` 표 테두리 적용 |
| `ReleaseView` | `printConfirmedList()` 표 테두리 적용 |

---

## 3. 테두리 설계 방침 — 외곽 테두리만 적용

### 컬럼 구분선을 적용하지 않는 이유

한글은 터미널에서 1자가 2컬럼을 차지하지만 Java의 `printf` 너비 지정은 **문자 수** 기준이다.  
`%-22s`로 포맷한 한글 이름은 Java 문자열 길이는 22지만 화면 출력 폭은 더 넓어져,  
컬럼 구분선(`│`)의 위치가 행마다 달라진다.

→ 외곽 테두리(`┌─┐`, `│`, `└─┘`)만 적용해 안정적인 시각 구조를 만든다.

### 변경 전

```
  ID       이름                      생산시간    수율      재고
  ────────────────────────────────────────────────────────────────
  S-001    실리콘 웨이퍼-8인치        0.5 min   0.92    480 ea
  S-002    GaN 에피택셜-4인치         0.3 min   0.78    220 ea
```

### 변경 후

```
  ┌──────────────────────────────────────────────────────────────┐
  │  ID       이름                      생산시간    수율    재고  │
  ├──────────────────────────────────────────────────────────────┤
  │  S-001    실리콘 웨이퍼-8인치        0.5 min   0.92   480 ea │
  │  S-002    GaN 에피택셜-4인치         0.3 min   0.78   220 ea │
  └──────────────────────────────────────────────────────────────┘
```

---

## 4. ConsoleHelper 메서드 추가

기존 `SEPARATOR`(64자)·`THIN_LINE`(64자)과 동일 너비로 설계한다.  
내부 너비 62자 + `│` 양끝 = 64자.

```java
private static final String TABLE_TOP     = "┌" + "─".repeat(62) + "┐";
private static final String TABLE_DIVIDER = "├" + "─".repeat(62) + "┤";
private static final String TABLE_BOTTOM  = "└" + "─".repeat(62) + "┘";

public static void printTableTop()     { System.out.println(TABLE_TOP); }
public static void printTableDivider() { System.out.println(TABLE_DIVIDER); }
public static void printTableBottom()  { System.out.println(TABLE_BOTTOM); }
```

---

## 5. 뷰별 변경 상세

### 5-1. SampleView — listSamples() / searchSample()

데이터 행의 내부 너비가 62자이므로 `│` 양끝을 추가하면 딱 맞는다.

```java
// 변경 전
System.out.printf("  %-8s %-22s %10s  %5s  %7s%n", "ID", "이름", "생산시간", "수율", "재고");
ConsoleHelper.printThinLine();
for (Sample s : items) {
    System.out.printf("  %-8s %-22s %7.1f min  %5.2f  %5d ea%n", ...);
}

// 변경 후
ConsoleHelper.printTableTop();
System.out.printf("│  %-8s %-22s %10s  %5s  %7s  │%n", "ID", "이름", "생산시간", "수율", "재고");
ConsoleHelper.printTableDivider();
for (Sample s : items) {
    System.out.printf("│  %-8s %-22s %7.1f min  %5.2f  %5d ea│%n", ...);
}
ConsoleHelper.printTableBottom();
```

> **헤더 행 너비 조정:** 헤더 포맷(`%7s` → `%7s  `)에 공백 2개를 추가해 내부 너비를 62자로 맞춘다.

---

### 5-2. ApprovalView — printReservedList()

```java
// 변경 전
System.out.printf("  %-4s %-22s %-18s %7s%n", "번호", "주문번호", "고객명", "수량");
ConsoleHelper.printThinLine();
for (...) {
    System.out.printf("  [%d]  %-22s %-18s %5d ea%n", ...);
    System.out.printf("        시료: %s%n", sampleName);
}
ConsoleHelper.println("  [0]  위로");
ConsoleHelper.printThinLine();

// 변경 후
ConsoleHelper.printTableTop();
System.out.printf("│  %-4s %-22s %-18s %7s      │%n", "번호", "주문번호", "고객명", "수량");
ConsoleHelper.printTableDivider();
for (...) {
    System.out.printf("│  [%d]  %-22s %-18s %5d ea     │%n", ...);
    System.out.printf("│        시료: %-44s│%n", sampleName);
}
System.out.printf("│  %-60s│%n", "[0]  위로");
ConsoleHelper.printTableBottom();
```

> **각 행 너비 조정:** 오른쪽 여백을 채워 전체 내부 너비를 62자로 통일한다.

---

### 5-3. ReleaseView — printConfirmedList()

```java
// 변경 전
System.out.printf("  %-4s %-22s %-22s %-18s %7s%n", "번호", "주문번호", "시료명", "고객명", "수량");
ConsoleHelper.printThinLine();
for (...) {
    System.out.printf("  [%d]  %-22s %-22s %-18s %5d ea%n", ...);
}
ConsoleHelper.println("  [0]  위로");
ConsoleHelper.printThinLine();

// 변경 후
ConsoleHelper.printTableTop();
System.out.printf("│  %-4s %-22s %-22s %-18s %5s│%n", "번호", "주문번호", "시료명", "고객명", "수량");
ConsoleHelper.printTableDivider();
for (...) {
    System.out.printf("│  [%d]  %-22s %-22s %-18s %3d ea│%n", ...);
}
System.out.printf("│  %-60s│%n", "[0]  위로");
ConsoleHelper.printTableBottom();
```

> **ReleaseView 내부 너비:** 4+1+22+1+22+1+18+1+5 = 75자로 62를 초과한다.  
> 이 경우 표 너비를 해당 테이블에 맞게 늘리거나, 열 너비를 줄이는 방향으로 조정한다.  
> **구체적인 너비 수치는 구현 시 결정한다.**

---

## 6. TDD 테스트 계획

`printTableTop()`, `printTableDivider()`, `printTableBottom()`은 stdout 캡처로 검증한다.  
`ConsoleHelperTest`에 3개 테스트 추가.

```java
@Test
@DisplayName("printTableTop()은 ┌ 와 ┐ 를 포함한다")
void printTableTop_상단테두리() {
    ConsoleHelper.printTableTop();
    String output = outContent.toString(StandardCharsets.UTF_8);
    assertTrue(output.contains("┌"));
    assertTrue(output.contains("┐"));
    assertTrue(output.contains("─"));
}

@Test
@DisplayName("printTableDivider()는 ├ 와 ┤ 를 포함한다")
void printTableDivider_중간구분선() {
    ConsoleHelper.printTableDivider();
    String output = outContent.toString(StandardCharsets.UTF_8);
    assertTrue(output.contains("├"));
    assertTrue(output.contains("┤"));
}

@Test
@DisplayName("printTableBottom()은 └ 와 ┘ 를 포함한다")
void printTableBottom_하단테두리() {
    ConsoleHelper.printTableBottom();
    String output = outContent.toString(StandardCharsets.UTF_8);
    assertTrue(output.contains("└"));
    assertTrue(output.contains("┘"));
}
```

View 레이어 행 포맷 변경은 수동 확인.

---

## 7. 작업 목록

| # | 작업 | 파일 | TDD |
|---|------|------|-----|
| 7-1 | `ConsoleHelperTest` — 테두리 메서드 테스트 3개 | `ConsoleHelperTest.java` | RED |
| 7-2 | `ConsoleHelper` — `printTableTop/Divider/Bottom()` 구현 | `ConsoleHelper.java` | GREEN |
| 7-3 | `SampleView.listSamples()` 테두리 적용 | `SampleView.java` | 수동 |
| 7-4 | `SampleView.searchSample()` 테두리 적용 | `SampleView.java` | 수동 |
| 7-5 | `ApprovalView.printReservedList()` 테두리 적용 | `ApprovalView.java` | 수동 |
| 7-6 | `ReleaseView.printConfirmedList()` 테두리 적용 | `ReleaseView.java` | 수동 |

---

## 8. 완료 기준

- [ ] `ConsoleHelperTest` 신규 3개 통과
- [ ] 시료 목록 테두리 정상 표시 확인
- [ ] 승인 대기 / 출고 처리 목록 테두리 정상 표시 확인
- [ ] 기존 테스트 85개 전체 통과
- [ ] 컴파일 경고 없음

---

## 9. 논의 포인트

### 9-1. ReleaseView 열 너비 초과

`printConfirmedList()`는 열 너비 합계가 75자로 내부 62자를 초과한다.  
아래 두 가지 방향 중 하나를 선택해야 한다.

**A안 — 표 너비를 늘린다**  
`TABLE_WIDTH`를 77(내부)로 변경하거나, ReleaseView 전용 폭을 사용한다.  
일관성이 떨어진다.

**B안 — 시료명 열을 제거하거나 축약한다**  
`시료명` 컬럼(22자)을 제거하고 상세 정보는 선택 후 표시한다.  
열 수가 줄어 가독성이 오히려 높아질 수 있다.