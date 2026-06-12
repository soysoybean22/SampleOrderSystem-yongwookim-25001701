# Phase 2 설계 — 시료 관리 (SampleController + SampleView + 메인 메뉴)

## 1. 목표

이 Phase가 끝나면 프로그램을 실행했을 때 **메인 메뉴가 뜨고**,  
`[1] 시료 관리`로 진입해 시료 등록·목록 조회·검색을 실제로 사용할 수 있다.

고객님 확인 포인트:
- `./gradlew run` 실행 → 메인 메뉴 출력
- `[1]` 입력 → 시료 관리 서브메뉴 진입
- 시료 등록 → 재시작 후에도 등록한 시료가 남아있는지 (영속성)
- 중복 ID 등록 시 오류 메시지 출력

---

## 2. 구현 대상 목록

| 분류 | 클래스 | 역할 |
|------|--------|------|
| Controller | `SampleController` | 시료 등록·조회·검색 비즈니스 로직 |
| View | `ConsoleHelper` | Scanner 공통 입력 유틸 |
| View | `MainView` | 메인 메뉴 출력 및 루프 |
| View | `SampleView` | 시료 관리 서브메뉴 출력 및 입력 처리 |
| Entry | `Main.java` 수정 | 메인 메뉴 루프 진입점으로 변경 |

---

## 3. 클래스 설계

### 3-1. SampleController

Repository를 생성자로 주입받아 비즈니스 로직만 담당한다.

```java
package org.example.controller;

public final class SampleController {
    private final SampleRepository repository;

    public SampleController(SampleRepository repository) {
        this.repository = repository;
    }

    // 시료 등록 — 중복 ID 시 IllegalArgumentException (Repository에서 발생)
    public void register(String sampleId, String name,
                         double avgProductionTime, double yield, int stock) {
        repository.save(new Sample(sampleId, name, avgProductionTime, yield, stock));
    }

    // 전체 목록 조회
    public List<Sample> findAll() {
        return repository.findAll();
    }

    // 이름 포함 검색
    public List<Sample> searchByName(String keyword) {
        return repository.findByNameContaining(keyword);
    }
}
```

**설계 원칙:**
- Controller는 View를 모른다 — 출력은 View가 담당
- Controller는 Repository만 의존한다
- 예외를 직접 잡지 않는다 — View에서 사용자 메시지로 변환

---

### 3-2. ConsoleHelper

`Scanner`를 앱 전체에서 하나만 유지하는 공통 입력 유틸.

```java
package org.example.view;

public final class ConsoleHelper {
    private static final Scanner SCANNER = new Scanner(System.in, StandardCharsets.UTF_8);

    private ConsoleHelper() {}

    // 프롬프트 출력 후 한 줄 입력 받기
    public static String readLine(String prompt) { ... }

    // 정수 입력 받기 — 숫자가 아니면 재입력 요청
    public static int readInt(String prompt) { ... }

    // 실수 입력 받기 — 숫자가 아니면 재입력 요청
    public static double readDouble(String prompt) { ... }

    // 구분선 출력
    public static void printSeparator() { ... }

    // 제목 박스 출력
    public static void printHeader(String title) { ... }
}
```

---

### 3-3. MainView

메인 메뉴 루프. 시스템 현황 요약 + 메뉴 선택.

```java
package org.example.view;

public final class MainView {
    private final SampleController sampleController;
    // Phase 3 이후 다른 Controller 추가 예정

    public MainView(SampleController sampleController) {
        this.sampleController = sampleController;
    }

    // 메인 루프 — 0 입력 시 종료
    public void run() { ... }

    // 시스템 현황 요약 출력
    // 등록 시료 N종 | 총 재고 N ea | 전체 주문 N건 | 생산라인 N건 대기
    private void printSummary() { ... }

    // 메뉴 출력
    private void printMenu() { ... }
}
```

**메인 메뉴 화면 예시:**
```
================================================================
        반도체 시료 생산주문관리 시스템
================================================================
시스템 현황  2026-04-16 09:32:15

등록 시료   5종    총 재고   1,640 ea
전체 주문  10건    생산라인   2건 대기

[1] 시료 관리          [2] 시료 주문
[3] 주문 승인/거절     [4] 모니터링
[5] 생산라인 조회      [6] 출고 처리
[0] 종료

선택 > _
```

Phase 2에서는 `[1]`만 동작하고 나머지는 "준비 중" 메시지를 출력한다.

---

### 3-4. SampleView

시료 관리 서브메뉴. 입력·출력·오류 메시지를 담당한다.

