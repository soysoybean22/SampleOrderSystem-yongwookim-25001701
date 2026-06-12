# Phase 8 설계 — 뒤로가기 입력 통일

## 1. 목표

화면마다 상위 메뉴로 돌아가는 방법이 `[0]`과 `[Enter]`로 혼재하는 문제를 해결한다.  
통일 기준을 정하고 변경이 필요한 4곳만 수정한다.

---

## 2. 통일 원칙

| 화면 유형 | 방법 | 이유 |
|-----------|------|------|
| **조회 전용 화면** (목록·통계 등) | `[0] 위로` | 메뉴에서 선택해 진입한 화면이므로 [0]으로 복귀 |
| **선택 가능 목록** (승인·출고 등) | `[0] 위로` | 이미 [0]을 사용 중 — 유지 |
| **완료·오류·취소 화면** | `[Enter] 메뉴로 돌아가기` | 행동을 확인하는 acknowledgment — Enter가 자연스러움 |
| **빈 목록 안내 화면** | `[Enter] 메뉴로 돌아가기` | 정보성 메시지 — Enter 유지 |
| **서브메뉴 루프** | `[0] 위로` | 이미 [0] 사용 중 — 유지 |

---

## 3. 변경 대상

조회 전용 화면 중 `[Enter]`를 사용하는 4곳만 변경한다.

| # | 위치 | 메서드 | 현재 | 변경 후 |
|---|------|--------|------|---------|
| 8-1 | `MonitoringView` | `showOrderSummary()` | `[Enter] 메뉴로 돌아가기` | `[0] 위로` |
| 8-2 | `MonitoringView` | `showStockStatus()` | `[Enter] 메뉴로 돌아가기` | `[0] 위로` |
| 8-3 | `SampleView` | `listSamples()` — 비페이지 경로 | `[Enter] 메뉴로 돌아가기` | `[0] 위로` |
| 8-4 | `SampleView` | `searchSample()` — 비페이지 경로 | `[Enter] 메뉴로 돌아가기` | `[0] 위로` |

---

## 4. 변경하지 않는 곳

아래는 원칙에 따라 `[Enter]`가 맞으므로 유지한다.

| 위치 | 유형 | 유지 이유 |
|------|------|-----------|
| `OrderView.placeOrder()` — 오류, 취소, 완료 | 완료·오류 | 행동 결과 확인 |
| `ApprovalView.handleApprovalConfirm()` | 완료 | 승인/거절 결과 확인 |
| `ApprovalView.run()` — 빈 목록 | 빈 목록 안내 | 정보성 메시지 |
| `ProductionView.completeProduction()` — 취소, 완료 | 완료·취소 | 행동 결과 확인 |
| `ProductionView.run()` — IDLE | 빈 목록 안내 | 정보성 메시지 |
| `ReleaseView.processRelease()` — 취소, 완료 | 완료·취소 | 행동 결과 확인 |
| `ReleaseView.run()` — 빈 목록 | 빈 목록 안내 | 정보성 메시지 |
| `SampleView.registerSample()` — 완료, 오류 | 완료·오류 | 행동 결과 확인 |

---

## 5. 상세 코드 변경

### 5-1. MonitoringView.showOrderSummary()

```java
// 변경 전
ConsoleHelper.readLine("\n  [Enter] 메뉴로 돌아가기");

// 변경 후
ConsoleHelper.readLine("\n  [0] 위로 > ");
```

> `readLine`의 반환값은 사용하지 않는다.  
> 어떤 입력이든 (Enter 포함) 다음 코드로 진행되므로 [0]을 입력하거나 그냥 Enter를 눌러도 동일하게 동작한다.  
> 프롬프트 텍스트만 바꿔 사용자에게 [0]을 안내한다.

### 5-2. MonitoringView.showStockStatus()

```java
// 변경 전
ConsoleHelper.readLine("\n  [Enter] 메뉴로 돌아가기");

// 변경 후
ConsoleHelper.readLine("\n  [0] 위로 > ");
```

### 5-3. SampleView.listSamples() — 비페이지네이션 경로

비페이지네이션 경로는 5건 이하일 때 진입한다.

```java
// 변경 전
if (!paginator.needsPagination()) {
    ConsoleHelper.readLine("\n  [Enter] 메뉴로 돌아가기");
    return;
}

// 변경 후
if (!paginator.needsPagination()) {
    ConsoleHelper.readLine("\n  [0] 위로 > ");
    return;
}
```

빈 목록 경로(`isEmpty()`)는 완료 메시지이므로 `[Enter]` 유지.

### 5-4. SampleView.searchSample() — 비페이지네이션 경로

동일 패턴.

```java
// 변경 전
if (!paginator.needsPagination()) {
    ConsoleHelper.readLine("\n  [Enter] 메뉴로 돌아가기");
    return;
}

// 변경 후
if (!paginator.needsPagination()) {
    ConsoleHelper.readLine("\n  [0] 위로 > ");
    return;
}
```

검색 결과 없음 경로는 `[Enter]` 유지.

---

## 6. 변경 전·후 화면 흐름 비교

### MonitoringView (변경 전)
```
[4] 모니터링 > 재고량 확인
────────────────────────
  시료별 재고 현황
  ...
  [Enter] 메뉴로 돌아가기   ← 사용자가 입력 후 메뉴로 복귀
```

### MonitoringView (변경 후)
```
[4] 모니터링 > 재고량 확인
────────────────────────
  시료별 재고 현황
  ...
  [0] 위로 >               ← [0] 또는 아무 키로 복귀
```

---

## 7. TDD 테스트 계획

이번 변경은 `readLine()` 호출의 프롬프트 문자열만 바뀌는 순수 UI 수정이다.  
자동 테스트 대상이 아니며 기존 91개 테스트 전체 통과로 완료 확인한다.

수동 확인 항목:

| 확인 | 기대 동작 |
|------|-----------|
| `[4] 모니터링 → [1] 주문량 확인` | `[0] 위로 >` 프롬프트 표시, 입력 후 모니터링 메뉴로 복귀 |
| `[4] 모니터링 → [2] 재고량 확인` | 동일 |
| `[1] 시료 관리 → [2] 시료 목록` (5건 이하) | `[0] 위로 >` 프롬프트 표시 |
| `[1] 시료 관리 → [3] 시료 검색` (결과 5건 이하) | `[0] 위로 >` 프롬프트 표시 |
| `[2] 시료 주문 → 완료 후` | `[Enter] 메뉴로 돌아가기` 유지 확인 |
| `[3] 주문 승인 → 승인 완료 후` | `[Enter] 메뉴로 돌아가기` 유지 확인 |

---

## 8. 작업 목록

| # | 작업 | 파일 |
|---|------|------|
| 8-1 | `MonitoringView.showOrderSummary()` 프롬프트 변경 | `MonitoringView.java` |
| 8-2 | `MonitoringView.showStockStatus()` 프롬프트 변경 | `MonitoringView.java` |
| 8-3 | `SampleView.listSamples()` 비페이지 경로 프롬프트 변경 | `SampleView.java` |
| 8-4 | `SampleView.searchSample()` 비페이지 경로 프롬프트 변경 | `SampleView.java` |

---

## 9. 완료 기준

- [ ] 조회 전용 4곳에서 `[0] 위로 >` 프롬프트 표시 확인
- [ ] 완료·오류·빈 목록 화면에서 `[Enter] 메뉴로 돌아가기` 유지 확인
- [ ] 기존 테스트 91개 전체 통과
- [ ] 컴파일 경고 없음
