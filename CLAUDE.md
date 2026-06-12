# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# 빌드
./gradlew build

# 테스트 전체 실행
./gradlew test

# 단일 테스트 클래스 실행
./gradlew test --tests "com.example.OrderServiceTest"

# 단일 테스트 메서드 실행
./gradlew test --tests "com.example.OrderServiceTest.shouldCreateOrder"

# 빌드 결과물 제거
./gradlew clean

# 컴파일만 수행
./gradlew classes
```

Windows에서는 `./gradlew` 대신 `gradlew.bat` 또는 `.\gradlew`를 사용한다.

## Project Stack

- **Language:** Java 17 (Temurin)
- **Build:** Gradle 9.3.0 (Kotlin DSL, `build.gradle.kts`)
- **Test:** JUnit 5 (Jupiter, BOM 6.0.0)
- **Group:** `org.example`

## Coding Convention

### 네이밍
- **클래스/인터페이스:** PascalCase (`OrderService`, `SampleRepository`)
- **메서드/변수:** camelCase (`createOrder`, `sampleId`)
- **상수:** UPPER_SNAKE_CASE (`MAX_RETRY_COUNT`)
- **패키지:** 소문자, 단수형 (`org.example.order`)
- **테스트 메서드:** 한글 `@DisplayName` 필수, 메서드명은 camelCase 동사구

### 포맷
- 들여쓰기: 스페이스 4칸
- 줄 최대 길이: 120자
- 여는 중괄호: 같은 줄 (`{`)
- 빈 줄: 메서드 사이 1줄, 논리 블록 사이 1줄

### 코드 스타일
- 불변 우선: 가능하면 `final` 사용
- `var` 사용 금지 — 타입을 명시한다
- `null` 반환 금지 — `Optional` 또는 빈 컬렉션 반환
- 유틸리티 클래스: `private` 생성자 + `final` 클래스
- 주석: WHY가 명확한 경우에만, 한 줄로

### 테스트
- TDD 필수 (Red → Green → Refactor)
- 테스트 클래스명: `{대상클래스}Test`
- mock은 불가피한 경우에만 사용

---

## Commit Convention

| 태그 | 용도 |
|------|------|
| `[feat]` | 새로운 기능 추가 |
| `[fix]` | 버그 수정 |
| `[test]` | 테스트 코드 추가 또는 수정 |
| `[refactor]` | 기능 변경 없는 코드 구조 개선 |
| `[docs]` | 문서 추가 또는 수정 (CLAUDE.md, README 등) |
| `[chore]` | 빌드 설정, 의존성, 프로젝트 설정 변경 |
| `[style]` | 포맷, 공백 등 기능과 무관한 코드 스타일 정리 |

**형식:** `[태그] 변경 내용을 명사형으로 간결하게`

```
[feat] 주문 생성 기능 구현
[fix] 재고 부족 시 예외 미발생 버그 수정
[test] OrderService 주문 취소 시나리오 테스트 추가
[refactor] OrderValidator 검증 로직 분리
[docs] 커밋 컨벤션 CLAUDE.md에 추가
[chore] JUnit 5 의존성 버전 업그레이드
```

---

## Architecture

이 프로젝트는 주문 시스템(Order System) 샘플 애플리케이션이다. 현재 소스 코드 디렉터리(`src/main/java/`, `src/test/java/`)는 비어 있으며 구현을 기다리는 스켈레톤 상태다.

표준 Gradle 레이아웃을 따른다:
- `src/main/java/` — 애플리케이션 소스
- `src/main/resources/` — 리소스 파일
- `src/test/java/` — JUnit 5 테스트
- `src/test/resources/` — 테스트 리소스
