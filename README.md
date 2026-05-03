# Token Ledger

Token Ledger is a lightweight Java/Spring library for tracking Spring AI token usage, calculating model cost, publishing Micrometer metrics, and enforcing budget policy.

The intended user experience is one dependency plus `token-ledger.*` configuration:

```gradle
dependencies {
    implementation 'io.springai.ledger:token-ledger-starter'
}
```

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
  budget:
    enabled: false
    monthly-limit: 10.00
```

## What It Does

- Normalizes AI token usage into `TokenUsage`.
- Calculates model cost from `PricingPlan` using `BigDecimal`.
- Records usage and cost through `LedgerManager`.
- Publishes token and cost metrics through Micrometer.
- Integrates with Spring AI `ChatClient` through `LedgerAdvisor`.
- Supports basic budget evaluation with allow, warn, and block decisions.
- Provides Spring Boot autoconfiguration through `token-ledger-starter`.

## Modules

| Module | Purpose | Status |
| --- | --- | --- |
| `token-ledger-core` | Core domain, pricing, cost calculation, ledger interfaces | Basic implementation complete |
| `token-ledger-spring-ai` | Spring AI usage extraction and advisor integration | Basic implementation complete |
| `token-ledger-micrometer` | Micrometer listener for token and cost metrics | Basic implementation complete |
| `token-ledger-budget` | Budget state store and budget evaluator | Basic implementation complete |
| `token-ledger-autoconfigure` | Spring Boot autoconfiguration and property binding | Basic implementation complete |
| `token-ledger-starter` | Final user dependency bundle | Basic implementation complete |
| `token-ledger-sample-app` | Local starter verification app | Smoke verification complete; E2E demo pending |

## Metrics

Token Ledger publishes:

- `ai.token.usage.total`
- `ai.token.usage.distribution`
- `ai.token.cost.total`

Metric tags are filtered by an allowlist to avoid high-cardinality tags by default. The default allowed tag is:

```text
tenant_id
```

Spring Boot actuator metrics such as `jvm_*`, `application_*`, and `http_server_*` only prove that Prometheus exposure is working. Token Ledger metrics appear after a ledger event is recorded.

## Sample App

In this repository the sample app uses the local starter project:

```gradle
dependencies {
    implementation project(':token-ledger-starter')
}
```

Run it:

```bash
./gradlew :token-ledger-sample-app:bootRun
```

Check the starter smoke endpoint:

```bash
curl http://localhost:8080/test/token-ledger/smoke
```

Check autoconfigured beans:

```bash
curl http://localhost:8080/test/token-ledger/beans
```

Expected shape:

```json
{
  "ledgerManager": true,
  "ledgerAdvisor": true,
  "pricingRegistry": true,
  "microCostMetricsPublisher": true
}
```

Record a deterministic ledger event:

```bash
curl http://localhost:8080/test/token-ledger/record
```

When budget is enabled, check budget behavior:

```bash
curl http://localhost:8080/test/token-ledger/budget
```

Check Prometheus exposure:

```bash
curl http://localhost:8080/actuator/prometheus
```

## Current Status

The starter and autoconfigure path is implemented at a basic level:

- `token-ledger-starter` pulls in the runtime modules.
- `token-ledger-autoconfigure` registers the core, Spring AI, Micrometer, and Budget beans conditionally.
- `token-ledger.pricing.*` is bound into pricing plans and connected to `PricingRegistry`.
- Budget beans are connected to `LedgerAdvisor` when budget is enabled.
- The sample app confirms starter classpath, bean registration, direct ledger recording, Spring AI `ChatClient` advisor flow with a fake model, budget behavior, token-ledger metrics, and Prometheus actuator exposure.

Remaining MVP work:

- Add Maven publishing and validate a separate consumer app that depends on the published starter artifact.

## Development

Run all tests:

```bash
./gradlew test
```

Project implementation notes, roadmap, and agent instructions live in [AGENTS.md](AGENTS.md).
