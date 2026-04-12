# Spring AI Ledger - Project Status & Guidelines

## 📌 핵심 원칙 (Core Mandates)
- **작업 업데이트 필수:** 모든 기능 구현, 모듈 추가, 주요 변경 사항 발생 시 반드시 이 `GEMINI.md` 파일을 최신화해야 합니다.
- **인터페이스 우선:** 모듈 간 결합도를 낮추기 위해 핵심 인터페이스를 먼저 정의하고 구현합니다.
- **관측 가능성 중심:** 모든 비용 데이터는 Micrometer를 통해 시각화 및 관측이 가능해야 합니다.
- **금융급 정밀도:** 비용 계산 시 `BigDecimal`을 사용하여 소수점 정밀도(6자리 권장)를 보장합니다.

## 🚀 프로젝트 개요 (Overview)
Spring AI 애플리케이션에서 AI 호출 비용을 실시간으로 측정, 시각화하고 예산 정책에 따라 호출을 제어하는 엔터프라이즈급 경량 라이브러리입니다.

---

## 🏛 라이브러리 아키텍처 및 설계 가이드

### 1. 4계층 아키텍처 (Architecture Layers)
- **API & Domain (`core`)**: 핵심 모델 및 인터페이스 (의존성 제로).
- **Implementation (`core.internal`)**: 엔진의 기본 로직 구현체 (Default Impl).
- **Adapter (`spring-ai`, `micrometer`, `budget`)**: 외부 프레임워크와 Core 도메인을 연결하는 Bridge.
- **Infrastructure (`autoconfigure`, `starter`)**: 자동 빈 등록 및 Plug-and-Play 지원.

### 2. 모듈 내부 패키지 전략 (Package Strategy)
라이브러리 사용자의 편의성과 캡슐화를 위해 다음과 같은 구조를 권장합니다.

```text
io.springai.ledger.{module}
├── {Name}.java (Public API: Interface)
├── {Name}.java (Domain Model: Record)
├── exception
│   └── {Name}Exception.java (Custom Exceptions)
└── internal
    └── {Name}Impl.java (Private Implementation)
```

- **최상위 패키지**: 사용자가 직접 사용할 `Interface`와 `Record`를 위치시켜 `import` 경로를 최적화합니다.
- **`internal` 패키지**: 실제 구현 클래스를 둡니다. 가능하면 `package-private`으로 선언하여 외부 노출을 엄격히 차단합니다.
- **`domain` 패키지 (선택)**: 데이터 모델이 아주 많아질 경우 최상위 대신 별도 분리합니다.

---

## 🛠 마스터 로드맵 및 진행 상황

### 진행 단계 (Roadmap)
- **Phase 1 (MVP):** 핵심 측정 및 Micrometer 시각화 연동. (진행 중)
- **Phase 1.5:** 스트리밍 응답 추적 및 토큰 추산(Fallback) 로직 강화.
- **Phase 2:** 멀티 테넌시 식별 및 고카디널리티 방어.
- **Phase 3:** 예산 통제(Budget Control) 및 정책 엔진 도입.

### 현재 상태 (Progress)
- [x] **프로젝트 초기화:** Java 25, Spring Boot 4.0.5 기반 멀티 모듈 구조 확립.
- [x] **핵심 인터페이스 및 아키텍처 설계:** 4계층 레이어 및 내부 패키지 전략 수립.
- [ ] **Core 도메인 구현 (Next Step):** `TokenUsage`, `Cost`, `PricingPlan` 등 핵심 모델 작성.

---

## 📂 모듈별 역할 및 인터페이스 명세

### 모듈 역할 상세
| 모듈명 | 역할 | 상태 |
| :--- | :--- | :--- |
| `spring-ai-ledger-core` | 비용 계산 로직, Usage 추상화, 핵심 인터페이스 | 뼈대 생성 |
| `spring-ai-ledger-spring-ai` | ChatModel 어드바이저, Usage 추출 및 변환 | 뼈대 생성 |
| `spring-ai-ledger-micrometer` | MeterRegistry 연동, 비용 메트릭 발행 | 뼈대 생성 |
| `spring-ai-ledger-budget` | 예산 통제, 정책 엔진, 한도 초과 처리 (ERP 협업) | 뼈대 생성 |
| `spring-ai-ledger-autoconfigure` | @AutoConfiguration, 프로퍼티 바인딩 | 뼈대 생성 |
| `spring-ai-ledger-starter` | 사용자용 통합 의존성 진입점 | 뼈대 생성 |

### 핵심 인터페이스 스펙
- **`core`**: `PricingRegistry`, `CostCalculator`, `LedgerManager`.
- **`spring-ai`**: `UsageExtractor`, `LedgerAdvisor`.
- **`budget`**: `BudgetEvaluator` (ERP 담당 팀원 구현 포인트).
- **`micrometer`**: `CostMetricsPublisher`.

---

## 🧠 주요 기술적 챌린지 (Technical Challenges)
1. **스트리밍 응답 추적:** Flux 응답의 마지막 청크에서 Usage를 정확히 캡처하는 로직.
2. **고카디널리티 제어:** 가변 태그(user_id 등)가 Prometheus 메모리에 주는 부하 방어.
3. **Context 전파:** MVC/WebFlux 환경에서 테넌트 식별자 유실 방지.
