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
- Commit messages must be written in Korean unless the user explicitly requests another language.

## Architecture

| Layer | Modules | Responsibility |
| --- | --- | --- |
| API & Domain | `token-ledger-core` | Core models, pricing, cost calculation interfaces, ledger interfaces |
| Adapter | `token-ledger-spring-ai`, `token-ledger-micrometer`, `token-ledger-budget` | Integrate with Spring AI, Micrometer, and budget policy |
| Infrastructure | `token-ledger-autoconfigure`, `token-ledger-starter` | Spring Boot auto-configuration and final user dependency |
| Demo | `token-ledger-sample-app`, `external-consumer-fixture` | Local verification app for starter/autoconfigure integration and published artifact consumption |

## Module Status

| Module | Status | Notes |
| --- | --- | --- |
| `token-ledger-core` | Basic implementation complete | Domain records, pricing, calculator, registry, ledger manager |
| `token-ledger-spring-ai` | Basic implementation complete | `UsageExtractor`, `LedgerAdvisor`, response usage recording |
| `token-ledger-micrometer` | Basic implementation complete | Tag whitelist and metric metadata implemented; options object is next |
| `token-ledger-budget` | Basic implementation complete | Needs richer policy/window/store support |
| `token-ledger-autoconfigure` | Basic implementation complete | Bean registration, property binding, pricing/budget wiring, and ChatClient customizer implemented |
| `token-ledger-starter` | Basic implementation complete | Thin final user entrypoint that brings runtime modules together |
| `token-ledger-sample-app` | Basic E2E complete | Direct ledger metrics, budget, and fake Spring AI advisor E2E implemented |
| `external-consumer-fixture` | Basic implementation complete | Verification module that consumes the published starter from `mavenLocal` |

## Current Work Focus

The current MVP workstream is packaging and external consumer validation.

MVP tasks:

- Keep sample app dependent on `project(':token-ledger-starter')`.
- Keep local Maven publishing and the external consumer fixture healthy as the default artifact verification path.
- Validate GitHub Packages snapshot publishing before public release.
- Keep published POM metadata aligned with Maven Central promotion requirements.

Autoconfigure basic implementation has landed. Future autoconfigure work should be incremental hardening rather than first implementation.

Gradle dependency cleanup has landed. Library modules should not regain app-only Spring Boot plugin, actuator, or Prometheus dependencies from the root build.

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
    implementation 'cloud.token-ledger:token-ledger-starter'
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

## Autoconfigure Implementation Notes

Autoconfigure is responsible for wiring the starter dependency graph into a Spring Boot application. It should not implement provider API calls, sample-app-only beans, or production Redis/JDBC budget stores.

Bean registration principles:

- Use `@ConditionalOnMissingBean` so user beans win over defaults.
- Use `@ConditionalOnClass` for optional adapter integrations.
- Use `@ConditionalOnProperty` for feature flags under `token-ledger.*`.
- Register beans by public interface type whenever possible.
- Keep `token-ledger-starter` free of business logic and bean creation.

Default bean graph:

| Bean | Condition | Purpose |
| --- | --- | --- |
| `CostCalculator` | missing bean | Core cost calculation |
| `PricingRegistry` | missing bean | Pricing plan lookup |
| `LedgerManager` | missing bean | Cost and usage recording |
| `UsageExtractor` | Spring AI classpath + missing bean | Spring AI response usage extraction |
| `LedgerAdvisor` | Spring AI classpath + missing bean | ChatClient advisor |
| `MicroCostMetricsPublisher` | Micrometer classpath + `token-ledger.metrics.enabled` | Cost/token metrics listener |
| `BudgetStateStore` | `token-ledger.budget.enabled` + missing bean | Default in-memory budget state |
| `BudgetEvaluator` | `token-ledger.budget.enabled` + missing bean | Default budget evaluator |
| `ChatClientCustomizer` | Spring AI classpath + `LedgerAdvisor` bean | Adds advisor to ChatClient builders |

`core.internal` implementation classes should remain package-private. Cross-module construction should go through `LedgerComponents` or another deliberate public factory/API. Do not make internal implementation classes public just to satisfy autoconfigure access.

Autoconfigure tests should use `ApplicationContextRunner` and verify:

