# Phase 6 설계 — Progress Bar + 재고 화면 개선

## 1. 목표

`MonitoringView.showStockStatus()`의 잔여율 컬럼에 시각적 Progress Bar를 추가한다.  
숫자만 표시되던 잔여율이 막대 그래프로 한눈에 파악된다.

---

## 2. 구현 대상

| 클래스 | 변경 내용 |
|--------|-----------|
| `ConsoleHelper` | `progressBar(int ratio)` 메서드 추가 |
| `MonitoringView` | `showStockStatus()` — Progress Bar 컬럼 추가 |

---

## 3. ConsoleHelper.progressBar() 설계

### 동작 규칙

- `ratio`: 0~100 정수 (잔여율 %)
- 10칸 막대 표시: `█`(채워짐) + `░`(빔)
- `filled = ratio / 10` → 정수 나눗셈으로 칸 수 결정
- 색상 규칙 (7단계):

| 구간 | 색상 | 의미 |
|------|------|------|
| `ratio == 0` | RED BOLD | 완전 고갈 |
| `1 ~ 19` | RED | 위험 |
| `20 ~ 39` | MAGENTA | 경계 |
| `40 ~ 59` | YELLOW | 주의 |
| `60 ~ 79` | YELLOW BOLD | 보통 |
| `80 ~ 99` | CYAN | 양호 |
| `ratio == 100` | GREEN | 완전 여유 |

### 구현

```java
public static String progressBar(int ratio) {
    int filled = ratio / 10;
    String bar = "█".repeat(filled) + "░".repeat(10 - filled);
    String color;
    if      (ratio == 0)  color = AnsiColor.RED + AnsiColor.BOLD;
    else if (ratio < 20)  color = AnsiColor.RED;
    else if (ratio < 40)  color = AnsiColor.MAGENTA;
    else if (ratio < 60)  color = AnsiColor.YELLOW;
    else if (ratio < 80)  color = AnsiColor.YELLOW + AnsiColor.BOLD;
    else if (ratio < 100) color = AnsiColor.CYAN;
    else                  color = AnsiColor.GREEN;
    return AnsiColor.color("[" + bar + "]", color);
}
```

### 출력 예시

| ratio | 출력 (색상 제외) | 색상 |
|-------|------------------|------|
| 0 | `[░░░░░░░░░░]` | RED BOLD |
| 10 | `[█░░░░░░░░░]` | RED |
| 20 | `[██░░░░░░░░]` | MAGENTA |
| 40 | `[████░░░░░░]` | YELLOW |
| 60 | `[██████░░░░]` | YELLOW BOLD |
| 80 | `[████████░░]` | CYAN |
| 100 | `[██████████]` | GREEN |

---

## 4. MonitoringView.showStockStatus() 변경

### 변경 전 출력

```
  시료명                    재고      미처리주문   상태    잔여율
  ──────────────────────────────────────────────────────────────
  실리콘 웨이퍼-8인치        480 ea    100 ea  여유          80%
  GaN 에피택셜-4인치         220 ea    500 ea  부족          44%
  SiC 파워기판-6인치           0 ea    200 ea  고갈           0%
```

### 변경 후 출력

```
  시료명                    재고      미처리주문   상태    잔여율
  ──────────────────────────────────────────────────────────────
  실리콘 웨이퍼-8인치        480 ea    100 ea  여유    [████████░░]  80%
  GaN 에피택셜-4인치         220 ea    500 ea  부족    [████░░░░░░]  44%
  SiC 파워기판-6인치           0 ea    200 ea  고갈    [░░░░░░░░░░]   0%
  포토레지스트-PR7            910 ea      0 ea  여유    [██████████] 100%
  산화막 웨이퍼-SiO2            0 ea      0 ea  고갈    [░░░░░░░░░░]   0%
```

### 코드 변경

```java
// 변경 전
ConsoleHelper.println(String.format("  %-24s %6d ea %8d ea  %-10s  %4d%%",
    info.getSample().getName(),
    stock, pending,
    AnsiColor.stockStatusColored(info.getStatus()),
    ratio));

// 변경 후
ConsoleHelper.println(String.format("  %-24s %6d ea %8d ea  %-10s  %s  %3d%%",
    info.getSample().getName(),
    stock, pending,
    AnsiColor.stockStatusColored(info.getStatus()),
    ConsoleHelper.progressBar(ratio),
    ratio));
```

