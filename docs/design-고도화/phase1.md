# Phase 1 설계 — AnsiColor 유틸 구현

## 1. 목표

이후 모든 색상·스타일 작업의 기반이 되는 `AnsiColor` 유틸 클래스를 완성한다.  
이 Phase가 끝나면 어떤 View에서든 일관된 방식으로 색상을 적용할 수 있다.

**이 Phase에서 화면 변화는 없다.** 클래스만 추가하고 기존 코드는 수정하지 않는다.

---

## 2. 구현 대상

| 클래스 | 패키지 | 신규/수정 |
|--------|--------|----------|
| `AnsiColor` | `org.example.view` | **신규** |

---

## 3. ANSI 이스케이프 코드 개요

터미널에서 색상·스타일을 적용하는 표준 방법이다.  
`ESC[코드m` 형식으로 출력하면 이후 텍스트에 스타일이 적용되고, `ESC[0m`으로 초기화한다.

```
\033[31m  → 빨강 시작
Hello
\033[0m   → 스타일 초기화
```

Windows 10 1511 이상, CMD/PowerShell 모두 지원한다.  
`chcp 65001` 설정 시 한글과 함께 사용 가능하다.

---

## 4. AnsiColor 클래스 설계

```java
package org.example.view;

import org.example.model.OrderStatus;
import org.example.model.StockStatus;

public final class AnsiColor {

    // ── 기본 색상 ──────────────────────────────────────
    public static final String RESET   = "\033[0m";
    public static final String BOLD    = "\033[1m";
    public static final String RED     = "\033[31m";
    public static final String GREEN   = "\033[32m";
    public static final String YELLOW  = "\033[33m";
    public static final String BLUE    = "\033[34m";
    public static final String MAGENTA = "\033[35m";
    public static final String CYAN    = "\033[36m";

    // ── 용도별 조합 상수 ──────────────────────────────
    public static final String SUCCESS  = GREEN + BOLD;   // ✓ 완료/성공
    public static final String ERROR    = RED + BOLD;     // ✗ 오류/실패
    public static final String WARN     = YELLOW;         // ※ 안내/경고
    public static final String MENU_NUM = CYAN + BOLD;    // [1] 메뉴 번호
    public static final String HEADER   = CYAN + BOLD;    // 헤더/제목
    public static final String NUMBER   = BOLD;           // 주요 숫자값

    private AnsiColor() {}

    // ── 핵심 메서드 ────────────────────────────────────

    /**
     * 텍스트에 ANSI 색상을 적용하고 RESET으로 닫는다.
     */
    public static String color(String text, String ansiCode) {
        return ansiCode + text + RESET;
    }

    /**
     * 주문 상태에 맞는 색상 뱃지를 반환한다.
     * 예) CONFIRMED → "\033[32m\033[1m[CONFIRMED]\033[0m"
     */
    public static String statusBadge(OrderStatus status) {
        return switch (status) {
            case RESERVED  -> color("[RESERVED]",  YELLOW);
            case CONFIRMED -> color("[CONFIRMED]", SUCCESS);
            case PRODUCING -> color("[PRODUCING]", MAGENTA + BOLD);
            case RELEASE   -> color("[RELEASE]",   CYAN + BOLD);
            case REJECTED  -> color("[REJECTED]",  ERROR);
        };
    }

    /**
     * 재고 상태에 맞는 색상 텍스트를 반환한다.
     * 예) 여유 → "\033[32m\033[1m여유\033[0m"
     */
    public static String stockStatusColored(StockStatus status) {
        return switch (status) {
            case 여유 -> color("여유", SUCCESS);
            case 부족 -> color("부족", YELLOW + BOLD);
            case 고갈 -> color("고갈", ERROR);
        };
    }

    /**
     * 메뉴 번호를 색상 적용 형태로 반환한다.
     * 예) menuNum("1") → "\033[36m\033[1m[1]\033[0m"
     */
    public static String menuNum(String num) {
        return color("[" + num + "]", MENU_NUM);
    }

    /**
     * 숫자값에 BOLD를 적용한다.
     * 예) boldNum("480 ea") → "\033[1m480 ea\033[0m"
     */
    public static String boldNum(String value) {
        return color(value, NUMBER);
    }
}
```