```java
package org.example.view;

public final class SampleView {
    private final SampleController controller;

    public SampleView(SampleController controller) {
        this.controller = controller;
    }

    // 시료 관리 서브메뉴 루프 — 0 입력 시 메인으로 복귀
    public void run() { ... }

    // [1] 시료 등록 화면
    private void registerSample() { ... }

    // [2] 시료 목록 화면
    private void listSamples() { ... }

    // [3] 시료 검색 화면
    private void searchSample() { ... }
}
```

**시료 등록 화면 예시:**
```
[1] 시료 관리 > 시료 등록
----------------------------------------------------------------
시료 ID      > S-006
시료 이름    > InP 기판-2인치
평균 생산시간 (min/ea) > 1.2
수율 (0~1)   > 0.85
초기 재고    > 50

등록 완료: InP 기판-2인치 (S-006)
```

**시료 목록 화면 예시:**
```
[1] 시료 관리 > 시료 목록  (총 6종)
----------------------------------------------------------------
ID       이름                    생산시간    수율    재고
S-001    실리콘 웨이퍼-8인치     0.5 min    0.92    480 ea
S-002    GaN 에피택셜-4인치      0.3 min    0.78    220 ea
...
```

**시료 검색 화면 예시:**
```
[1] 시료 관리 > 시료 검색
----------------------------------------------------------------
검색어 > 웨이퍼

검색 결과 (2건)
ID       이름                    생산시간    수율    재고
S-001    실리콘 웨이퍼-8인치     0.5 min    0.92    480 ea
S-005    산화막 웨이퍼-SiO2      0.6 min    0.88      0 ea
```

---

### 3-5. Main.java 수정

`dummy` / `monitor` 도구 실행 외에 **인자 없이 실행하면 메인 메뉴로 진입**한다.

```java
public static void main(String[] args) {
    if (args.length > 0) {
        switch (args[0]) {
            case "monitor" -> DataMonitorTool.run();
            case "dummy"   -> DummyDataGenerator.run();
            default        -> System.out.println("알 수 없는 명령: " + args[0]);
        }
        return;
    }
    // 메인 메뉴 진입
    SampleController sampleController = new SampleController(new SampleRepository());
    new MainView(sampleController).run();
}
```

---

## 4. 레이어 의존 방향

```
Main
 └── MainView
       └── SampleView ──── SampleController ──── SampleRepository
       └── ConsoleHelper
```

View는 Controller만 안다. Controller는 Repository만 안다.  
View끼리는 의존하지 않는다.

---

## 5. TDD 테스트 계획

> View는 콘솔 I/O를 다루므로 자동 테스트 대상이 아니다.  
> Controller만 자동 테스트하고, View는 수동으로 확인한다.

### SampleController

테스트 격리: `@BeforeEach`에서 `JsonFileStorage.DATA_DIR = "test-data"` 설정

| 테스트 메서드 | 검증 내용 |
|--------------|----------|
| `시료를_정상_등록한다` | `register()` 호출 후 `findAll()`에 포함 확인 |
| `중복_ID_등록_시_예외` | 동일 ID 두 번 등록 시 `IllegalArgumentException` |
| `전체_시료를_조회한다` | 등록 2건 후 `findAll()` 크기 2 확인 |
| `이름으로_시료를_검색한다` | 키워드 포함 시료만 반환 확인 |
| `검색_결과_없으면_빈_리스트` | 없는 키워드 검색 시 빈 리스트 반환 |

---

## 6. 수동 테스트 시나리오

Phase 2 완료 후 고객님이 확인하실 내용:

| 순서 | 행동 | 기대 결과 |
|------|------|-----------|
| 1 | `./gradlew run` 실행 | 메인 메뉴 출력 |
| 2 | `1` 입력 | 시료 관리 서브메뉴 진입 |
| 3 | `2` 입력 | 기존 시료 5종 목록 출력 |
| 4 | `1` 입력 → 새 시료 정보 입력 | "등록 완료" 메시지 출력 |
| 5 | `2` 입력 | 방금 등록한 시료 포함 6종 출력 |
| 6 | `3` 입력 → "웨이퍼" 검색 | 웨이퍼 포함 시료만 출력 |
| 7 | `0` 입력 | 메인 메뉴로 복귀 |
| 8 | `0` 입력 | 프로그램 종료 |
| 9 | 재실행 후 목록 조회 | 등록한 시료 유지 확인 (영속성) |
| 10 | 중복 ID 등록 시도 | 오류 메시지 출력 후 서브메뉴 유지 |

---

## 7. 완료 기준

- [ ] `SampleControllerTest` 전체 통과 (`./gradlew test`)
- [ ] `./gradlew run` 실행 시 메인 메뉴 출력
- [ ] 시료 등록·목록·검색 동작 확인 (수동)
- [ ] 재시작 후 데이터 유지 확인 (수동)
- [ ] 잘못된 입력(문자, 범위 초과) 시 재입력 요청 확인 (수동)
- [ ] 컴파일 경고 없음
