package io.tokenledger.autoconfigure;

import io.tokenledger.core.PricingProvider;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

class TokenLedgerAutoConfigurationPricingTest {
    private static final String PREFIX = "token-ledger.pricing.plans[0]";
    private static final String PROP_MODEL_ID = PREFIX + ".model-id";
    private static final String PROP_PROMPT = PREFIX + ".rates.PROMPT";
    private static final String PROP_COMPLETION = PREFIX + ".rates.COMPLETION";
    private static final String PROP_CURRENCY = PREFIX + ".currency";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TokenLedgerAutoConfiguration.class));

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

                    SoftAssertions.assertSoftly(softly -> {
                        softly.assertThat(env.getProperty(PROP_MODEL_ID))
                              .isEqualTo(modelId);

                        softly.assertThat(env.getProperty(PROP_PROMPT))
                              .isEqualTo(promptRate);

                        softly.assertThat(env.getProperty(PROP_COMPLETION))
                              .isEqualTo(completionRate);

                        softly.assertThat(env.getProperty(PROP_CURRENCY))
                              .isEqualTo(currency);
                    });
                });
    }

    private String[] buildProperties(String modelId, String promptRate, String completionRate, String currency) {
        return new String[]{
                PROP_MODEL_ID + "=" + modelId,
                PROP_PROMPT + "=" + promptRate,
                PROP_COMPLETION + "=" + completionRate,
                PROP_CURRENCY + "=" + currency
        };
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
}