`%s` 자리에 `progressBar(ratio)`가 들어간다.  
ANSI 코드는 눈에 보이지 않는 문자이므로 진행 막대 12자(`[` + 10칸 + `]`)의 시각적 너비는 고정된다.

---

## 5. 재고량 0 & 미처리주문 0 케이스 처리

현재 코드:
```java
int ratio = total == 0 ? 0 : (int) (stock * 100.0 / total);
```

재고와 미처리 주문이 모두 0이면 `total == 0` → `ratio = 0` → RED 바 출력.  
이는 "데이터 없음"이지 고갈이 아니므로 아래와 같이 표기한다.

```java
int ratio = total == 0 ? 100 : (int) (stock * 100.0 / total);
```

재고 0, 미처리 0 → "주문 없음, 재고 없음" → 잔여율 100%로 간주(GREEN 표시).  
단, 재고 0이면 `고갈` 상태가 이미 별도 표시되므로 혼란 없음.

> **의논 포인트:** `total == 0` 케이스를 100%로 처리하는 것이 맞는지 확인 필요.  
> 현재 `S-005(산화막 웨이퍼)` 는 재고 0, 미처리 0 → `고갈` + 0%로 표시된다.  
> 0% → RED는 "위험"처럼 보이지만 실제로는 "미사용 상태"일 수 있다.

---

## 6. TDD 테스트 계획

`progressBar()`는 순수 문자열 변환 함수로 자동 테스트 대상이다.  
`ConsoleHelperTest`에 5개 테스트 추가.

```java
@Test
@DisplayName("progressBar(0)은 전체 빈 막대를 RED로 출력한다")
void progressBar_0_전체빈막대_RED() {
    String result = ConsoleHelper.progressBar(0);
    assertTrue(result.contains("░░░░░░░░░░"));
    assertTrue(result.contains(AnsiColor.RED));
}

@Test
@DisplayName("progressBar(100)은 전체 채워진 막대를 GREEN으로 출력한다")
void progressBar_100_전체채워짐_GREEN() {
    String result = ConsoleHelper.progressBar(100);
    assertTrue(result.contains("██████████"));
    assertTrue(result.contains(AnsiColor.GREEN));
}

@Test
@DisplayName("progressBar(50)은 절반 채워진 막대를 YELLOW로 출력한다")
void progressBar_50_절반채워짐_YELLOW() {
    String result = ConsoleHelper.progressBar(50);
    assertTrue(result.contains("█████░░░░░"));
    assertTrue(result.contains(AnsiColor.YELLOW));
}

@Test
@DisplayName("progressBar(60)은 6칸 채워진 막대를 GREEN으로 출력한다")
void progressBar_60_GREEN_경계값() {
    String result = ConsoleHelper.progressBar(60);
    assertTrue(result.contains("██████░░░░"));
    assertTrue(result.contains(AnsiColor.GREEN));
}

@Test
@DisplayName("progressBar()는 대괄호로 막대를 감싼다")
void progressBar_대괄호로_감싼다() {
    String result = ConsoleHelper.progressBar(50);
    assertTrue(result.contains("["));
    assertTrue(result.contains("]"));
}
```

`showStockStatus()` 변경은 View 레이어로 수동 확인.

---

## 7. 작업 목록

| # | 작업 | 파일 | TDD |
|---|------|------|-----|
| 6-1 | `ConsoleHelperTest` — `progressBar()` 테스트 5개 추가 | `ConsoleHelperTest.java` | RED |
| 6-2 | `ConsoleHelper.progressBar()` 구현 | `ConsoleHelper.java` | GREEN |
| 6-3 | `MonitoringView.showStockStatus()` — Progress Bar 컬럼 추가 | `MonitoringView.java` | 수동 |
| 6-4 | `total == 0` 케이스 처리 방향 결정 후 적용 | `MonitoringView.java` | 수동 |

---

## 8. 완료 기준

- [ ] `ConsoleHelperTest` 신규 5개 통과
- [ ] Progress Bar 비율 정확성 수동 확인
- [ ] 0% → RED, 1~59% → YELLOW, 60~100% → GREEN 색상 구분 확인
- [ ] 기존 테스트 78개 전체 통과
- [ ] 컴파일 경고 없음
