# Phase 2 설계 — S-Semi 배너 + 화면 클리어

## 1. 목표

프로그램 시작 시 S-Semi ASCII 배너를 1회 출력하고,
메뉴 전환마다 이전 출력물이 화면에 쌓이지 않도록 화면 클리어를 적용한다.

**이 Phase에서 메뉴·메시지 색상 변경은 없다.**  
배너 출력(`AnsiColor.CYAN + BOLD`)과 화면 클리어만 추가한다.

---

## 2. 구현 대상

| 클래스 | 패키지 | 신규/수정 |
|--------|--------|----------|
| `ConsoleHelper` | `org.example.view` | **수정** — `clearScreen()`, `printBanner()` 추가 |
| `Main` | `org.example` | **수정** — `printBanner()` 호출 추가 |
| `MainView` | `org.example.view` | **수정** — 루프 매 반복 `clearScreen()` 추가 |
| `SampleView` | `org.example.view` | **수정** — `run()` 루프 매 반복 `clearScreen()` 추가 |
| `OrderView` | `org.example.view` | **수정** — `run()` 루프 매 반복 `clearScreen()` 추가 |
| `ApprovalView` | `org.example.view` | **수정** — `run()` 루프 매 반복 `clearScreen()` 추가 |
| `ProductionView` | `org.example.view` | **수정** — `run()` 루프 매 반복 `clearScreen()` 추가 |
| `MonitoringView` | `org.example.view` | **수정** — `run()` 루프 매 반복 `clearScreen()` 추가 |
| `ReleaseView` | `org.example.view` | **수정** — `run()` 루프 매 반복 `clearScreen()` 추가 |

---

## 3. 상세 설계

### 3-1. ConsoleHelper — clearScreen() / printBanner() 추가

**clearScreen():**

ANSI 이스케이프 코드 두 개를 연속으로 전송해 터미널 화면을 지운다.

| 코드 | 동작 |
|------|------|
| `\033[H` | 커서를 좌상단(행 1, 열 1)으로 이동 |
| `\033[2J` | 화면 전체 내용 지우기 |

`System.out.flush()`로 버퍼를 즉시 반영한다.

```java
public static void clearScreen() {
    System.out.print("\033[H\033[2J");
    System.out.flush();
}
```

**printBanner():**

프로그램 진입 시 1회 출력하는 S-Semi ASCII 배너다.
Phase 1에서 구현한 `AnsiColor.color()`를 사용해 배너 텍스트에 CYAN BOLD를 적용한다.
`ConsoleHelper`와 `AnsiColor`는 같은 패키지(`org.example.view`)이므로 import 불필요.

```java
public static void printBanner() {
    String art = """
          ____       ____                 _
         / ___|     / ___|  ___ _ __ ___ (_)
         \\___ \\ ____\\___ \\ / _ \\ '_ ` _ \\| |
          ___) |_____|__) |  __/ | | | | | |
         |____/     |____/ \\___|_| |_| |_|_|
        """;
    System.out.println(AnsiColor.color(art, AnsiColor.CYAN + AnsiColor.BOLD));
    System.out.println("        반도체 시료 생산주문관리 시스템");
    printSeparator();
}
```

> **텍스트 블록(`"""`)** — Java 17 프로젝트이므로 사용 가능하다.  
> 역슬래시(`\`)는 텍스트 블록 내에서도 이스케이프가 필요하므로 `\\_`와 같이 작성한다.

---

### 3-2. Main.java — printBanner() 1회 호출

정상 실행 경로(인자 없음)에서만 배너를 출력한다.  
`monitor` / `dummy` 인자 실행 경로는 배너가 불필요하므로 적용하지 않는다.

**변경 위치:** `new MainView(...).run()` 직전

```java
// 변경 전
new MainView(sampleController, orderController, productionController,
    monitoringController, releaseController).run();

// 변경 후
ConsoleHelper.printBanner();
new MainView(sampleController, orderController, productionController,
    monitoringController, releaseController).run();
