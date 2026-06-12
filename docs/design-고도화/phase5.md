# Phase 5 설계 — 페이지네이션 (5행 제한)

## 1. 목표

목록이 5건을 초과하면 페이지 단위로 나눠 표시한다.  
페이지 이동은 `[P]`·`[N]`으로 처리하고, 번호 선택은 현재 페이지 기준 1~5로 유지한다.

---

## 2. 구현 대상

| 클래스 | 패키지 | 신규/수정 |
|--------|--------|----------|
| `Paginator<T>` | `org.example.view` | **신규** |
| `SampleView` | `org.example.view` | **수정** — `listSamples()`, `searchSample()` |
| `ApprovalView` | `org.example.view` | **수정** — `run()`, `printReservedList()` |
| `ReleaseView` | `org.example.view` | **수정** — `run()`, `printConfirmedList()` |

---

## 3. Paginator\<T\> 클래스 설계

페이지네이션 로직을 담당하는 순수 유틸 클래스다.  
뷰에서 `List<T>`를 넘기면 현재 페이지 슬라이스를 반환한다.

```java
package org.example.view;

import java.util.List;

public final class Paginator<T> {

    public static final int PAGE_SIZE = 5;

    private final List<T> items;
    private int page = 0; // 0-based 내부 관리

    public Paginator(List<T> items) {
        this.items = List.copyOf(items);
    }

    public List<T> currentItems() {
        int from = page * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, items.size());
        return items.subList(from, to);
    }

    public boolean hasNext()  { return (page + 1) * PAGE_SIZE < items.size(); }
    public boolean hasPrev()  { return page > 0; }
    public void nextPage()    { if (hasNext()) page++; }
    public void prevPage()    { if (hasPrev()) page--; }
    public int totalPages()   { return (int) Math.ceil((double) items.size() / PAGE_SIZE); }
    public int currentPage()  { return page + 1; } // 1-based 표시용

    public boolean needsPagination() { return items.size() > PAGE_SIZE; }

    public String pageInfo() {
        return AnsiColor.color(
            String.format("페이지 %d / %d", currentPage(), totalPages()),
            AnsiColor.CYAN);
    }
}
```

---

## 4. 페이지네이션 UI 규칙

### 5건 이하 — 페이지 UI 미표시

기존과 동일하게 전체 목록 출력 후 Enter 대기.

### 6건 이상 — 페이지 분할 + 네비게이션

```
  [1]  ORD-20260416-0001    LG이노텍      실리콘 웨이퍼   300 ea
  [2]  ORD-20260416-0002    SK하이닉스    GaN 에피택셜    150 ea
  [3]  ORD-20260416-0003    삼성전자      SiC 파워기판    200 ea
  [4]  ORD-20260416-0004    DB하이텍      포토레지스트    400 ea
  [5]  ORD-20260416-0005    인텔코리아    실리콘 웨이퍼   100 ea

  ◀ 이전 [P]    페이지 1 / 2    다음 [N] ▶
  번호 선택 또는 [P/N] 페이지 이동, [0] 위로 >
```

- `◀ 이전 [P]`는 이전 페이지가 있을 때만 표시
- `다음 [N] ▶`는 다음 페이지가 있을 때만 표시
- `페이지 X / Y`는 항상 CYAN
- 페이지 번호는 현재 페이지 기준 1~5 (2페이지의 [1] = 전체 6번째 항목)

---

## 5. 뷰별 상세 변경

### 5-1. SampleView — listSamples() / searchSample()

두 메서드 모두 **보기 전용** (선택 없음). 페이지 이동 후 0으로 메뉴 복귀.

#### listSamples() 변경 후 구조

```java
private void listSamples() {
    List<Sample> samples = controller.findAll();
    ConsoleHelper.println("");
    ConsoleHelper.println("[1] 시료 관리 > 시료 목록  (총 " + samples.size() + "종)");
    ConsoleHelper.printThinLine();

    if (samples.isEmpty()) {
        ConsoleHelper.println("  등록된 시료가 없습니다.");
        ConsoleHelper.readLine("\n  [Enter] 메뉴로 돌아가기");
        return;
    }

    Paginator<Sample> paginator = new Paginator<>(samples);

    while (true) {
        printSampleTable(paginator.currentItems());

        if (!paginator.needsPagination()) {
            ConsoleHelper.readLine("\n  [Enter] 메뉴로 돌아가기");
            return;
        }

        printPageNav(paginator);
        String input = ConsoleHelper.readLine("  [P/N] 페이지 이동, [0] 위로 > ");
        if (input.equals("0"))                return;
        if (input.equalsIgnoreCase("N"))      paginator.nextPage();
        else if (input.equalsIgnoreCase("P")) paginator.prevPage();
    }
}
```

