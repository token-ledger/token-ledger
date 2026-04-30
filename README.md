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

## Starter 작업 가이드

`token-ledger-starter`는 최종 사용자가 가장 먼저 만나는 진입점입니다. 이 모듈은 자체 비즈니스 로직을 많이 갖기보다, 사용자가 의존성 하나만 추가해도 core, Spring AI adapter, Micrometer, Budget, autoconfigure가 함께 들어오게 하는 얇은 dependency bundle 역할을 합니다.

최종 사용자가 기대하는 사용 방식:

```gradle
dependencies {
    implementation 'io.springai.ledger:token-ledger-starter'
}
```

현재 멀티 모듈 개발 환경에서는 다음처럼 프로젝트 의존성으로 검증합니다.

```gradle
dependencies {
    implementation project(':token-ledger-starter')
}
```

### Starter 담당 범위

- `token-ledger-starter/build.gradle`을 최종 사용자 진입점답게 정리합니다.
- starter가 포함해야 하는 모듈 경계를 명확히 합니다.
- sample app이 개별 하위 모듈이 아니라 starter만 바라보도록 유지합니다.
- README에 starter 설치, 설정, 검증 방법을 계속 갱신합니다.
- autoconfigure가 들어오면 바로 확인할 수 있는 smoke test와 sample endpoint를 준비합니다.

Starter에서 하지 않는 일:

- bean 자동 등록 로직 구현
- `@AutoConfiguration` 작성
- pricing/budget property binding 구현
- Spring AI advisor를 직접 생성하는 application code 작성

위 작업은 `token-ledger-autoconfigure` 담당 범위입니다.

### Autoconfigure 팀과 맞출 Contract

starter와 autoconfigure는 다음 계약을 기준으로 맞춥니다.

- starter는 `token-ledger-autoconfigure`를 포함합니다.
- autoconfigure는 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`를 제공합니다.
- 사용자는 starter만 추가해도 자동 설정이 활성화됩니다.
- 설정 prefix는 `token-ledger`로 고정합니다.
- sample app의 `application.yml`은 autoconfigure contract의 기준 문서로 사용합니다.

권장 설정 예시:

```yaml
token-ledger:
  enabled: true
  pricing:
    plans:
      - model-id: gpt-4o-mini
        currency: USD
        rates:
          PROMPT: 0.00015
          COMPLETION: 0.00060
  metrics:
    enabled: true
    tag-whitelist:
      - tenant_id
      - model
  budget:
    enabled: false
    monthly-limit: 10.00
```

### Sample App 검증 방향

`token-ledger-sample-app`은 starter 통합 검증용 앱으로 둡니다. 초기에는 실제 AI API key 없이도 실행되는 smoke endpoint를 우선 제공합니다.

권장 endpoint:

- `GET /test/token-ledger/smoke`: sample app 실행 확인
- `GET /test/token-ledger/beans`: autoconfigure가 등록한 주요 bean 존재 여부 확인
- `GET /actuator/prometheus`: Micrometer/Prometheus 노출 확인

`/test/token-ledger/beans`는 autoconfigure가 아직 비어 있어도 앱이 죽지 않게 `ApplicationContext#containsBean(...)` 기반으로 작성하는 편이 좋습니다.

예상 응답 예시:

```json
{
  "ledgerManager": false,
  "ledgerAdvisor": false,
  "pricingRegistry": false
}
```

autoconfigure 구현이 들어오면 위 값이 `true`로 바뀌는 식으로 통합 상태를 확인합니다.

### Starter Smoke Test 방향

지금 바로 넣을 수 있는 테스트:

```java
@SpringBootTest
class SampleApplicationSmokeTest {

    @Test
    void contextLoads() {
    }
}
```

autoconfigure가 구현된 뒤 추가할 테스트:

```java
@SpringBootTest(properties = {
    "token-ledger.enabled=true",
    "token-ledger.pricing.plans[0].model-id=gpt-4o-mini",
    "token-ledger.pricing.plans[0].currency=USD",
    "token-ledger.pricing.plans[0].rates.PROMPT=0.00015",
    "token-ledger.pricing.plans[0].rates.COMPLETION=0.00060"
})
class TokenLedgerAutoConfigurationSmokeTest {

    @Autowired
    ApplicationContext context;

    @Test
    void tokenLedgerBeansAreRegistered() {
        assertThat(context).hasSingleBean(LedgerManager.class);
        assertThat(context).hasSingleBean(PricingRegistry.class);
    }
}
```