```

---

### 3-3. MainView.run() — 루프 매 반복 clearScreen 추가

서브메뉴에서 메인으로 돌아올 때마다 화면을 지우고 시스템 현황을 새로 출력한다.

```java
// 변경 전
public void run() {
    while (true) {
        printSummary();
        printMenu();
        String input = ConsoleHelper.readLine("선택 > ");
        ...
    }
}

// 변경 후
public void run() {
    while (true) {
        ConsoleHelper.clearScreen();  // ← 추가
        printSummary();
        printMenu();
        String input = ConsoleHelper.readLine("선택 > ");
        ...
    }
}
```

---

### 3-4. 서브메뉴 View 6개 — run() 루프 매 반복 clearScreen 추가

6개 서브메뉴 모두 `run()` 루프의 **매 반복 첫 줄**에 clearScreen을 추가한다.  
현재 각 서브뷰의 `run()`은 루프 시작 시 헤더와 메뉴를 출력하는 구조이므로,
clearScreen을 첫 줄에 넣으면 서브메뉴 헤더가 항상 화면 최상단에 위치한다.

**SampleView 예시:**

```java
// 변경 전
public void run() {
    while (true) {
        printMenu();
        String input = ConsoleHelper.readLine("선택 > ");
        ...
    }
}

// 변경 후
public void run() {
    while (true) {
        ConsoleHelper.clearScreen();  // ← 추가
        printMenu();
        String input = ConsoleHelper.readLine("선택 > ");
        ...
    }
}
```

**MonitoringView 예시 (printMenu() 없이 인라인 출력 구조):**

```java
// 변경 전
public void run() {
    while (true) {
        ConsoleHelper.println("");
        ConsoleHelper.printHeader("[4] 모니터링  " + LocalDateTime.now().format(FMT));
        ...
    }
}

// 변경 후
public void run() {
    while (true) {
        ConsoleHelper.clearScreen();  // ← 추가
        ConsoleHelper.println("");
        ConsoleHelper.printHeader("[4] 모니터링  " + LocalDateTime.now().format(FMT));
        ...
    }
}
```

**적용 대상 View 목록:**

| View | 현재 루프 첫 출력 | clearScreen 삽입 위치 |
|------|------------------|-----------------------|
| `SampleView` | `printMenu()` | `printMenu()` 직전 |
| `OrderView` | `printMenu()` 계열 | `printMenu()` 직전 |
| `ApprovalView` | `printMenu()` 계열 | `printMenu()` 직전 |
| `ProductionView` | `printMenu()` 계열 | `printMenu()` 직전 |
| `MonitoringView` | `println("")` + `printHeader()` | `println("")` 직전 |
| `ReleaseView` | `printMenu()` 계열 | `printMenu()` 직전 |

---

## 4. 화면 흐름

```
[프로그램 시작]
  └─ Main.main()
       ├─ ConsoleHelper.printBanner()          ← 1회만 출력
       └─ MainView.run()
            └─ [루프]
                 ├─ ConsoleHelper.clearScreen()
                 ├─ printSummary()
                 ├─ printMenu()
                 ├─ 입력 "1" ──► SampleView.run()
                 │                └─ [루프]
                 │                     ├─ ConsoleHelper.clearScreen()
                 │                     ├─ printMenu()
                 │                     └─ 입력 "0" ──► return
                 │                                      (MainView 루프로 복귀 → clearScreen)
                 └─ 입력 "0" ──► return (종료)
