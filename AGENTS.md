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
| `token-ledger-micrometer` | Basic implementation complete | Tag whitelist and metric metadata implemented; options object is next |
| `token-ledger-budget` | Basic implementation complete | Needs richer policy/window/store support |
| `token-ledger-autoconfigure` | Basic implementation complete | Bean registration, property binding, pricing/budget wiring, and ChatClient customizer implemented |
| `token-ledger-starter` | Basic implementation complete | Thin final user entrypoint that brings runtime modules together |
| `token-ledger-sample-app` | Current focus | Needs E2E verification for ledger metrics and Spring AI advisor flow |

## Current Work Focus

The current MVP workstream is post-autoconfigure validation and packaging.

MVP tasks:

- Keep sample app dependent on `project(':token-ledger-starter')`.
- Add sample app E2E checks that call `LedgerManager.record(...)` and verify token-ledger Prometheus metrics.
- Add sample app E2E checks for the Spring AI `ChatClient`/`LedgerAdvisor` path, using a fake/mock provider if needed before real API keys are available.
- Add local Maven publishing and an external consumer fixture that depends on the published `token-ledger-starter` artifact.
- Choose and document the remote Maven repository target before public release.

Autoconfigure basic implementation has landed. Future autoconfigure work should be incremental hardening rather than first implementation.

Current Micrometer follow-up:

- Introduce a small metrics options/properties object.
- Keep default allowed tag keys as `tenant_id`.
- Preserve the existing `MicroCostMetricsPublisher(MeterRegistry)` constructor.
- Add tests for null/empty tags and multiple allowed tags.
- Keep metric descriptions and base units stable.

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

The autoconfigure module provides:

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

Current endpoints:

- `GET /test/token-ledger/smoke`: app is running and starter is on classpath.
- `GET /test/token-ledger/beans`: reports whether expected autoconfigure beans exist.
- `GET /actuator/prometheus`: validates actuator/prometheus exposure.

Near-term E2E endpoints:

- `GET /test/token-ledger/record`: records a deterministic token usage event through `LedgerManager`.
- `GET /test/token-ledger/budget`: exercises budget enabled/limit behavior.
- `GET /test/token-ledger/chat`: exercises the Spring AI `ChatClient` advisor path with a fake/mock provider or documented real provider setup.

The E2E endpoints should make it easy to verify that `/actuator/prometheus` contains token-ledger metrics after a ledger event is recorded.

## Roadmap

1. Sample app E2E verification for direct ledger recording, metrics, budget, and Spring AI advisor flow.
2. Local Maven publishing plus an external consumer fixture that depends on the published starter artifact.
3. Remote Maven repository publishing setup.
4. Gradle dependency cleanup so library modules are not overloaded with app dependencies.
5. Micrometer options object for autoconfigure integration.
6. Budget policy expansion.
7. Streaming usage aggregation and fallback token estimation.

## Known Risks

- Root `build.gradle` currently applies Spring Boot plugin and actuator/prometheus dependencies to every subproject. This is heavy for library modules.
- `core.internal` implementation classes are package-private by design. Cross-module construction should continue through public factory/configuration APIs.
- Micrometer publisher filters tags, but the configuration is still constructor-level and should be wrapped in an options object before autoconfigure integration.
- Sample app currently proves bean registration and Prometheus exposure, but not yet a full Spring AI call E2E.
- Published artifact behavior is not yet verified outside the multi-module repository.

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

### 2026-05-04

- Merged basic autoconfigure implementation for property binding, conditional bean registration, pricing registry wiring, budget-aware advisor creation, and ChatClient customization.
- Confirmed sample app starter smoke endpoints and Prometheus actuator exposure.
- Reframed MVP roadmap around sample app E2E verification and Maven publishing/consumer validation.

### 2026-04-30

- Renamed project guidance from `GEMINI.md` to `AGENTS.md`.
- Added README roadmap for current project gaps.
- Added starter-focused workstream guidance.
- Clarified that `token-ledger-starter` is the current user-entrypoint task while autoconfigure is owned separately.
- Added a README autoconfigure implementation guide covering bean registration, property binding, internal factory options, and test expectations.
- Implemented Micrometer tag whitelist support and metric description/base unit metadata; documented the next options-object step.

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
