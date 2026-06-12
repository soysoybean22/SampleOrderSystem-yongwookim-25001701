# PRD-고도화 — 반도체 시료 생산주문관리 시스템 UI/UX 개선

## 1. 개요

기능 구현이 완료된 시스템의 **콘솔 UI/UX를 개선**한다.  
핵심 목표는 담당자가 많은 정보를 빠르게 파악하고, 오입력을 줄이고, 시각적 피로를 낮추는 것이다.

---

## 2. 구현 항목

### 2-1. ANSI 색상 유틸리티 (`AnsiColor`)

모든 색상·스타일 처리를 담당하는 유틸 클래스를 신규 추가한다.  
Windows 10 이상은 ANSI 이스케이프 코드를 지원하므로 별도 라이브러리 없이 구현한다.

**지원할 색상/스타일:**

| 용도 | 색상 | 적용 대상 |
|------|------|-----------|
| 성공 | 초록 (Green) | 등록 완료, 승인 완료, 출고 완료, 재고 여유, CONFIRMED, RELEASE |
| 오류/위험 | 빨강 (Red) | 오류 메시지, 거절, 재고 고갈, REJECTED |
| 주의/대기 | 노랑 (Yellow) | 안내사항(※), RESERVED, 재고 부족 |
| 진행 중 | 마젠타 (Magenta) | PRODUCING, 생산 중 |
| 강조/제목 | 청록 (Cyan) | 메뉴 번호 [1]~[6], 헤더, 주문번호 |
| 일반 강조 | 흰색 굵게 (Bold) | 수량, 주요 숫자값 |
| 배경 강조 | 파랑 (Blue) | 섹션 구분선 |

```java
package org.example.view;

public final class AnsiColor {
    // 색상
    public static final String RED     = "[31m";
    public static final String GREEN   = "[32m";
    public static final String YELLOW  = "[33m";
    public static final String BLUE    = "[34m";
    public static final String MAGENTA = "[35m";
    public static final String CYAN    = "[36m";
    public static final String WHITE   = "[37m";

    // 스타일
    public static final String BOLD    = "[1m";
    public static final String RESET   = "[0m";

    // 조합 상수
    public static final String SUCCESS = GREEN + BOLD;  // 성공
    public static final String ERROR   = RED + BOLD;    // 오류
    public static final String WARN    = "[33m";  // 짙은 노랑 (안내사항)
    public static final String MENU_NUM = CYAN + BOLD;  // 메뉴 번호
    public static final String HEADER  = BLUE + BOLD;   // 헤더

    private AnsiColor() {}

    public static String color(String text, String ansiCode) {
        return ansiCode + text + RESET;
    }

    // 주문 상태별 색상 적용
    public static String statusBadge(String status) { ... }
}
```

---

### 2-2. S-Semi ASCII 배너

프로그램 시작 시 한 번만 출력한다. 메인 메뉴가 반복 출력될 때는 표시하지 않는다.

```
  ____       ____                 _
 / ___|     / ___|  ___ _ __ ___ (_)
 \___ \ ____\___ \ / _ \ '_ ` _ \| |
  ___) |_____|__) |  __/ | | | | | |
 |____/     |____/ \___|_| |_| |_|_|

        반도체 시료 생산주문관리 시스템
```

- `S-Semi` 텍스트: **CYAN + BOLD**
- 부제 텍스트: WHITE

---

### 2-3. 화면 클리어

서브메뉴 진입 시 이전 화면이 아래로 쌓이지 않도록 화면을 지운다.

```java
// ConsoleHelper에 추가
public static void clearScreen() {
    System.out.print("\033[H\033[2J");
    System.out.flush();
}
```

**적용 위치:**
- 각 서브메뉴 `run()` 진입 시 첫 줄에 호출
- 메인 메뉴 루프의 각 반복 시작 시 호출

---

### 2-4. 메인 메뉴 색상 적용

```
================================================================
        S-Semi  반도체 시료 생산주문관리 시스템          ← CYAN BOLD
================================================================
시스템 현황  2026-04-16 09:32:15

등록 시료   5종    총 재고   1,640 ea                    ← 숫자: GREEN BOLD
전체 주문  10건    생산라인   2건 대기    승인 대기  3건  ← "2건 대기": MAGENTA, "3건": YELLOW

  [1] 시료 관리          [2] 시료 주문                   ← [숫자]: CYAN BOLD
  [3] 주문 승인/거절     [4] 모니터링
  [5] 생산라인 조회      [6] 출고 처리
  [0] 종료