- context starts with default settings
- pricing properties flow into `PricingProvider`, `PricingRegistry`, and `LedgerManager` cost calculation
- user-defined beans are not overridden
- Micrometer publisher registers only when `MeterRegistry` is available and metrics are enabled
- budget beans do not register by default
- budget beans register when budget is enabled
- budget-enabled `LedgerAdvisor` calls `BudgetEvaluator`
- Spring AI classpath registers `UsageExtractor`, `LedgerAdvisor`, and `ChatClientCustomizer`

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
- `GET /test/token-ledger/record`: records a deterministic token usage event through `LedgerManager`.
- `GET /test/token-ledger/budget`: exercises budget enabled/limit behavior when budget beans are present.
- `GET /actuator/prometheus`: validates actuator/prometheus exposure.

Test-only E2E endpoint:

- `GET /test/token-ledger/chat`: exercises the Spring AI `ChatClient` advisor path with a fake/mock provider or documented real provider setup.

The direct ledger E2E test verifies that `/actuator/prometheus` contains token-ledger metrics after a ledger event is recorded. The fake ChatClient E2E test verifies that Spring AI `ChatClient` calls flow through `LedgerAdvisor` into token-ledger metrics without requiring a real provider API key.

## Maven Publishing Direction

MVP publishing should proceed in this order:

1. Add Gradle `maven-publish` configuration.
2. Confirm artifact ids, versions, generated POM metadata, and runtime dependency scopes.
3. Run `publishToMavenLocal`.
4. Create or maintain an external consumer verification module that depends on the published artifact coordinates.
5. Verify the consumer can use only `implementation 'cloud.token-ledger:token-ledger-starter:0.0.1-SNAPSHOT'`.
6. Publish snapshots to GitHub Packages.
7. Document consumer credentials and CI publish flow before public release.
8. Add signing and staging automation before Maven Central promotion.

## Roadmap

1. GitHub Packages snapshot publishing flow and CI credentials setup.
2. Maven Central signing and staging flow.
3. Real provider Spring AI smoke verification behind an opt-in profile.
4. Micrometer options object for autoconfigure integration.
5. Budget policy expansion.
6. Streaming usage aggregation and fallback token estimation.

## Known Risks

- `core.internal` implementation classes are package-private by design. Cross-module construction should continue through public factory/configuration APIs.
- Micrometer publisher filters tags, but the configuration is still constructor-level and should be wrapped in an options object before autoconfigure integration.
- Sample app E2E uses a fake Spring AI `ChatModel`; real provider API behavior is not yet verified.
- Public remote repository consumption is not yet verified outside local Maven and GitHub Packages snapshot flow.

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

Verify the published starter from the external consumer module:

```bash
./gradlew publishToMavenLocal
./gradlew :external-consumer-fixture:bootRun -PusePublishedStarter=true
curl http://localhost:8081/test/token-ledger/published
```

Publish snapshots to GitHub Packages:

```bash
./gradlew publish \
  -PmavenRepoUrl=https://maven.pkg.github.com/token-ledger/token-ledger \
  -PmavenRepoUsername="$GITHUB_ACTOR" \
  -PmavenRepoPassword="$GITHUB_TOKEN"
```

## Update History

### 2026-05-11

- Added Gradle `maven-publish` configuration for library modules with shared POM metadata and optional remote repository credentials.
- Added `external-consumer-fixture` as a repository-managed verification module that depends on published `cloud.token-ledger:token-ledger-starter:0.0.1-SNAPSHOT` from `mavenLocal()`.
- Chose GitHub Packages as the first remote snapshot repository target and documented the publish command in `README.md`.
- Added GitHub Packages consumer examples and expanded published POM metadata for later Maven Central promotion.
- Switched `external-consumer-fixture` to use `project(':token-ledger-starter')` by default and require `-PusePublishedStarter=true` for published artifact verification so CI builds do not fail before publish.

### 2026-05-04

- Merged basic autoconfigure implementation for property binding, conditional bean registration, pricing registry wiring, budget-aware advisor creation, and ChatClient customization.
- Added sample app direct ledger, budget, and fake Spring AI ChatClient E2E verification for starter endpoints, `LedgerManager.record(...)`, `LedgerAdvisor`, Micrometer listener wiring, Prometheus `ai.token.*` metrics, and budget block behavior.
- Cleaned Gradle dependencies so app-only Spring Boot plugin, actuator, and Prometheus dependencies are scoped to the sample app instead of every library module.
- Reframed MVP roadmap around Maven publishing and external consumer validation.

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