---

## 5. 색상 규칙 정리

| 적용 대상 | 색상/스타일 | 상수 |
|-----------|------------|------|
| 완료·성공 메시지 | 초록 + 굵게 | `SUCCESS` |
| 오류·실패 메시지 | 빨강 + 굵게 | `ERROR` |
| 안내사항 `※` | 노랑 | `WARN` |
| 메뉴 번호 `[1]` | 청록 + 굵게 | `MENU_NUM` |
| 헤더·제목 | 청록 + 굵게 | `HEADER` |
| 주요 숫자 | 굵게 | `NUMBER` |
| RESERVED | 노랑 | `YELLOW` |
| CONFIRMED | 초록 + 굵게 | `SUCCESS` |
| PRODUCING | 마젠타 + 굵게 | `MAGENTA + BOLD` |
| RELEASE | 청록 + 굵게 | `CYAN + BOLD` |
| REJECTED | 빨강 + 굵게 | `ERROR` |
| 재고 여유 | 초록 + 굵게 | `SUCCESS` |
| 재고 부족 | 노랑 + 굵게 | `YELLOW + BOLD` |
| 재고 고갈 | 빨강 + 굵게 | `ERROR` |

---

## 6. TDD 테스트 계획

View 레이어는 콘솔 출력 특성상 자동 테스트 대상이 아니지만,  
`AnsiColor`의 순수 문자열 변환 로직은 자동 테스트가 가능하다.

```java
package org.example.view;

class AnsiColorTest {

    @Test
    @DisplayName("color()는 ANSI 코드로 텍스트를 감싼다")
    void color_텍스트를_ANSI코드로_감싼다() {
        String result = AnsiColor.color("hello", AnsiColor.GREEN);
        assertEquals("\033[32mhello\033[0m", result);
    }

    @Test
    @DisplayName("statusBadge()는 CONFIRMED에 초록 굵게를 적용한다")
    void statusBadge_CONFIRMED는_초록굵게() {
        String badge = AnsiColor.statusBadge(OrderStatus.CONFIRMED);
        assertTrue(badge.contains("[CONFIRMED]"));
        assertTrue(badge.contains(AnsiColor.GREEN));
    }

    @Test
    @DisplayName("statusBadge()는 REJECTED에 빨강 굵게를 적용한다")
    void statusBadge_REJECTED는_빨강굵게() {
        String badge = AnsiColor.statusBadge(OrderStatus.REJECTED);
        assertTrue(badge.contains("[REJECTED]"));
        assertTrue(badge.contains(AnsiColor.RED));
    }

    @Test
    @DisplayName("stockStatusColored()는 고갈에 빨강 굵게를 적용한다")
    void stockStatusColored_고갈은_빨강굵게() {
        String result = AnsiColor.stockStatusColored(StockStatus.고갈);
        assertTrue(result.contains("고갈"));
        assertTrue(result.contains(AnsiColor.RED));
    }

    @Test
    @DisplayName("menuNum()은 번호를 대괄호로 감싸고 청록 굵게를 적용한다")
    void menuNum_청록굵게() {
        String result = AnsiColor.menuNum("1");
        assertTrue(result.contains("[1]"));
        assertTrue(result.contains(AnsiColor.CYAN));
    }
}
```

---

## 7. 완료 기준

- [ ] `AnsiColor` 클래스 구현
- [ ] `AnsiColorTest` 전체 통과 (`./gradlew test`)
- [ ] 기존 테스트 56개 전체 통과
- [ ] 컴파일 경고 없음