`printSampleTable(List<Sample>)`는 기존 `printf` 출력 로직을 추출한 내부 메서드.

#### searchSample() 변경 후 구조

동일 패턴 적용. 검색 결과를 `Paginator<Sample>`로 감싸고 같은 루프 사용.

---

### 5-2. ApprovalView — run() / printReservedList()

**아이템 선택 가능** (번호 입력 → 승인/거절 처리).  
처리 후에는 즉시 반환 (목록이 변경되므로 재진입 불필요).

#### run() 변경 후 구조

```java
public void run() {
    ConsoleHelper.clearScreen();
    ConsoleHelper.println("");
    ConsoleHelper.printHeader("[3] 주문 승인/거절");

    List<Order> reserved = orderController.findReservedOrders();
    if (reserved.isEmpty()) {
        ConsoleHelper.println("  승인 대기 중인 주문이 없습니다.");
        return;
    }

    Paginator<Order> paginator = new Paginator<>(reserved);

    while (true) {
        printReservedList(paginator.currentItems(), paginator);

        String prompt = paginator.needsPagination()
            ? "번호 선택 또는 [P/N] 페이지 이동, [0] 위로 > "
            : "승인할 번호 > ";
        String input = ConsoleHelper.readLine(prompt);

        if (input.equals("0")) return;
        if (input.equalsIgnoreCase("N")) { paginator.nextPage(); continue; }
        if (input.equalsIgnoreCase("P")) { paginator.prevPage(); continue; }

        int index;
        try { index = Integer.parseInt(input) - 1; }
        catch (NumberFormatException e) {
            ConsoleHelper.println("  잘못된 입력입니다.");
            continue;
        }

        List<Order> current = paginator.currentItems();
        if (index < 0 || index >= current.size()) {
            ConsoleHelper.println("  목록에 없는 번호입니다.");
            continue;
        }

        processApproval(current.get(index));
        return;
    }
}
```

#### printReservedList() 시그니처 변경

```java
// 변경 전
private void printReservedList(List<Order> orders)

// 변경 후
private void printReservedList(List<Order> orders, Paginator<Order> paginator)
```

페이지네이션이 필요한 경우 목록 하단에 `printPageNav(paginator)` 출력.

---

### 5-3. ReleaseView — run() / printConfirmedList()

`ApprovalView`와 동일 패턴.

#### run() 변경 후 구조

```java
public void run() {
    ConsoleHelper.clearScreen();
    ConsoleHelper.println("");
    ConsoleHelper.printHeader("[6] 출고 처리");

    List<Order> confirmed = releaseController.findConfirmedOrders();
    if (confirmed.isEmpty()) {
        ConsoleHelper.println("  출고 가능한 주문이 없습니다.");
        return;
    }

    Paginator<Order> paginator = new Paginator<>(confirmed);

    while (true) {
        printConfirmedList(paginator.currentItems(), paginator);

        String prompt = paginator.needsPagination()
            ? "번호 선택 또는 [P/N] 페이지 이동, [0] 위로 > "
            : "출고할 번호 > ";
        String input = ConsoleHelper.readLine(prompt);

        if (input.equals("0")) return;
        if (input.equalsIgnoreCase("N")) { paginator.nextPage(); continue; }
        if (input.equalsIgnoreCase("P")) { paginator.prevPage(); continue; }

        int index;
        try { index = Integer.parseInt(input) - 1; }
        catch (NumberFormatException e) {
            ConsoleHelper.println("  잘못된 입력입니다.");
            continue;
        }

        List<Order> current = paginator.currentItems();
        if (index < 0 || index >= current.size()) {
            ConsoleHelper.println("  목록에 없는 번호입니다.");
            continue;
        }

        processRelease(current.get(index));
        return;
    }
}
```

#### printConfirmedList() 시그니처 변경

