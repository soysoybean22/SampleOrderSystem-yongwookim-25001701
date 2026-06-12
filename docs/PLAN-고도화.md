# PLAN-고도화 — UI/UX 개선 구현 계획

## 1. 개요

PRD-고도화.md의 요구사항을 7개 Phase로 나눠 순차 구현한다.  
각 Phase는 독립적으로 동작하며, 이전 Phase가 완료된 후 다음으로 진행한다.  
기존 테스트(56개)는 모든 Phase에서 통과 상태를 유지해야 한다.

---

## 2. 영향 범위

### 신규 추가 클래스

| 클래스 | 패키지 | Phase |
|--------|--------|-------|
| `AnsiColor` | `view` | 1 |
| `Paginator<T>` | `view` | 5 |

### 수정 클래스

| 클래스 | 수정 내용 | Phase |
|--------|-----------|-------|
| `ConsoleHelper` | `clearScreen()`, `progressBar()`, 배너 출력 추가 | 2, 6 |
| `MainView` | 배너, 색상, 클리어 적용 | 2, 3 |
| `SampleView` | 색상, 클리어, 페이지네이션 적용 | 3, 4, 5 |
| `OrderView` | 색상, 클리어 적용 | 3, 4 |
| `ApprovalView` | 색상, 클리어, 페이지네이션 적용 | 3, 4, 5 |
| `ProductionView` | 색상, 클리어 적용 | 3, 4 |
| `MonitoringView` | 색상, 클리어, Progress Bar 적용 | 3, 4, 6 |
| `ReleaseView` | 색상, 클리어, 페이지네이션 적용 | 3, 4, 5 |

---

## 3. Phase별 구현 계획

---

### Phase 1. AnsiColor 유틸 구현

**목표:** 이후 모든 색상 작업의 기반이 되는 유틸 클래스 완성

**구현 내용:**

```java
public final class AnsiColor {
    public static final String RESET   = "\033[0m";
    public static final String BOLD    = "\033[1m";
    public static final String RED     = "\033[31m";
    public static final String GREEN   = "\033[32m";
    public static final String YELLOW  = "\033[33m";
    public static final String BLUE    = "\033[34m";
    public static final String MAGENTA = "\033[35m";
    public static final String CYAN    = "\033[36m";

    // 조합 상수
    public static final String SUCCESS  = GREEN + BOLD;   // ✓ 완료
    public static final String ERROR    = RED + BOLD;     // ✗ 오류
    public static final String WARN     = YELLOW;         // ※ 안내
    public static final String MENU_NUM = CYAN + BOLD;    // [1] 메뉴 번호
    public static final String HEADER   = CYAN + BOLD;    // 헤더

    public static String color(String text, String code) {
        return code + text + RESET;
    }

    // 주문 상태 뱃지
    public static String statusBadge(OrderStatus status) {
        return switch (status) {
            case RESERVED  -> color("[RESERVED]",  YELLOW);
            case CONFIRMED -> color("[CONFIRMED]", SUCCESS);
            case PRODUCING -> color("[PRODUCING]", MAGENTA);
            case RELEASE   -> color("[RELEASE]",   CYAN);
            case REJECTED  -> color("[REJECTED]",  ERROR);
        };
    }

    // 재고 상태 색상
    public static String stockStatusColored(StockStatus status) {
        return switch (status) {
            case 여유 -> color("여유", SUCCESS);
            case 부족 -> color("부족", YELLOW + BOLD);
            case 고갈 -> color("고갈", ERROR);
        };
    }
}
```

**작업 목록:**

| # | 작업 | 확인 방법 |
|---|------|-----------|
| 1-1 | `AnsiColor` 클래스 구현 | 컴파일 확인 |
| 1-2 | `statusBadge()` 구현 | 컴파일 확인 |
| 1-3 | `stockStatusColored()` 구현 | 컴파일 확인 |

**완료 기준:**
- [ ] 컴파일 통과
- [ ] 기존 테스트 전체 통과

---

### Phase 2. S-Semi 배너 + 화면 클리어

**목표:** 시작 배너 출력 및 메뉴 전환 시 화면 클리어

**구현 내용:**

`ConsoleHelper`에 두 메서드 추가:

```java
// 화면 클리어 (ANSI 이스케이프)
public static void clearScreen() {
    System.out.print("\033[H\033[2J");
    System.out.flush();
}

// S-Semi ASCII 배너 출력
public static void printBanner() {
    System.out.println(AnsiColor.color("""
          ____       ____                 _
         / ___|     / ___|  ___ _ __ ___ (_)
         \\___ \\ ____\\___ \\ / _ \\ '_ ` _ \\| |
          ___) |_____|__) |  __/ | | | | | |
         |____/     |____/ \\___|_| |_| |_|_|
        """, AnsiColor.CYAN + AnsiColor.BOLD));
    System.out.println("        반도체 시료 생산주문관리 시스템");
    ConsoleHelper.printSeparator();
}
```

**적용 위치:**

| 위치 | 동작 |
|------|------|
| `MainView.run()` 루프 매 반복 | `clearScreen()` → `printBanner()` → 요약 출력 |
| 각 서브메뉴 `run()` 진입 시 | `clearScreen()` 후 헤더 출력 |

**작업 목록:**

| # | 작업 | 확인 방법 |
|---|------|-----------|
| 2-1 | `ConsoleHelper.clearScreen()` 구현 | 수동 확인 |
| 2-2 | `ConsoleHelper.printBanner()` 구현 | 수동 확인 |
| 2-3 | `Main.java` 배너 호출 추가 | 수동 확인 |
| 2-4 | `MainView` 루프 매 반복 `clearScreen()` + `printBanner()` 출력 | 수동 확인 |
| 2-5 | 각 서브메뉴 `run()` 진입 시 클리어 적용 | 수동 확인 |
| 2-6 | `build.gradle.kts` — Windows 배치 스크립트에 `chcp 65001` 자동 삽입 | 수동 확인 |

**완료 기준:**
- [x] 실행 시 배너 1회 출력 확인
- [x] 메뉴 이동 시 이전 출력이 남지 않음 확인
- [x] 첫 화면에서 배너가 clearScreen으로 지워지지 않음 확인
- [x] Windows CMD/PowerShell에서 한글 깨짐 없음 확인
- [x] 기존 테스트 전체 통과

---

### Phase 3. 메인 메뉴 + 공통 색상 적용

**목표:** 메인 메뉴 요약·메뉴 번호·헤더에 색상 적용

**MainView 요약 색상:**

```
시스템 현황  2026-04-16 09:32:15

등록 시료   5종    총 재고   1,640 ea        ← 숫자: GREEN BOLD
전체 주문  10건    생산라인   2건 대기    승인 대기  3건
                             ↑MAGENTA         ↑YELLOW
```

**메뉴 번호 색상:**

```
  [1] 시료 관리          [2] 시료 주문     ← [숫자]: CYAN BOLD
  [3] 주문 승인/거절     [4] 모니터링
  [5] 생산라인 조회      [6] 출고 처리
  [0] 종료                                 ← [0]: RED
```

**헤더 색상:**

```
[1] 시료 관리    ← CYAN BOLD
─────────────────
```

**작업 목록:**

| # | 작업 | 확인 방법 |
|---|------|-----------|
| 3-1 | `MainView.printSummary()` 색상 적용 | 수동 확인 |
| 3-2 | `MainView.printMenu()` 메뉴 번호 색상 | 수동 확인 |
| 3-3 | `ConsoleHelper.printHeader()` CYAN BOLD 적용 | 수동 확인 |
| 3-4 | 각 서브메뉴 헤더 색상 통일 | 수동 확인 |

**완료 기준:**
- [x] 메인 메뉴 색상 출력 확인
- [x] 모든 서브메뉴 헤더 색상 통일 확인
- [x] 기존 테스트 전체 통과

---

### Phase 4. 상태 뱃지 + 메시지 색상 전체 적용

**목표:** 모든 View에서 상태값·성공·오류·안내 메시지에 색상 적용

**적용 기준:**

| 유형 | 적용 방법 | 예시 |
|------|-----------|------|
| 주문 상태 | `AnsiColor.statusBadge()` | `[CONFIRMED]` → GREEN |
| 성공 메시지 | `AnsiColor.SUCCESS` | `✓ 예약 접수 완료.` |
| 오류 메시지 | `AnsiColor.ERROR` | `✗ [오류] 등록되지 않은 시료 ID` |
| 취소 메시지 | `AnsiColor.WARN` | `취소되었습니다.` |
| 안내사항 | `AnsiColor.WARN` | `※ 재고 확인은 [3] 메뉴에서 진행하세요.` |
| 주요 숫자 | `AnsiColor.BOLD` | `200 ea`, `164.8 min` |

**적용 View 목록:**

| View | 적용 내용 |
|------|-----------|
| `SampleView` | 등록 완료(GREEN), 오류(RED) |
| `OrderView` | 접수 완료(GREEN), 상태 뱃지, 취소(YELLOW), 안내(YELLOW) |
| `ApprovalView` | 재고 충분(GREEN)/부족(YELLOW), 상태 전환 뱃지, 승인완료(GREEN), 거절(RED) |
| `ProductionView` | 생산중 상태(MAGENTA), 완료(GREEN), 안내(YELLOW) |
| `MonitoringView` | 상태 뱃지, 재고 상태 색상 |
| `ReleaseView` | 출고 완료(GREEN), 상태 뱃지, 취소(YELLOW) |

**작업 목록:**

| # | 작업 | 확인 방법 |
|---|------|-----------|
| 4-1 | `SampleView` 색상 적용 | 수동 확인 |
| 4-2 | `OrderView` 색상 적용 | 수동 확인 |
| 4-3 | `ApprovalView` 색상 적용 | 수동 확인 |
| 4-4 | `ProductionView` 색상 적용 | 수동 확인 |
| 4-5 | `MonitoringView` 상태 뱃지 적용 | 수동 확인 |
| 4-6 | `ReleaseView` 색상 적용 | 수동 확인 |

**완료 기준:**
- [x] 모든 상태값이 색상 뱃지로 표시됨
- [x] 성공/오류/안내 메시지 색상 구분 확인
- [x] 기존 테스트 전체 통과

---

### Phase 5. 페이지네이션 (5행 제한)

**목표:** 목록이 5행 초과 시 페이지 단위로 분할 표시

**Paginator<T> 구현:**

```java
public final class Paginator<T> {
    public static final int PAGE_SIZE = 5;

    private final List<T> items;
    private int page = 0; // 0-based

    public Paginator(List<T> items) { this.items = items; }

    public List<T> currentItems() {
        int from = page * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, items.size());
        return items.subList(from, to);
    }

    public boolean hasNext() { return (page + 1) * PAGE_SIZE < items.size(); }
    public boolean hasPrev() { return page > 0; }
    public void nextPage()   { if (hasNext()) page++; }
    public void prevPage()   { if (hasPrev()) page--; }
    public int totalPages()  { return (int) Math.ceil((double) items.size() / PAGE_SIZE); }
    public int currentPage() { return page + 1; } // 1-based 표시용
    public boolean needsPagination() { return items.size() > PAGE_SIZE; }

    public String pageInfo() {
        return AnsiColor.color(
            String.format("페이지 %d / %d", currentPage(), totalPages()),
            AnsiColor.CYAN);
    }
}
```

**페이지 네비게이션 UI:**

```
  [1]  ORD-20260416-0001    LG이노텍      실리콘 웨이퍼   300 ea
  [2]  ORD-20260416-0002    SK하이닉스    GaN 에피택셜    150 ea
  [3]  ORD-20260416-0003    삼성전자      SiC 파워기판    200 ea
  [4]  ORD-20260416-0004    DB하이텍      포토레지스트    400 ea
  [5]  ORD-20260416-0005    인텔코리아    실리콘 웨이퍼   100 ea

  ◀ 이전 [P]    페이지 1 / 2    다음 [N] ▶
  번호 선택 또는 [P/N] 페이지 이동, [0] 위로 >
```

**적용 대상:**

| View | 대상 목록 |
|------|-----------|
| `SampleView` | 시료 목록, 검색 결과 |
| `ApprovalView` | RESERVED 주문 목록 |
| `ReleaseView` | CONFIRMED 주문 목록 |

**작업 목록:**

| # | 작업 | 확인 방법 |
|---|------|-----------|
| 5-1 | `Paginator<T>` 구현 | 컴파일 확인 |
| 5-2 | `SampleView` 목록 페이지네이션 적용 | 수동 — 6건 이상 등록 후 확인 |
| 5-3 | `ApprovalView` RESERVED 목록 페이지네이션 | 수동 — 6건 이상 주문 후 확인 |
| 5-4 | `ReleaseView` CONFIRMED 목록 페이지네이션 | 수동 — 6건 이상 승인 후 확인 |

**완료 기준:**
- [ ] 5건 이하: 페이지 UI 미표시 확인
- [ ] 6건 이상: 5건씩 분할, P/N 이동 확인
- [ ] 페이지 번호 `페이지 1 / N` 표시 확인
- [ ] 기존 테스트 전체 통과

---

### Phase 6. Progress Bar + 재고 화면 개선

**목표:** 모니터링 재고량 화면에 Progress Bar와 색상 적용

**Progress Bar 구현 (`ConsoleHelper`에 추가):**

```java
public static String progressBar(int ratio) {
    // ratio: 0~100, 10칸 표시
    int filled = ratio / 10;
    String bar = "█".repeat(filled) + "░".repeat(10 - filled);
    String color = ratio == 0 ? AnsiColor.RED
                 : ratio < 60 ? AnsiColor.YELLOW
                 : AnsiColor.GREEN;
    return AnsiColor.color("[" + bar + "]", color);
}
```

**재고량 확인 화면 (변경 후):**

```
  시료명                    재고      미처리주문   상태    잔여율
  ──────────────────────────────────────────────────────────────
  실리콘 웨이퍼-8인치        480 ea    100 ea    여유    [████████░░]  80%
  GaN 에피택셜-4인치         220 ea    500 ea    부족    [████░░░░░░]  44%
  SiC 파워기판-6인치           0 ea    200 ea    고갈    [░░░░░░░░░░]   0%
  포토레지스트-PR7            910 ea      0 ea    여유    [██████████] 100%
  산화막 웨이퍼-SiO2            0 ea      0 ea    고갈    [░░░░░░░░░░]   0%
```

- `여유` → GREEN BOLD
- `부족` → YELLOW BOLD
- `고갈` → RED BOLD
- Progress Bar → 비율에 따라 GREEN / YELLOW / RED

**작업 목록:**

| # | 작업 | 확인 방법 |
|---|------|-----------|
| 6-1 | `ConsoleHelper.progressBar()` 구현 | 수동 확인 |
| 6-2 | `MonitoringView.showStockStatus()` Progress Bar 적용 | 수동 확인 |
| 6-3 | 재고 상태 색상 (`AnsiColor.stockStatusColored()`) 적용 | 수동 확인 |

**완료 기준:**
- [x] Progress Bar 비율 정확성 확인
- [x] 여유/부족/고갈 색상 구분 확인
- [x] 기존 테스트 전체 통과

---

### Phase 7. 표 테두리 개선 (선택)

**목표:** 핵심 목록에 박스 드로잉 문자로 시각적 구분 강화

> **사전 확인 필요:** CMD 폰트(주로 Consolas, NanumGothicCoding 등)에서  
> `┌ ─ ┐ │ ├ ┤ └ ┘ ┬ ┴ ┼` 문자가 정상 표시되는지 먼저 확인 후 진행한다.

**적용 대상:** 시료 목록, 주문 목록

**변경 전:**
```
  ID       이름                생산시간    수율    재고
  ────────────────────────────────────────────────────
  S-001    실리콘 웨이퍼-8인치  0.5 min   0.92   480 ea
```

**변경 후:**
```
  ┌─────────┬────────────────────────┬──────────┬──────┬─────────┐
  │  ID     │  이름                  │ 생산시간 │  수율│    재고 │
  ├─────────┼────────────────────────┼──────────┼──────┼─────────┤
  │  S-001  │  실리콘 웨이퍼-8인치   │ 0.5 min  │ 0.92 │  480 ea │
  └─────────┴────────────────────────┴──────────┴──────┴─────────┘
```

**작업 목록:**

| # | 작업 | 확인 방법 |
|---|------|-----------|
| 7-1 | `ConsoleHelper`에 표 테두리 출력 유틸 추가 | 수동 확인 |
| 7-2 | `SampleView` 시료 목록 테두리 적용 | 수동 확인 |
| 7-3 | `ApprovalView` / `ReleaseView` 주문 목록 적용 | 수동 확인 |

**완료 기준:**
- [ ] CMD 폰트에서 테두리 문자 정상 표시 확인 (사전 검토)
- [ ] 시료 목록 테두리 표시 확인
- [ ] 기존 테스트 전체 통과

---

## 4. 구현 순서 체크리스트

- [x] Phase 1 — AnsiColor 유틸
- [x] Phase 2 — 배너 + 화면 클리어
- [x] Phase 3 — 메인 메뉴 + 공통 색상
- [x] Phase 4 — 상태 뱃지 + 메시지 색상 전체
- [x] Phase 5 — 페이지네이션
- [ ] Phase 6 — Progress Bar
- [ ] Phase 7 — 표 테두리 (선택)

---

## 5. 미결 UX 이슈

### 뒤로가기 입력 일관성 문제

현재 화면마다 상위 메뉴로 돌아가는 방법이 통일되어 있지 않다.

| 화면 유형 | 현재 방법 |
|-----------|-----------|
| 조회 전용 화면 (목록, 재고 현황 등) | `Enter` |
| 선택 가능 목록 (승인, 출고 등) | `[0]` 입력 |
| 액션 완료 화면 (승인 완료, 출고 완료 등) | `Enter` |
| 서브메뉴 루프 | `[0]` 입력 |

**목표:** 모든 화면에서 뒤로가기를 `[0]` 또는 `Enter` 중 하나로 통일.  
**우선순위:** Phase 6·7 완료 후 전체 UX 정리 단계에서 결정.
