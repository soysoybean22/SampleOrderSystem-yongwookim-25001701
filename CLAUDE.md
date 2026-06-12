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

## Architecture

이 프로젝트는 주문 시스템(Order System) 샘플 애플리케이션이다. 현재 소스 코드 디렉터리(`src/main/java/`, `src/test/java/`)는 비어 있으며 구현을 기다리는 스켈레톤 상태다.

표준 Gradle 레이아웃을 따른다:
- `src/main/java/` — 애플리케이션 소스
- `src/main/resources/` — 리소스 파일
- `src/test/java/` — JUnit 5 테스트
- `src/test/resources/` — 테스트 리소스