```

---

### 2-5. 주문 상태 뱃지 색상

모든 화면에서 상태값 출력 시 일관된 색상을 적용한다.

| 상태 | 색상 | 예시 |
|------|------|------|
| `RESERVED` | YELLOW | `[RESERVED]` |
| `CONFIRMED` | GREEN | `[CONFIRMED]` |
| `PRODUCING` | MAGENTA | `[PRODUCING]` |
| `RELEASE` | CYAN | `[RELEASE]` |
| `REJECTED` | RED | `[REJECTED]` |

---

### 2-6. 성공/실패 메시지 색상

| 메시지 유형 | 색상 | 예시 |
|------------|------|------|
| 완료 메시지 | GREEN BOLD | `✓ 예약 접수 완료.` |
| 오류 메시지 | RED BOLD | `✗ [오류] 등록되지 않은 시료 ID` |
| 취소 메시지 | YELLOW | `취소되었습니다.` |

---

### 2-7. 안내사항 색상

`※` 로 시작하는 안내 문구는 **YELLOW(짙은 노랑)** 으로 표시한다.

```
※ 재고 확인 및 승인은 [3] 주문 승인/거절 메뉴에서 진행하세요.   ← YELLOW
* 부족분 = 주문량 - 재고,  실생산량 = ceil(부족분 / (수율 × 0.9))  ← YELLOW
```

---

### 2-8. 페이지네이션 (목록 5행 제한)

목록이 5행을 초과하는 경우 페이지 단위로 나눠 표시한다.

**적용 대상:** 시료 목록, 주문 목록(승인/거절, 출고처리), RESERVED 목록

**표시 형식:**
```
  ID       이름                    생산시간    수율    재고
  ──────────────────────────────────────────────────────
  S-001    실리콘 웨이퍼-8인치     0.5 min    0.92    480 ea
  S-002    GaN 에피택셜-4인치      0.3 min    0.78    220 ea
  S-003    SiC 파워기판-6인치      0.8 min    0.92     30 ea
  S-004    포토레지스트-PR7        0.2 min    0.95    910 ea
  S-005    산화막 웨이퍼-SiO2      0.6 min    0.88      0 ea

  ◀ 이전 [P]    페이지 1 / 2    다음 [N] ▶     ← CYAN
```

**구현 유틸 (`Paginator`):**

```java
package org.example.view;

public final class Paginator<T> {
    private static final int PAGE_SIZE = 5;

    private final List<T> items;
    private int currentPage; // 0-based

    public Paginator(List<T> items) { ... }

    public List<T> currentItems() { ... }  // 현재 페이지 5개
    public boolean hasNext() { ... }
    public boolean hasPrev() { ... }
    public void nextPage() { ... }
    public void prevPage() { ... }
    public String pageInfo() { ... }       // "페이지 1 / 3"
    public int totalPages() { ... }
}
```

페이지 이동 입력:
- `N` 또는 `n`: 다음 페이지
- `P` 또는 `p`: 이전 페이지
- 숫자: 해당 항목 선택 (현재 페이지 기준 번호)
- `0`: 위로

---

### 2-9. 재고 Progress Bar

모니터링 재고량 확인 화면에서 잔여율을 Progress Bar로 표시한다.

```
  시료명                    재고      미처리주문   상태    잔여율
  ──────────────────────────────────────────────────────────────
  실리콘 웨이퍼-8인치        480 ea    100 ea    여유    [████████░░] 80%
  GaN 에피택셜-4인치         220 ea    500 ea    부족    [████░░░░░░] 44%
  SiC 파워기판-6인치           0 ea    200 ea    고갈    [░░░░░░░░░░]  0%
```

**Progress Bar 색상:**
- 여유 (60% 이상): GREEN
- 부족 (1~59%): YELLOW
- 고갈 (0%): RED

**재고 상태 색상:**
- `여유`: GREEN BOLD
- `부족`: YELLOW BOLD
- `고갈`: RED BOLD

**구현 유틸:**
```java
// ConsoleHelper에 추가
public static String progressBar(int ratio) {
    // ratio: 0~100
    // "████████░░" 형태로 10칸 표시
}
```

---

### 2-10. 표 테두리 개선

현재 `─` 단순 줄 대신 박스 드로잉 문자를 활용한 표 테두리를 적용한다.

```
  ┌──────────┬────────────────────────┬──────────┐
  │  ID      │  이름                  │   재고   │
  ├──────────┼────────────────────────┼──────────┤
  │  S-001   │  실리콘 웨이퍼-8인치   │  480 ea  │
  │  S-002   │  GaN 에피택셜-4인치    │  220 ea  │
  └──────────┴────────────────────────┴──────────┘
```

> **선택적 적용:** 모든 표가 아닌 핵심 목록(시료 목록, 주문 목록)에만 적용.

---

## 3. 구현 순서

| 단계 | 작업 | 비고 |
|------|------|------|
| Step 1 | `AnsiColor` 유틸 구현 | 모든 색상의 기반 |
| Step 2 | S-Semi 배너 + 화면 클리어 | `ConsoleHelper` 확장 |
| Step 3 | 메인 메뉴 색상 적용 | `MainView` 수정 |
| Step 4 | 상태 뱃지 + 성공/실패/안내 색상 | 전체 View 수정 |
| Step 5 | `Paginator` 구현 + 목록 화면 적용 | 시료·주문·생산 View |
| Step 6 | Progress Bar 구현 + 재고 화면 적용 | `MonitoringView` 수정 |
| Step 7 | 표 테두리 개선 (선택) | 핵심 목록에만 적용 |

---

## 4. 비기능 요구사항

- 외부 라이브러리 추가 없이 **순수 ANSI 이스케이프 코드**로 구현
- Windows 10 이상 CMD / PowerShell 환경 지원 (`chcp 65001` 필요)
- 색상이 없는 환경에서도 내용 파악 가능 (색상은 보조 수단)
- 기존 테스트 코드 변경 없이 동작

---

## 5. 완료 기준

- [ ] Step 1 — `AnsiColor` 구현
- [ ] Step 2 — 배너 + 화면 클리어
- [ ] Step 3 — 메인 메뉴 색상
- [ ] Step 4 — 상태 뱃지 + 메시지 색상
- [ ] Step 5 — 페이지네이션
- [ ] Step 6 — Progress Bar
- [ ] Step 7 — 표 테두리 (선택)
- [ ] 기존 테스트 전체 통과
