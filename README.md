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
| `token-ledger-micrometer` | `LedgerListener` 기반 Micrometer 메트릭 발행 | tag whitelist 및 metric metadata 구현 완료 |
| `token-ledger-budget` | 누적 비용 저장, 예산 평가, 초과 예외 | 기본 구현 완료, 정책 확장 필요 |
| `token-ledger-autoconfigure` | Spring Boot 자동 설정, 프로퍼티 바인딩 | 기본 구현 및 테스트 완료 |
| `token-ledger-starter` | 사용자용 통합 의존성 | 기본 dependency bundle 구현 완료 |
| `token-ledger-sample-app` | 로컬 데모 앱 | starter smoke 검증 완료, E2E 데모 확장 필요 |

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
- Micrometer 고카디널리티 방어:
  - 기본 허용 tag: `tenant_id`
  - 생성자 기반 tag whitelist 설정
  - 허용되지 않은 `user_id`, `request_id` 등은 metric tag에서 제외
- Micrometer metric metadata:
  - token metric `description`
  - token metric `baseUnit = tokens`
  - cost metric `description`
  - cost metric `baseUnit = currency`
- 기본 예산 평가:
  - 80% 미만 `ALLOW`
  - 80% 이상 `WARN`
  - 100% 이상 `BLOCK`
- Spring Boot 자동 설정:
  - `token-ledger.*` 프로퍼티 바인딩
  - 설정 기반 pricing plan 등록
  - core, Spring AI, Micrometer, Budget bean 조건부 등록
  - `ChatClientCustomizer`를 통한 `LedgerAdvisor` 자동 연결
  - budget enabled 시 `LedgerAdvisor`와 budget evaluator/store 연결

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

- `token-ledger-starter/build.gradle`을 최종 사용자 진입점답게 유지합니다.
- starter가 core, Spring AI adapter, Micrometer, Budget, autoconfigure를 함께 제공하게 유지합니다.
- sample app이 개별 하위 모듈이 아니라 starter만 바라보도록 유지합니다.
- README에 starter 설치, 설정, 검증 방법을 계속 갱신합니다.
- Maven publish 후 외부 consumer app이 starter 하나만으로 실행되는지 검증합니다.

Starter에서 하지 않는 일:

- bean 자동 등록 로직 구현
- `@AutoConfiguration` 작성
- pricing/budget property binding 구현
- Spring AI advisor를 직접 생성하는 application code 작성

위 작업은 `token-ledger-autoconfigure` 담당 범위이며, 기본 구현은 완료되었습니다.

### Starter/Autoconfigure Contract

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

`token-ledger-sample-app`은 starter 통합 검증용 앱으로 둡니다. 현재는 starter classpath, autoconfigure bean 등록, actuator/prometheus 노출을 smoke 수준으로 확인합니다.

현재 endpoint:

- `GET /test/token-ledger/smoke`: sample app 실행 확인
- `GET /test/token-ledger/beans`: autoconfigure가 등록한 주요 bean 존재 여부 확인
- `GET /actuator/prometheus`: Micrometer/Prometheus 노출 확인

현재 bean endpoint의 기대 응답:

```json
{
  "ledgerManager": true,
  "ledgerAdvisor": true,
  "pricingRegistry": true
}
```

다음 MVP E2E endpoint:

- `GET /test/token-ledger/record`: `LedgerManager.record(...)`를 직접 호출해 deterministic token usage event를 기록
- `GET /test/token-ledger/budget`: budget enabled/limit 동작 확인
- `GET /test/token-ledger/chat`: fake/mock provider 또는 실제 provider 설정으로 Spring AI `ChatClient`와 `LedgerAdvisor` 경로 확인

E2E endpoint를 호출한 뒤 `/actuator/prometheus`에서 `ai.token.*` metric이 생성되는지 확인합니다. 단순 actuator 출력의 `jvm_*`, `application_*`, `http_server_*` metric은 Prometheus 노출 확인일 뿐 token-ledger 기록 검증은 아닙니다.

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