```java
// 변경 전
private void printConfirmedList(List<Order> orders)

// 변경 후
private void printConfirmedList(List<Order> orders, Paginator<Order> paginator)
```

---

### 5-4. 공통 — printPageNav() 헬퍼

세 View 모두에서 페이지 네비게이션 바를 출력하는 공통 패턴을 각 View 내부 메서드로 추출한다.

```java
private <T> void printPageNav(Paginator<T> paginator) {
    ConsoleHelper.println("");
    String prev = paginator.hasPrev() ? AnsiColor.color("◀ 이전 [P]", AnsiColor.CYAN) : "          ";
    String next = paginator.hasNext() ? AnsiColor.color("다음 [N] ▶", AnsiColor.CYAN) : "          ";
    ConsoleHelper.println("  " + prev + "    " + paginator.pageInfo() + "    " + next);
}
```

---

## 6. TDD 테스트 계획

`Paginator<T>`는 순수 로직 클래스로 자동 테스트 대상이다.

### PaginatorTest

```java
class PaginatorTest {

    private List<Integer> tenItems() {
        return List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    @DisplayName("currentItems()는 첫 페이지의 5건을 반환한다")
    void currentItems_첫페이지_5건() { ... }

    @Test
    @DisplayName("nextPage() 후 currentItems()는 다음 5건을 반환한다")
    void nextPage_후_다음항목() { ... }

    @Test
    @DisplayName("hasNext()는 다음 페이지가 있으면 true를 반환한다")
    void hasNext_다음페이지_있으면_true() { ... }

    @Test
    @DisplayName("hasPrev()는 첫 페이지에서 false를 반환한다")
    void hasPrev_첫페이지_false() { ... }

    @Test
    @DisplayName("totalPages()는 올바른 총 페이지 수를 반환한다")
    void totalPages_올바른_계산() { ... }

    @Test
    @DisplayName("needsPagination()은 5건 이하에서 false를 반환한다")
    void needsPagination_5건이하_false() { ... }

    @Test
    @DisplayName("needsPagination()은 6건 이상에서 true를 반환한다")
    void needsPagination_6건이상_true() { ... }

    @Test
    @DisplayName("마지막 페이지의 항목 수는 나머지 건수다")
    void 마지막페이지_나머지건수() { ... }

    @Test
    @DisplayName("hasNext()는 마지막 페이지에서 false를 반환한다")
    void hasNext_마지막페이지_false() { ... }

    @Test
    @DisplayName("nextPage()는 마지막 페이지에서 페이지를 변경하지 않는다")
    void nextPage_마지막페이지_변경없음() { ... }
}
```

View 레이어 변경(페이지 UI, 입력 처리)은 수동 확인.

---

## 7. 작업 목록

| # | 작업 | 파일 | TDD |
|---|------|------|-----|
| 5-1 | `PaginatorTest` 작성 | `PaginatorTest.java` | RED |
| 5-2 | `Paginator<T>` 구현 | `Paginator.java` | GREEN |
| 5-3 | `SampleView.listSamples()` 페이지네이션 적용 | `SampleView.java` | 수동 |
| 5-4 | `SampleView.searchSample()` 페이지네이션 적용 | `SampleView.java` | 수동 |
| 5-5 | `ApprovalView.run()` 페이지 루프 + 선택 처리 | `ApprovalView.java` | 수동 |
| 5-6 | `ApprovalView.printReservedList()` 시그니처 변경 | `ApprovalView.java` | 수동 |
| 5-7 | `ReleaseView.run()` 페이지 루프 + 선택 처리 | `ReleaseView.java` | 수동 |
| 5-8 | `ReleaseView.printConfirmedList()` 시그니처 변경 | `ReleaseView.java` | 수동 |

---

## 8. 완료 기준

- [ ] `PaginatorTest` 10개 통과
- [ ] 5건 이하: 페이지 UI 미표시, 기존과 동일하게 동작
- [ ] 6건 이상: 5건씩 분할, `[P/N]` 이동 정상 동작
- [ ] 페이지 번호 `페이지 X / Y` CYAN 표시
- [ ] 마지막 페이지에서 `[N]` 입력 시 이동 없음
- [ ] 첫 페이지에서 `[P]` 입력 시 이동 없음
- [ ] 기존 테스트 68개 전체 통과