```

**배너는 재출력되지 않는다.** 메인 메뉴로 돌아올 때는 clearScreen 후 `printSummary()`만 출력한다.

---

## 5. TDD 테스트 계획

`clearScreen()`과 `printBanner()`는 `System.out`에 직접 출력하는 메서드로
반환값이 없어 자동 테스트 대상이 아니다.

기존 56개 테스트가 모두 통과하면 완료로 간주한다.

수동 확인 항목:

| 확인 항목 | 기대 동작 |
|-----------|-----------|
| 프로그램 시작 시 | S-Semi 배너가 CYAN BOLD로 1회 출력됨 |
| 메인 메뉴 루프 진입 시 | 이전 출력이 지워지고 시스템 현황이 최상단에 표시됨 |
| 서브메뉴 진입 시 | 메인 메뉴가 지워지고 서브메뉴 헤더가 최상단에 표시됨 |
| 서브메뉴 → 메인 복귀 시 | 서브메뉴가 지워지고 메인 메뉴가 최상단에 표시됨 |
| `./gradlew run --args="monitor"` | 배너 미출력 |
| `./gradlew run --args="dummy"` | 배너 미출력 |

---

## 6. 작업 목록

| # | 작업 | 변경 파일 | 확인 방법 |
|---|------|-----------|-----------|
| 2-1 | `ConsoleHelper.clearScreen()` 구현 | `ConsoleHelper.java` | 컴파일 확인 |
| 2-2 | `ConsoleHelper.printBanner()` 구현 | `ConsoleHelper.java` | 수동 확인 |
| 2-3 | `Main.java` — `printBanner()` 호출 추가 | `Main.java` | 수동 확인 |
| 2-4 | `MainView.run()` — `clearScreen()` 추가 | `MainView.java` | 수동 확인 |
| 2-5 | `SampleView.run()` — `clearScreen()` 추가 | `SampleView.java` | 수동 확인 |
| 2-6 | `OrderView.run()` — `clearScreen()` 추가 | `OrderView.java` | 수동 확인 |
| 2-7 | `ApprovalView.run()` — `clearScreen()` 추가 | `ApprovalView.java` | 수동 확인 |
| 2-8 | `ProductionView.run()` — `clearScreen()` 추가 | `ProductionView.java` | 수동 확인 |
| 2-9 | `MonitoringView.run()` — `clearScreen()` 추가 | `MonitoringView.java` | 수동 확인 |
| 2-10 | `ReleaseView.run()` — `clearScreen()` 추가 | `ReleaseView.java` | 수동 확인 |

---

## 7. 완료 기준

- [ ] 프로그램 시작 시 S-Semi 배너 1회 출력 확인
- [ ] 메인 메뉴 복귀 시 화면 클리어 확인
- [ ] 각 서브메뉴 진입 시 화면 클리어 확인
- [ ] `monitor`/`dummy` 인자 실행 시 배너 미출력 확인
- [ ] 기존 테스트 56개 전체 통과
- [ ] 컴파일 경고 없음

---

## 8. 논의 포인트

구현 전 확인이 필요한 사항입니다.

### 8-1. 서브메뉴 액션 완료 후 clearScreen 여부

현재 설계는 `run()` **루프 시작 시**에만 clearScreen을 적용합니다.

예: 시료 등록 완료 메시지 출력 → 사용자가 다음 입력 → 루프 재진입 시 clearScreen

동작 예시:
```
[시료 등록 완료 메시지가 화면에 남음]
선택 > _          ← 사용자 입력 대기
                   ↓ 입력 후 루프 재진입
[clearScreen → 서브메뉴 헤더가 최상단으로]
```

대안으로 각 액션 완료 후에도 clearScreen을 추가하면 완료 메시지가 즉시 지워집니다.
완료 메시지를 사용자가 확인할 시간이 없을 수 있으므로 현재 설계를 기본으로 하고,
필요 시 Phase 4(메시지 색상 적용) 단계에서 조정하는 방향이 적절하다고 판단합니다.

### 8-2. 배너 하단 부제 색상

현재 설계에서 배너 아래 부제("반도체 시료 생산주문관리 시스템")는 색상 없이 출력됩니다.
PRD-고도화.md 2-2항에는 부제 텍스트에 WHITE를 적용하도록 되어 있는데,
흰색 기본 터미널에서는 효과가 없으므로 생략해도 무방합니다.
적용 여부를 결정해주시면 반영하겠습니다.
