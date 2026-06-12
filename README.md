# 반도체 시료 생산주문관리 시스템

가상의 반도체 회사 **S-Semi**의 시료 생산주문을 관리하는 콘솔 기반 애플리케이션입니다.

## 기술 스택

| 항목 | 내용 |
|------|------|
| Language | Java 17 (Temurin) |
| Build | Gradle 9.3.0 (Kotlin DSL) |
| Test | JUnit 5 (Jupiter) |
| 데이터 저장 | JSON 파일 (`data/*.json`) |
| 아키텍처 | MVC 패턴 |

---

## 시작하기

### 요구사항

- Java 17 이상
- Gradle (Wrapper 포함 — 별도 설치 불필요)

### 빌드

```cmd
gradlew build
```

### 테스트 실행

```cmd
gradlew test
```

---

## 실행 방법

### 메인 앱 실행 (메인 메뉴)

```cmd
gradlew run
```

### 더미 데이터 생성

테스트용 초기 데이터를 `data/` 폴더에 생성합니다.  
시료 5종, 주문 10건, 생산 작업 2건이 생성됩니다.

```cmd
gradlew run --args="dummy"
```

### 데이터 현황 조회 (관리자 도구)

`data/*.json`에 저장된 전체 데이터를 테이블 형태로 출력합니다.

```cmd
gradlew run --args="monitor"
```

---

## 메인 메뉴 구성

```
================================================================
        반도체 시료 생산주문관리 시스템
================================================================
시스템 현황  2026-04-16 09:32:15

등록 시료   5종    총 재고   1,640 ea
전체 주문  10건    생산라인   2건 대기    승인 대기  3건

  [1] 시료 관리          [2] 시료 주문
  [3] 주문 승인/거절     [4] 모니터링
  [5] 생산라인 조회      [6] 출고 처리
  [0] 종료
```

| 메뉴 | 기능 |
|------|------|
| [1] 시료 관리 | 시료 등록 / 목록 조회 / 이름 검색 |
| [2] 시료 주문 | 고객 주문 접수 (`RESERVED`) |
| [3] 주문 승인/거절 | 재고 확인 후 승인(`CONFIRMED`/`PRODUCING`) 또는 거절(`REJECTED`) |
| [4] 모니터링 | 상태별 주문 건수 + 시료별 재고 현황(여유/부족/고갈) |
| [5] 생산라인 조회 | FIFO 생산 큐 확인 및 생산 완료 처리 |
| [6] 출고 처리 | CONFIRMED 주문 출고 실행(`RELEASE`) |

---

## 주문 상태 흐름

```
RESERVED ──→ CONFIRMED ──→ RELEASE
         └──→ PRODUCING ─→ CONFIRMED ──→ RELEASE
         └──→ REJECTED
```

| 상태 | 의미 |
|------|------|
| `RESERVED` | 주문 접수 |
| `CONFIRMED` | 승인 완료 + 출고 대기 |
| `PRODUCING` | 재고 부족으로 생산 중 |
| `RELEASE` | 출고 완료 |
| `REJECTED` | 주문 거절 |

---

## 실 생산량 계산

재고 부족 시 생산 수량은 수율과 오차 보정을 적용해 계산합니다.

```
실 생산량 = ceil( 부족분 / (수율 × 0.9) )
총 생산시간 = 평균 생산시간 × 실 생산량
```

---

## 프로젝트 구조

```
src/main/java/org/example/
├── Main.java
├── model/          # 도메인 객체
├── repository/     # JSON 파일 CRUD
├── controller/     # 비즈니스 로직
├── view/           # 콘솔 화면
└── tool/           # DataMonitorTool, DummyDataGenerator

data/               # JSON 데이터 저장소 (자동 생성)
docs/               # 설계 문서 (PRD, PLAN, Phase별 설계)
```

---

## 문서

- [`docs/PRD.md`](docs/PRD.md) — 요구사항 정의서
- [`docs/PLAN.md`](docs/PLAN.md) — 구현 계획 및 체크리스트
- [`docs/design/`](docs/design/) — Phase별 상세 설계
