# Spring AI Ledger - Project Status & Guidelines

## 📌 핵심 원칙 (Core Mandates)
- **작업 업데이트 필수:** 모든 기능 구현, 모듈 추가, 주요 변경 사항 발생 시 반드시 이 `GEMINI.md` 파일을 최신화해야 합니다.
- **인터페이스 우선:** 모듈 간 결합도를 낮추기 위해 핵심 인터페이스를 먼저 정의하고 구현합니다.
- **관측 가능성 중심:** 모든 비용 데이터는 Micrometer를 통해 시각화될 수 있어야 합니다.

## 🚀 프로젝트 개요 (Overview)
Spring AI 애플리케이션에서 AI 호출 비용을 계산하고, Micrometer/Prometheus/Grafana를 통해 시각화 및 예산 통제(Budget Control)를 지원하는 라이브러리.

## 🛠 현재 진행 상황 (Progress)

### Phase 1 (MVP) & Phase 3 (Budget Control) 병렬 개발 중
- [x] **프로젝트 초기화:** Java 25, Spring Boot 4.0.5, Spring AI 1.1.0 GA 기반.
- [x] **멀티 모듈 구조 확립:**
    - `core`: 핵심 도메인 및 계산 엔진.
    - `spring-ai`: Spring AI 연동 어댑터.
    - `micrometer`: 메트릭 발행 모듈.
    - `budget`: 예산 임계치 감지 및 정책 차단 (Phase 3 기능 선반영).
    - `autoconfigure`: 자동 설정 모듈.
    - `starter`: 통합 의존성 진입점.
- [ ] **핵심 인터페이스 설계 (Next Step):** `CostCalculator`, `PricingRegistry`, `BudgetEvaluator` 등.

## 📂 모듈별 역할
| 모듈명 | 역할 | 상태 |
| :--- | :--- | :--- |
| `spring-ai-ledger-core` | 비용 계산 로직, Usage 추상화 | 뼈대 생성 |
| `spring-ai-ledger-spring-ai` | ChatModel, StreamingChatModel 인터셉터 | 뼈대 생성 |
| `spring-ai-ledger-micrometer` | MeterRegistry 연동, 태깅 전략 | 뼈대 생성 |
| `spring-ai-ledger-budget` | 예산 통제, 정책 엔진, 한도 초과 처리 | 뼈대 생성 |
| `spring-ai-ledger-autoconfigure` | @AutoConfiguration, 프로퍼티 바인딩 | 뼈대 생성 |
| `spring-ai-ledger-starter` | 사용자용 통합 의존성 | 뼈대 생성 |