autoconfigure bean 등록 테스트:

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

Maven publish 후에는 멀티 모듈 내부 `project(':token-ledger-starter')`가 아니라 published artifact를 사용하는 외부 consumer fixture에서도 같은 검증을 반복합니다.

## Autoconfigure 구현 가이드

`token-ledger-autoconfigure`는 starter가 끌고 온 모듈들을 Spring Boot 애플리케이션에서 자동으로 조립하는 모듈입니다. 사용자는 `token-ledger-starter`만 추가하고 `application.yml`에 `token-ledger.*` 설정을 넣으면 기본 bean이 등록되어야 합니다.

Autoconfigure의 목표:

- 사용자가 직접 `LedgerManager`, `PricingRegistry`, `LedgerAdvisor` 등을 생성하지 않아도 됩니다.
- 사용자가 직접 `ChatClient.Builder`에 advisor를 붙이지 않아도 됩니다.
- 사용자가 설정한 pricing/budget/metrics 옵션이 bean 생성에 반영됩니다.
- 사용자가 직접 bean을 등록한 경우에는 autoconfigure 기본 bean이 덮어쓰지 않습니다.

### Autoconfigure 담당 범위

- `TokenLedgerAutoConfiguration` 유지
- `TokenLedgerProperties` 유지
- pricing plan property binding 유지
- budget property binding 유지
- metrics/tag whitelist property binding 유지
- 기본 bean 조건부 등록 유지
- `AutoConfiguration.imports` 유지
- `ApplicationContextRunner` 기반 테스트 유지 및 확장

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

### Core internal 생성 원칙

`core.internal`의 기본 구현체는 package-private로 유지합니다. autoconfigure 모듈은 구현체를 직접 생성하지 않고 public factory인 `LedgerComponents`를 통해 생성합니다.

현재 방향:

```java
public final class LedgerComponents {

    public static CostCalculator defaultCostCalculator() {
        return new DefaultCostCalculator();
    }

    public static PricingRegistry inMemoryPricingRegistry(List<PricingProvider> providers) {
        return new InMemoryPricingRegistry(providers);
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

기본 구현체를 public으로 공개하지 않습니다. 새 internal 구현체가 다른 모듈에서 필요해지면 먼저 public factory/API를 추가하는 방향을 우선합니다.

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
- pricing 설정이 `PricingProvider`, `PricingRegistry`, `LedgerManager` 비용 계산 경로에 반영됩니다.
- 사용자 정의 bean이 있으면 autoconfigure bean이 덮어쓰지 않습니다.
- Micrometer `MeterRegistry`가 있을 때만 metrics publisher가 등록됩니다.
- budget disabled면 `BudgetEvaluator`가 등록되지 않습니다.
- budget enabled면 `BudgetEvaluator`, `BudgetStateStore`가 등록됩니다.
- budget enabled면 `LedgerAdvisor`가 `BudgetEvaluator`를 호출합니다.
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

Autoconfigure basic 작업은 다음이 만족되면 완료로 봅니다.

- `token-ledger-starter`만 의존한 sample app이 실행됩니다.
- `token-ledger.*` 설정으로 pricing plan이 등록됩니다.
- 주요 bean smoke endpoint에서 `ledgerManager`, `ledgerAdvisor`, `pricingRegistry`가 true로 확인됩니다.
- `/actuator/prometheus`가 노출됩니다.
- `./gradlew test`가 통과합니다.

MVP E2E 완료 기준은 별도로 봅니다.

- sample app endpoint가 `LedgerManager.record(...)`를 호출합니다.
- ledger 기록 후 `/actuator/prometheus`에 `ai.token.*` metric이 노출됩니다.
- Spring AI `ChatClient` 호출 경로에서 `LedgerAdvisor`가 동작합니다.
- Maven local 또는 remote에 publish된 `token-ledger-starter`를 외부 consumer fixture가 의존성으로 받아 실행됩니다.

## 해야 할 일

### 1. Sample app E2E 검증

MVP의 다음 핵심 작업입니다. smoke endpoint는 준비됐으므로 실제 ledger event와 metric 생성을 검증합니다.

- sample app이 `project(':token-ledger-starter')`만 직접 의존하도록 유지
- `token-ledger.pricing.*` 설정 예시 추가
- `LedgerManager.record(...)` 직접 호출 endpoint 추가
- 호출 후 `/actuator/prometheus`에서 `ai.token.*` metric 확인
- budget enabled/limit 검증 endpoint 추가
- fake/mock provider 또는 실제 provider 설정으로 Spring AI `ChatClient` advisor 경로 검증
- docker-compose로 Prometheus/Grafana 확인 플로우 문서화

### 2. Maven publish와 consumer 검증

sample app은 멀티 모듈 내부 project dependency를 쓰므로, 실제 사용자 설치 검증은 published artifact 기반으로 별도 확인합니다.

- Gradle `maven-publish` 설정 추가
- artifact id/version/POM metadata 정리
- `token-ledger-starter` POM이 core, spring-ai, micrometer, budget, autoconfigure를 올바르게 끌고 가는지 확인
- `publishToMavenLocal` 후 외부 consumer fixture에서 `implementation 'io.springai.ledger:token-ledger-starter:0.0.1-SNAPSHOT'` 검증
- remote Maven target 선택: GitHub Packages, Maven Central, 또는 사내 repo
- remote publish 문서화 및 CI publish 전략 정리

### 3. 빌드 의존성 정리

현재 루트 `build.gradle`에서 모든 subproject에 Spring Boot plugin, actuator, prometheus registry가 들어갑니다. 라이브러리 모듈에는 과합니다.

- `core`는 Spring Boot 의존성을 제거하고 순수 Java 모듈에 가깝게 유지
- actuator/prometheus는 sample app 또는 micrometer 모듈로 한정
- Boot plugin은 실행 앱인 `token-ledger-sample-app` 중심으로 적용
- 라이브러리 모듈은 `java-library` 중심으로 유지

### 4. Micrometer 설정 객체화

tag whitelist와 metric description/base unit은 구현되었습니다. 다음 단계는 autoconfigure가 설정을 넘기기 쉽게 Micrometer 옵션을 객체로 정리하는 것입니다.

- `MetricsOptions` 또는 `MetricsProperties` 도입
- 설정 후보:
  - `enabled`
  - `allowedTagKeys`
  - `publishDistributionSummary`
- 기본값:
  - `enabled = true`
  - `allowedTagKeys = ["tenant_id"]`
  - `publishDistributionSummary = true`
- null/empty tag map 테스트 보강
- 여러 allowed tag가 동시에 붙는 케이스 테스트
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

### 7. 문서와 테스트 보강

- README 사용 예시 추가
- 설정 예시 YAML 추가
- 모듈별 public API 설명 추가
- starter 사용법과 Maven publish 사용법 추가
- 외부 consumer fixture 실행법 추가
- sample app E2E 테스트 추가

## 권장 구현 순서

1. sample app에 `LedgerManager.record(...)` E2E endpoint를 추가하고 token-ledger Prometheus metric을 확인합니다.
2. sample app에서 budget enabled/limit E2E를 확인합니다.
3. fake/mock provider 또는 실제 provider 설정으로 Spring AI `ChatClient` advisor E2E를 확인합니다.
4. `maven-publish`를 추가하고 `publishToMavenLocal`로 starter artifact를 생성합니다.
5. 외부 consumer fixture에서 published starter 하나만 의존해 실행되는지 확인합니다.
6. remote Maven repository publish를 설정합니다.
7. 루트 Gradle 의존성을 라이브러리 구조에 맞게 정리합니다.
8. Micrometer 설정 객체를 도입하고 Budget/streaming 기능을 확장합니다.

## 테스트

```bash
./gradlew test
```
