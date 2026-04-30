# Token Ledger

Spring AI 애플리케이션에서 AI 호출 토큰 사용량과 비용을 기록하고, Micrometer 메트릭으로 관측하며, 예산 정책에 따라 호출을 제어하기 위한 경량 멀티 모듈 라이브러리입니다.

## 목표

- AI 모델별 토큰 사용량을 `TokenUsage`로 표준화합니다.
- 모델별 가격표(`PricingPlan`)를 기준으로 비용을 `BigDecimal`로 정밀 계산합니다.
- 호출 비용을 `LedgerManager`에 기록하고 이벤트로 전파합니다.
- Micrometer/Prometheus/Grafana로 비용과 토큰 사용량을 관측합니다.
- tenant/user/model 단위 예산 정책으로 호출을 경고하거나 차단합니다.
- Spring Boot starter 하나로 Spring AI 프로젝트에 붙일 수 있게 만듭니다.

## 모듈 구조

| 모듈 | 역할 | 현재 상태 |
| --- | --- | --- |
| `token-ledger-core` | 핵심 도메인, 가격표, 비용 계산, 기록 인터페이스 | 기본 구현 및 테스트 완료 |
| `token-ledger-spring-ai` | Spring AI `ChatClient` 응답에서 usage 추출, `LedgerAdvisor` 제공 | 기본 구현 및 테스트 완료 |
| `token-ledger-micrometer` | `LedgerListener` 기반 Micrometer 메트릭 발행 | 기본 구현 완료, tag 방어 필요 |
| `token-ledger-budget` | 누적 비용 저장, 예산 평가, 초과 예외 | 기본 구현 완료, 정책 확장 필요 |
| `token-ledger-autoconfigure` | Spring Boot 자동 설정, 프로퍼티 바인딩 | 최우선 구현 필요 |
| `token-ledger-starter` | 사용자용 통합 의존성 | autoconfigure 완성 후 의미 있음 |
| `token-ledger-sample-app` | 로컬 데모 앱 | 실제 Spring AI 호출 데모로 확장 필요 |

## 현재 구현된 것

- 모델별 가격 정책: `PricingPlan`
- 토큰 타입별 사용량: `PROMPT`, `COMPLETION`, `REASONING`, `CACHED_PROMPT`, `CACHED_COMPLETION`
- 비용 계산: 1K 토큰당 단가 기반, 최종 비용 소수점 6자리 반올림
- 비용 기록 이벤트: `CostRecordedEvent`
- 리스너 기반 확장: `LedgerListener`
- Spring AI 응답 usage 추출: `UsageExtractor`
- Spring AI advisor 후처리 기록: `LedgerAdvisor`
- Micrometer 메트릭 발행:
  - `ai.token.usage.total`
  - `ai.token.usage.distribution`
  - `ai.token.cost.total`
- 기본 예산 평가:
  - 80% 미만 `ALLOW`
  - 80% 이상 `WARN`
  - 100% 이상 `BLOCK`

## 해야 할 일

### 1. Autoconfigure 완성

가장 먼저 해야 할 작업입니다. starter를 실제 사용자 앱에서 바로 쓸 수 있게 만드는 핵심입니다.

- `TokenLedgerAutoConfiguration` 생성
- `TokenLedgerProperties` 생성
- 가격표 설정 바인딩
- 예산 설정 바인딩
- tag whitelist 설정 바인딩
- 기본 bean 등록
  - `CostCalculator`
  - `PricingRegistry`
  - `LedgerManager`
  - `UsageExtractor`
  - `LedgerAdvisor`
  - `BudgetEvaluator`
  - `BudgetStateStore`
  - `MicroCostMetricsPublisher`
  - `ChatClientCustomizer`
- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 추가
- `ApplicationContextRunner` 기반 자동 설정 테스트 추가

주의할 점:

- `core.internal` 구현체가 package-private이라 외부 모듈에서 바로 생성할 수 없습니다.
- autoconfigure에서 기본 구현체를 생성하려면 public factory를 만들거나 구현체 공개 범위를 조정해야 합니다.

### 2. 빌드 의존성 정리

현재 루트 `build.gradle`에서 모든 subproject에 Spring Boot plugin, actuator, prometheus registry가 들어갑니다. 라이브러리 모듈에는 과합니다.

- `core`는 Spring Boot 의존성을 제거하고 순수 Java 모듈에 가깝게 유지
- actuator/prometheus는 sample app 또는 micrometer 모듈로 한정
- Boot plugin은 실행 앱인 `token-ledger-sample-app` 중심으로 적용
- 라이브러리 모듈은 `java-library` 중심으로 유지

### 3. Micrometer 고카디널리티 방어

현재 event tag를 그대로 Micrometer tag로 내보냅니다. Prometheus 환경에서는 `user_id`, request id 같은 값이 메모리 폭증을 만들 수 있습니다.

- tag whitelist 추가
- 기본 허용 tag 예시: `tenant_id`, `model`, `token_type`, `currency`
- 위험 tag 차단: `user_id`, `request_id`, `session_id`
- metric description/base unit 추가
- Grafana dashboard JSON 추가

### 4. Budget 정책 확장

현재는 tenant 기준 단순 누적 비용 평가에 가깝습니다. 실제 운영용으로는 정책 모델이 더 필요합니다.

- 일/월/시간 단위 budget window
- tenant/user/model별 limit
- reset 정책
- currency 처리
- Redis 또는 JDBC 기반 `BudgetStateStore`
- WARN 상태 이벤트 발행
- BLOCK 예외를 HTTP 응답으로 변환하는 Spring 옵션
- 호출 전 예상 비용 기반 preflight 평가

### 5. Spring AI 스트리밍 대응

현재는 일반 `ChatClientResponse` 후처리 중심입니다.

- streaming response usage aggregation
- 마지막 chunk metadata 처리
- usage metadata가 없을 때 fallback token estimator
- provider별 metadata adapter
  - OpenAI
  - Anthropic
  - Gemini

### 6. Sample app을 실제 데모로 확장

현재 sample app은 문자열 응답만 반환합니다. 라이브러리 검증용으로는 실제 Spring AI 호출 흐름이 필요합니다.

- `ChatClient` 기반 실제 AI 호출 endpoint 추가
- `token-ledger.pricing.*` 설정 예시 추가
- budget limit 설정 예시 추가
- `/actuator/prometheus`에서 token/cost metric 확인
- docker-compose로 Prometheus/Grafana 확인 플로우 문서화

### 7. 문서와 테스트 보강

- README 사용 예시 추가
- 설정 예시 YAML 추가
- 모듈별 public API 설명 추가
- starter 사용법 추가
- autoconfigure 통합 테스트 추가
- sample app smoke test 추가

## 권장 구현 순서

1. `autoconfigure`를 완성해서 starter 사용 흐름을 닫습니다.
2. 루트 Gradle 의존성을 라이브러리 구조에 맞게 정리합니다.
3. Micrometer tag whitelist를 구현해 운영 위험을 줄입니다.
4. Budget 정책 모델을 확장합니다.
5. streaming usage 추적과 fallback token estimator를 붙입니다.
6. sample app과 README를 실제 데모 기준으로 보강합니다.

## 테스트

```bash
./gradlew test
```

현재 기준 전체 테스트는 통과합니다.