autoconfigure가 아직 없는 동안에는 bean 등록 검증 테스트를 TODO로만 남기고 활성화하지 않습니다.

## Autoconfigure 구현 가이드

`token-ledger-autoconfigure`는 starter가 끌고 온 모듈들을 Spring Boot 애플리케이션에서 자동으로 조립하는 모듈입니다. 사용자는 `token-ledger-starter`만 추가하고 `application.yml`에 `token-ledger.*` 설정을 넣으면 기본 bean이 등록되어야 합니다.

Autoconfigure의 목표:

- 사용자가 직접 `LedgerManager`, `PricingRegistry`, `LedgerAdvisor` 등을 생성하지 않아도 됩니다.
- 사용자가 직접 `ChatClient.Builder`에 advisor를 붙이지 않아도 됩니다.
- 사용자가 설정한 pricing/budget/metrics 옵션이 bean 생성에 반영됩니다.
- 사용자가 직접 bean을 등록한 경우에는 autoconfigure 기본 bean이 덮어쓰지 않습니다.

### Autoconfigure 담당 범위

- `TokenLedgerAutoConfiguration` 작성
- `TokenLedgerProperties` 작성
- pricing plan property binding
- budget property binding
- metrics/tag whitelist property binding
- 기본 bean 조건부 등록
- `AutoConfiguration.imports` 등록
- `ApplicationContextRunner` 기반 테스트 작성

Autoconfigure에서 하지 않는 일:

- starter에 비즈니스 로직 추가
- sample app 전용 bean 등록
- 실제 provider API 호출 구현
- 운영용 Redis/JDBC budget store 구현

### 권장 파일 구조

```text
token-ledger-autoconfigure
└── src
    ├── main
    │   ├── java
    │   │   └── io/tokenledger/autoconfigure
    │   │       ├── TokenLedgerAutoConfiguration.java
    │   │       ├── TokenLedgerProperties.java
    │   │       ├── PricingPlanProperties.java
    │   │       └── LedgerChatClientCustomizer.java
    │   └── resources
    │       └── META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
    └── test
        └── java
            └── io/tokenledger/autoconfigure
                ├── TokenLedgerAutoConfigurationTest.java
                └── TokenLedgerPropertiesTest.java
```

### Bean 등록 원칙

기본 bean은 모두 조건부로 등록합니다.

- `@ConditionalOnMissingBean`: 사용자가 같은 타입의 bean을 등록하면 사용자 bean을 우선합니다.
- `@ConditionalOnClass`: 관련 모듈이 classpath에 있을 때만 adapter bean을 등록합니다.
- `@ConditionalOnProperty`: `token-ledger.*.enabled` 설정으로 기능별 on/off를 제어합니다.

권장 기본 bean:

| Bean | 조건 | 설명 |
| --- | --- | --- |
| `CostCalculator` | missing bean | core 비용 계산기 |
| `PricingRegistry` | missing bean | 설정 기반 가격표 registry |
| `LedgerManager` | missing bean | 비용 기록 manager |
| `UsageExtractor` | Spring AI classpath + missing bean | Spring AI 응답 usage 추출 |
| `LedgerAdvisor` | Spring AI classpath + missing bean | ChatClient advisor |
| `MicroCostMetricsPublisher` | Micrometer classpath + metrics enabled | 비용/토큰 메트릭 발행 listener |
| `BudgetStateStore` | budget enabled + missing bean | 기본 in-memory budget store |
| `BudgetEvaluator` | budget enabled + missing bean | 기본 budget evaluator |
| `ChatClientCustomizer` | Spring AI classpath + advisor bean | ChatClient에 LedgerAdvisor 연결 |

### Core internal 생성 문제

현재 `core.internal`의 기본 구현체는 package-private입니다. 이 상태에서는 autoconfigure 모듈이 `DefaultCostCalculator`, `DefaultLedgerManager`, `InMemoryPricingRegistry`를 직접 생성할 수 없습니다.

해결 방향은 둘 중 하나로 정합니다.

1. core에 public factory 추가

