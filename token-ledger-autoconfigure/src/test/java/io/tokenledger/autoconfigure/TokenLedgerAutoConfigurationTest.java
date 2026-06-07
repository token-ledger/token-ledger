package io.tokenledger.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.tokenledger.budget.BudgetDecision;
import io.tokenledger.budget.BudgetEvaluator;
import io.tokenledger.budget.BudgetState;
import io.tokenledger.budget.BudgetStateStore;
import io.tokenledger.budget.BudgetThreshold;
import io.tokenledger.core.CostCalculator;
import io.tokenledger.core.LedgerManager;
import io.tokenledger.core.PricingProvider;
import io.tokenledger.core.PricingRegistry;
import io.tokenledger.core.domain.Cost;
import io.tokenledger.core.domain.PricingPlan;
import io.tokenledger.core.domain.TokenUsage;
import io.tokenledger.notification.BudgetNotificationHandler;
import io.tokenledger.notification.BudgetNotificationService;
import io.tokenledger.notification.NotificationStateStore;
import io.tokenledger.springai.LedgerAdvisor;
import io.tokenledger.springai.UsageExtractor;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static io.tokenledger.core.domain.TokenType.COMPLETION;
import static io.tokenledger.core.domain.TokenType.PROMPT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenLedgerAutoConfigurationTest {

    private static final String PREFIX = "token-ledger.pricing.plans[0]";
    private static final String PROP_MODEL_ID = PREFIX + ".model-id";
    private static final String PROP_PROMPT = PREFIX + ".rates.PROMPT";
    private static final String PROP_COMPLETION = PREFIX + ".rates.COMPLETION";
    private static final String PROP_CURRENCY = PREFIX + ".currency";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(TokenLedgerAutoConfiguration.class));

    @Test
    @DisplayName("기본 설정에서 Core 및 Spring AI 빈은 등록되고, Budget 빈은 등록되지 않아야 한다")
    void shouldRegisterDefaultBeans() {
        this.contextRunner.run(context -> {
            assertThat(context).hasSingleBean(PricingProvider.class);
            assertThat(context).hasSingleBean(PricingRegistry.class);
            assertThat(context).hasSingleBean(CostCalculator.class);
            assertThat(context).hasSingleBean(LedgerManager.class);

            assertThat(context).hasSingleBean(UsageExtractor.class);
            assertThat(context).hasSingleBean(LedgerAdvisor.class);
            assertThat(context).hasSingleBean(LedgerChatClientCustomizer.class);

            assertThat(context).doesNotHaveBean(BudgetStateStore.class);
            assertThat(context).doesNotHaveBean(BudgetEvaluator.class);
        });
    }

    @Test
    @DisplayName("설정 값이 없을 경우 빈 목록을 가진 PricingProvider가 생성되어야 한다")
    void shouldRegisterDefaultPricingProviderWhenNoProperties() {
        this.contextRunner.run(context -> {
            assertThat(context).hasSingleBean(PricingProvider.class);
            PricingProvider provider = context.getBean(PricingProvider.class);
            assertThat(provider.getAllPlans()).isEmpty();
        });
    }

    @ParameterizedTest(name = "[{index}] {argumentSetName}")
    @MethodSource("providePricingConfigs")
    @DisplayName("설정 프로퍼티가 PricingProvider 빈과 환경에 정확하게 바인딩되어야 한다")
    void shouldBindPropertiesToEnvironmentAndBean(
        String modelId,
        String promptRate,
        String completionRate,
        String currency
    ) {
        this.contextRunner
            .withPropertyValues(buildProperties(
                modelId,
                promptRate,
                completionRate,
                currency
            ))
            .run(context -> {
                assertThat(context).hasSingleBean(PricingProvider.class);

                var env = context.getEnvironment();
                var plans = context.getBean(PricingProvider.class)
                    .getAllPlans();

                SoftAssertions.assertSoftly(softly -> {
                    softly.assertThat(env.getProperty(PROP_MODEL_ID))
                        .isEqualTo(modelId);
                    softly.assertThat(env.getProperty(PROP_PROMPT))
                        .isEqualTo(promptRate);
                    softly.assertThat(env.getProperty(PROP_COMPLETION))
                        .isEqualTo(completionRate);
                    softly.assertThat(env.getProperty(PROP_CURRENCY))
                        .isEqualTo(currency);

                    softly.assertThat(plans)
                        .hasSize(1);

                    PricingPlan plan = plans.iterator()
                        .next();

                    softly.assertThat(plan.modelId())
                        .isEqualTo(modelId);
                    softly.assertThat(plan.currency()
                            .getCurrencyCode())
                        .isEqualTo(currency);
                    softly.assertThat(plan.getRate(PROMPT))
                        .isEqualByComparingTo(promptRate);
                    softly.assertThat(plan.getRate(COMPLETION))
                        .isEqualByComparingTo(completionRate);
                });
            });
    }

    @Test
    @DisplayName("설정된 가격 정책이 PricingRegistry와 LedgerManager 비용 계산에 연결되어야 한다")
    void shouldRegisterConfiguredPricingPlansInRegistry() {
        this.contextRunner
            .withPropertyValues(buildProperties(
                "gpt-4o",
                "0.005",
                "0.015",
                "USD"
            ))
            .run(context -> {
                PricingRegistry pricingRegistry = context.getBean(PricingRegistry.class);
                LedgerManager ledgerManager = context.getBean(LedgerManager.class);

                var plan = pricingRegistry.getPlan("gpt-4o");
                Cost cost = ledgerManager.record(
                    "gpt-4o",
                    TokenUsage.from(1_000, 2_000),
                    Map.of()
                );

                SoftAssertions.assertSoftly(softly -> {
                    softly.assertThat(plan)
                        .isPresent();
                    softly.assertThat(plan.orElseThrow()
                            .getRate(PROMPT))
                        .isEqualByComparingTo("0.005");
                    softly.assertThat(plan.orElseThrow()
                            .getRate(COMPLETION))
                        .isEqualByComparingTo("0.015");
                    softly.assertThat(cost.value())
                        .isEqualByComparingTo("0.035000");
                    softly.assertThat(cost.currency()
                            .getCurrencyCode())
                        .isEqualTo("USD");
                });
            });
    }

    @Test
    @DisplayName("token-ledger.budget.enabled=true 일 때 Budget 관련 빈이 등록되어야 한다")
    void shouldRegisterBudgetBeansWhenEnabled() {
        this.contextRunner
            .withPropertyValues("token-ledger.budget.enabled=true")
            .run(context -> {
                assertThat(context).hasSingleBean(BudgetStateStore.class);
                assertThat(context).hasSingleBean(BudgetEvaluator.class);
            });
    }

    @Test
    @DisplayName("Budget가 활성화되면 LedgerAdvisor가 BudgetEvaluator를 사용해야 한다")
    void shouldWireBudgetEvaluatorIntoLedgerAdvisorWhenBudgetEnabled() {
        this.contextRunner
            .withUserConfiguration(RecordingBudgetEvaluatorConfiguration.class)
            .withPropertyValues("token-ledger.budget.enabled=true")
            .run(context -> {
                LedgerAdvisor advisor = context.getBean(LedgerAdvisor.class);
                RecordingBudgetEvaluator evaluator = context.getBean(RecordingBudgetEvaluator.class);

                ChatClientRequest request = mock(ChatClientRequest.class);
                when(request.context()).thenReturn(Map.of("tenant_id", "tenant-abc"));

                advisor.before(request, mock(AdvisorChain.class));

                SoftAssertions.assertSoftly(softly -> {
                    softly.assertThat(evaluator.evaluateCalls())
                        .isEqualTo(1);
                    softly.assertThat(evaluator.lastTags())
                        .containsEntry("tenant_id", "tenant-abc");
                });
            });
    }

    @Test
    @DisplayName("token-ledger.budget.enabled=false 일 때 Budget 관련 빈이 등록되지 않아야 한다")
    void shouldNotRegisterBudgetBeansWhenDisabled() {
        this.contextRunner
            .withPropertyValues("token-ledger.budget.enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(BudgetStateStore.class);
                assertThat(context).doesNotHaveBean(BudgetEvaluator.class);
            });
    }

    @Test
    @DisplayName("MeterRegistry가 존재할 때 Micrometer 관련 빈이 등록되어야 한다")
    void shouldRegisterMicrometerBeanWhenMeterRegistryExists() {
        this.contextRunner
            .withUserConfiguration(MeterRegistryConfiguration.class)
            .run(context -> {
                assertThat(context).hasBean("microCostMetricsPublisher");
            });
    }

    @Test
    @DisplayName("token-ledger.metrics.enabled=false 일 때 Micrometer 관련 빈이 등록되지 않아야 한다")
    void shouldNotRegisterMicrometerBeanWhenMetricsDisabled() {
        this.contextRunner
            .withUserConfiguration(MeterRegistryConfiguration.class)
            .withPropertyValues("token-ledger.metrics.enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean("microCostMetricsPublisher");
            });
    }

    @Test
    @DisplayName("사용자 정의 빈이 있으면 자동 설정 빈이 덮어쓰지 않아야 한다")
    void shouldNotOverrideUserDefinedBeans() {
        this.contextRunner
            .withUserConfiguration(UserCustomConfiguration.class)
            .run(context -> {
                assertThat(context).hasSingleBean(PricingRegistry.class);
                assertThat(context.getBean(PricingRegistry.class))
                    .isInstanceOf(UserCustomPricingRegistry.class);
            });
    }

    // notification 비활성화 시 관련 빈이 등록되지 않는지 확인
    @Test
    @DisplayName("token-ledger.notification.enabled=false 일 때 Notification 관련 빈이 등록되지 않아야 한다")
    void shouldNotRegisterNotificationBeansWhenDisabled() {
        this.contextRunner
            .withPropertyValues("token-ledger.notification.enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(NotificationStateStore.class);
                assertThat(context).doesNotHaveBean(BudgetNotificationService.class);
            });
    }

    // notification 활성화 + 사용자 정의 handler 빈 존재 시 서비스가 등록되는지 확인
    @Test
    @DisplayName("notification이 활성화되고 BudgetNotificationHandler 빈이 있을 때 BudgetNotificationService가 등록되어야 한다")
    void shouldRegisterNotificationServiceWhenEnabledAndHandlerExists() {
        this.contextRunner
            .withUserConfiguration(FakeBudgetNotificationHandlerConfiguration.class)
            .withPropertyValues("token-ledger.notification.enabled=true")
            .run(context -> {
                assertThat(context).hasSingleBean(NotificationStateStore.class);
                assertThat(context).hasSingleBean(BudgetNotificationService.class);
            });
    }

    private static Stream<Arguments> providePricingConfigs() {
        return Stream.of(
            argumentSet(
                "OpenAI GPT-4o 표준 설정",
                "gpt-4o",
                "0.005",
                "0.015",
                "USD"),
            argumentSet(
                "Anthropic Claude-3 EUR 설정",
                "claude-3",
                "0.01",
                "0.03",
                "EUR")
        );
    }

    private String[] buildProperties(String modelId, String promptRate, String completionRate, String currency) {
        return new String[]{
            PROP_MODEL_ID + "=" + modelId,
            PROP_PROMPT + "=" + promptRate,
            PROP_COMPLETION + "=" + completionRate,
            PROP_CURRENCY + "=" + currency
        };
    }

    @Configuration(proxyBeanMethods = false)
    static class UserCustomConfiguration {
        @Bean
        public PricingRegistry pricingRegistry() {
            return new UserCustomPricingRegistry();
        }
    }

    static class UserCustomPricingRegistry implements PricingRegistry {
        @Override public void registerPlan(PricingPlan plan) {}
        @Override public Optional<PricingPlan> getPlan(String modelId) { return Optional.empty(); }
    }

    @Configuration(proxyBeanMethods = false)
    static class RecordingBudgetEvaluatorConfiguration {
        @Bean
        public RecordingBudgetEvaluator budgetEvaluator() {
            return new RecordingBudgetEvaluator();
        }
    }

    static class RecordingBudgetEvaluator implements BudgetEvaluator {
        private int evaluateCalls;
        private Map<String, String> lastTags = Map.of();

        @Override
        public BudgetDecision evaluate(Map<String, String> tags) {
            this.evaluateCalls++;
            this.lastTags = tags;
            return new BudgetDecision(BudgetState.ALLOW, BudgetThreshold.NONE, "allowed", BigDecimal.ZERO, BigDecimal.TEN);
        }

        @Override
        public BudgetDecision evaluate(Map<String, String> tags, BigDecimal costAmount) {
            return evaluate(tags);
        }

        int evaluateCalls() {
            return evaluateCalls;
        }

        Map<String, String> lastTags() {
            return lastTags;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class MeterRegistryConfiguration {
        @Bean
        public MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    // 테스트용 no-op handler - 실제 알림 전송 없이 빈 등록 여부만 검증
    @Configuration(proxyBeanMethods = false)
    static class FakeBudgetNotificationHandlerConfiguration {
        @Bean
        public BudgetNotificationHandler budgetNotificationHandler() {
            return event -> {};
        }
    }
}