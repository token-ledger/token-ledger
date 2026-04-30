# Token Ledger Agent Guide

## Project Summary

Token Ledger is a multi-module Java/Spring library for tracking Spring AI token usage, calculating model costs, publishing Micrometer metrics, and enforcing budget policy.

Primary goal: users should eventually add one dependency, `token-ledger-starter`, configure `token-ledger.*`, and get automatic cost tracking for Spring AI calls.

## Agent Rules

- Update this `AGENTS.md` whenever a meaningful feature, module, roadmap, or architectural decision changes.
- Prefer interface-first design across module boundaries.
- Keep core domain code precise and dependency-light.
- Use `BigDecimal` for monetary calculations.
- Avoid high-cardinality Micrometer tags by default.
- Do not place business logic in `token-ledger-starter`; keep starter as a thin user entrypoint.
- If implementation classes stay under `internal`, expose them to other modules through deliberate public factories or public configuration APIs.

## Architecture

| Layer | Modules | Responsibility |
| --- | --- | --- |
| API & Domain | `token-ledger-core` | Core models, pricing, cost calculation interfaces, ledger interfaces |
| Adapter | `token-ledger-spring-ai`, `token-ledger-micrometer`, `token-ledger-budget` | Integrate with Spring AI, Micrometer, and budget policy |
| Infrastructure | `token-ledger-autoconfigure`, `token-ledger-starter` | Spring Boot auto-configuration and final user dependency |
| Demo | `token-ledger-sample-app` | Local verification app for starter/autoconfigure integration |

## Module Status

| Module | Status | Notes |
| --- | --- | --- |
| `token-ledger-core` | Basic implementation complete | Domain records, pricing, calculator, registry, ledger manager |
| `token-ledger-spring-ai` | Basic implementation complete | `UsageExtractor`, `LedgerAdvisor`, response usage recording |
| `token-ledger-micrometer` | Basic implementation complete | Needs tag whitelist/high-cardinality protection |
| `token-ledger-budget` | Basic implementation complete | Needs richer policy/window/store support |
| `token-ledger-autoconfigure` | Pending team work | Should own bean registration and property binding |
| `token-ledger-starter` | Current focus | Should become final user entrypoint |
| `token-ledger-sample-app` | Current focus | Should validate starter integration |

## Current Work Focus

The current local workstream is starter readiness, not autoconfigure implementation.

Starter tasks:

- Clarify `token-ledger-starter` dependency intent.
- Keep sample app dependent on `project(':token-ledger-starter')`.
- Add README instructions for starter usage.
- Prepare sample app smoke checks that work before autoconfigure exists.
- Prepare TODO tests that can be enabled after autoconfigure lands.

Autoconfigure is assigned to another teammate. Do not implement it unless explicitly asked.

## Starter Contract

Expected final user setup:

```gradle
dependencies {
    implementation 'io.springai.ledger:token-ledger-starter'
}
```

In this repository, sample app verification uses:

```gradle
dependencies {
    implementation project(':token-ledger-starter')
}
```

Starter should include the modules users need at runtime, especially `token-ledger-autoconfigure`. The starter should not create beans itself.

## Autoconfigure Contract

The autoconfigure module should eventually provide:

- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- `TokenLedgerAutoConfiguration`
- `TokenLedgerProperties`
- Pricing property binding
- Budget property binding
- Metrics/tag whitelist property binding
- Conditional beans for:
  - `CostCalculator`
  - `PricingRegistry`
  - `LedgerManager`
  - `UsageExtractor`
  - `LedgerAdvisor`
  - `BudgetEvaluator`
  - `BudgetStateStore`
  - `MicroCostMetricsPublisher`
  - `ChatClientCustomizer`

Shared configuration prefix:

```yaml
token-ledger:
  enabled: true
```

## Recommended Configuration Shape

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

## Sample App Direction

`token-ledger-sample-app` should be a starter integration verification app.

Near-term endpoints:

- `GET /test/token-ledger/smoke`: app is running and starter is on classpath.
- `GET /test/token-ledger/beans`: reports whether expected autoconfigure beans exist.
- `GET /actuator/prometheus`: validates actuator/prometheus exposure.

The bean endpoint should avoid hard bean requirements until autoconfigure exists. Use `ApplicationContext#containsBean(...)` or type-safe optional lookups so the app still starts.

## Roadmap

1. Starter entrypoint cleanup.
2. Sample app starter smoke verification.
3. Autoconfigure contract alignment.
4. Autoconfigure implementation by teammate.
5. Bean smoke tests enabled after autoconfigure lands.
6. Gradle dependency cleanup so library modules are not overloaded with app dependencies.
7. Micrometer tag whitelist and metric metadata.
8. Budget policy expansion.
9. Streaming usage aggregation and fallback token estimation.

## Known Risks

- Root `build.gradle` currently applies Spring Boot plugin and actuator/prometheus dependencies to every subproject. This is heavy for library modules.
- `core.internal` implementation classes are package-private. Autoconfigure cannot instantiate them directly unless a public factory/API is introduced.
- Micrometer publisher currently forwards all event tags; this can cause high-cardinality metric explosion.
- Autoconfigure is currently not implemented, so starter cannot provide true zero-config behavior yet.

## Verification

Run all tests:

```bash
./gradlew test
```

Run sample app after implementation work:

```bash
./gradlew :token-ledger-sample-app:bootRun
```

Check Prometheus metrics:

```bash
curl http://localhost:8080/actuator/prometheus
```

## Update History

### 2026-04-30

- Renamed project guidance from `GEMINI.md` to `AGENTS.md`.
- Added README roadmap for current project gaps.
- Added starter-focused workstream guidance.
- Clarified that `token-ledger-starter` is the current user-entrypoint task while autoconfigure is owned separately.

### 2026-04-19

- Moved core domain records into `io.tokenledger.core.domain`.
- Tightened visibility of core internal default implementations.
- Updated dependent modules to use the new domain package structure.
- Verified tests after the package refactor.

### 2026-04-14

- Added token type support including prompt, completion, reasoning, and cached tokens.
- Added token-type-specific pricing fallback logic.
- Updated Spring AI integration for Spring AI 1.1.4 module split.
- Added usage extraction and advisor tests.