```java
public final class LedgerComponents {

    public static CostCalculator defaultCostCalculator() {
        return new DefaultCostCalculator();
    }

    public static PricingRegistry inMemoryPricingRegistry() {
        return new InMemoryPricingRegistry();
    }

    public static LedgerManager defaultLedgerManager(
            PricingRegistry pricingRegistry,
            CostCalculator costCalculator,
            List<LedgerListener> listeners
    ) {
        return new DefaultLedgerManager(pricingRegistry, costCalculator, listeners);
    }
}
```

2. 기본 구현체를 public으로 공개

라이브러리 API 노출을 최소화하려면 1번 factory 방식이 더 낫습니다.

### Property Binding 기준

설정 prefix는 `token-ledger`로 고정합니다.

```yaml
token-ledger:
  enabled: true
  pricing:
    plans:
      - model-id: gpt-4o-mini
        currency: USD
        rates:
          PROMPT: 0.00015
          COMPLETION: 0.00060
          REASONING: 0.00060
          CACHED_PROMPT: 0.000075
  metrics:
    enabled: true
    tag-whitelist:
      - tenant_id
      - model
  budget:
    enabled: false
    monthly-limit: 10.00
```

권장 properties 모델:

```java
@ConfigurationProperties(prefix = "token-ledger")
public class TokenLedgerProperties {
    private boolean enabled = true;
    private Pricing pricing = new Pricing();
    private Metrics metrics = new Metrics();
    private Budget budget = new Budget();
}
```

`PricingPlanProperties`는 `modelId`, `currency`, `rates`를 받고 `PricingPlan`으로 변환하는 메서드를 갖습니다.

### ChatClient 연결 기준

autoconfigure는 `LedgerAdvisor`가 등록되어 있을 때 `ChatClientCustomizer`를 등록합니다. customizer는 모든 `ChatClient.Builder`에 advisor를 추가하는 역할만 가져야 합니다.

개념 예시:

```java
@Bean
@ConditionalOnBean(LedgerAdvisor.class)
@ConditionalOnMissingBean
ChatClientCustomizer ledgerChatClientCustomizer(LedgerAdvisor ledgerAdvisor) {
    return builder -> builder.defaultAdvisors(ledgerAdvisor);
}
```

Spring AI 버전에 따라 `ChatClientCustomizer` 패키지나 builder API가 달라질 수 있으므로 현재 적용 중인 Spring AI `1.1.4` 기준으로 컴파일 확인이 필요합니다.

### Autoconfigure 테스트 기준

`ApplicationContextRunner`로 기능별 조건을 검증합니다.

필수 테스트:

- 설정이 없어도 context가 뜹니다.
- pricing 설정이 `PricingProvider` 또는 `PricingRegistry`에 반영됩니다.
- 사용자 정의 bean이 있으면 autoconfigure bean이 덮어쓰지 않습니다.
- Micrometer `MeterRegistry`가 있을 때만 metrics publisher가 등록됩니다.
- budget disabled면 `BudgetEvaluator`가 등록되지 않습니다.
- budget enabled면 `BudgetEvaluator`, `BudgetStateStore`가 등록됩니다.
- Spring AI classpath가 있을 때 `LedgerAdvisor`, `ChatClientCustomizer`가 등록됩니다.

예시:

```java
private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(TokenLedgerAutoConfiguration.class));

@Test
void shouldBindPricingPlans() {
    contextRunner
            .withPropertyValues(
                    "token-ledger.pricing.plans[0].model-id=gpt-4o-mini",
                    "token-ledger.pricing.plans[0].currency=USD",
                    "token-ledger.pricing.plans[0].rates.PROMPT=0.00015",
                    "token-ledger.pricing.plans[0].rates.COMPLETION=0.00060"
            )
            .run(context -> assertThat(context).hasSingleBean(PricingRegistry.class));
}
```

### 완료 기준

Autoconfigure 작업은 다음이 만족되면 완료로 봅니다.

- `token-ledger-starter`만 의존한 sample app이 실행됩니다.
- `token-ledger.*` 설정으로 pricing plan이 등록됩니다.
- 주요 bean smoke endpoint에서 `ledgerManager`, `ledgerAdvisor`, `pricingRegistry`가 true로 확인됩니다.
- `/actuator/prometheus`에 `ai.token.*` metric이 노출됩니다.
- `./gradlew test`가 통과합니다.

## 해야 할 일

### 1. Starter 진입점 정리

현재 담당 작업입니다. autoconfigure 구현과 병렬로 진행할 수 있습니다.

- `token-ledger-starter/build.gradle` 의존성 의도를 주석으로 정리
- sample app이 `project(':token-ledger-starter')`만 직접 의존하도록 유지
- sample app smoke endpoint 추가
- sample app context-load test 추가
- README에 starter 사용법과 sample 실행법 보강
- autoconfigure 완료 후 활성화할 bean smoke test TODO 추가

### 2. Autoconfigure 완성

다른 팀원이 담당하는 작업입니다. starter와 맞물리는 contract를 유지해야 합니다.

- `TokenLedgerAutoConfiguration` 생성
- `TokenLedgerProperties` 생성
- 가격표 설정 바인딩
- 예산 설정 바인딩
- tag whitelist 설정 바인딩
- 기본 bean 등록
- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 추가
- `ApplicationContextRunner` 기반 자동 설정 테스트 추가

### 3. 빌드 의존성 정리

현재 루트 `build.gradle`에서 모든 subproject에 Spring Boot plugin, actuator, prometheus registry가 들어갑니다. 라이브러리 모듈에는 과합니다.

- `core`는 Spring Boot 의존성을 제거하고 순수 Java 모듈에 가깝게 유지
- actuator/prometheus는 sample app 또는 micrometer 모듈로 한정
- Boot plugin은 실행 앱인 `token-ledger-sample-app` 중심으로 적용
- 라이브러리 모듈은 `java-library` 중심으로 유지

### 4. Micrometer 고카디널리티 방어

현재 event tag를 그대로 Micrometer tag로 내보냅니다. Prometheus 환경에서는 `user_id`, request id 같은 값이 메모리 폭증을 만들 수 있습니다.

- tag whitelist 추가
- 기본 허용 tag 예시: `tenant_id`, `model`, `token_type`, `currency`
- 위험 tag 차단: `user_id`, `request_id`, `session_id`
- metric description/base unit 추가
- Grafana dashboard JSON 추가

### 5. Budget 정책 확장

현재는 tenant 기준 단순 누적 비용 평가에 가깝습니다. 실제 운영용으로는 정책 모델이 더 필요합니다.

- 일/월/시간 단위 budget window
- tenant/user/model별 limit
- reset 정책
- currency 처리
- Redis 또는 JDBC 기반 `BudgetStateStore`
- WARN 상태 이벤트 발행
- BLOCK 예외를 HTTP 응답으로 변환하는 Spring 옵션
- 호출 전 예상 비용 기반 preflight 평가

### 6. Spring AI 스트리밍 대응

현재는 일반 `ChatClientResponse` 후처리 중심입니다.

- streaming response usage aggregation
- 마지막 chunk metadata 처리
- usage metadata가 없을 때 fallback token estimator
- provider별 metadata adapter
  - OpenAI
  - Anthropic
  - Gemini

### 7. Sample app을 실제 데모로 확장

현재 sample app은 문자열 응답만 반환합니다. 라이브러리 검증용으로는 실제 Spring AI 호출 흐름이 필요합니다.

- `ChatClient` 기반 실제 AI 호출 endpoint 추가
- `token-ledger.pricing.*` 설정 예시 추가
- budget limit 설정 예시 추가
- `/actuator/prometheus`에서 token/cost metric 확인
- docker-compose로 Prometheus/Grafana 확인 플로우 문서화

### 8. 문서와 테스트 보강

- README 사용 예시 추가
- 설정 예시 YAML 추가
- 모듈별 public API 설명 추가
- starter 사용법 추가
- autoconfigure 통합 테스트 추가
- sample app smoke test 추가

## 권장 구현 순서

1. `token-ledger-starter`를 최종 사용자 진입점으로 정리합니다.
2. sample app을 starter 기반 smoke 검증 앱으로 정리합니다.
3. autoconfigure 팀과 `token-ledger.*` 설정 contract를 맞춥니다.
4. autoconfigure가 들어오면 bean smoke test를 활성화합니다.
5. 루트 Gradle 의존성을 라이브러리 구조에 맞게 정리합니다.
6. Micrometer tag whitelist를 구현해 운영 위험을 줄입니다.
7. Budget 정책 모델을 확장합니다.
8. streaming usage 추적과 fallback token estimator를 붙입니다.

## 테스트

```bash
./gradlew test
```
