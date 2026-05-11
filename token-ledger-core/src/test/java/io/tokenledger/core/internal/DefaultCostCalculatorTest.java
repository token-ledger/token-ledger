package io.tokenledger.core.internal;

import io.tokenledger.core.domain.Cost;
import io.tokenledger.core.domain.PricingPlan;
import io.tokenledger.core.domain.TokenType;
import io.tokenledger.core.domain.TokenUsage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultCostCalculatorTest {

    private final DefaultCostCalculator calculator = new DefaultCostCalculator();

    @Test
    @DisplayName("기본 입력/출력 토큰 비용이 정확하게 계산되어야 한다")
    void calculateStandardTokens() {
        // Given: Input $0.01/1k, Output $0.03/1k
        PricingPlan plan = new PricingPlan("gpt-4o", new BigDecimal("0.01"), new BigDecimal("0.03"));
        // Usage: Input 1000, Output 2000
        TokenUsage usage = TokenUsage.from(1000, 2000);

        // When
        Cost cost = calculator.calculate(usage, plan);

        // Then: (1000 * 0.01 / 1000) + (2000 * 0.03 / 1000) = 0.01 + 0.06 = 0.07
        assertThat(cost.value()).isEqualByComparingTo("0.070000");
    }

    @Test
    @DisplayName("추론 토큰 요율이 다를 때 각각 정밀하게 계산되어야 한다")
    void calculateReasoningTokensWithDifferentRate() {
        // Given: Prompt $0.015, Completion $0.060, Reasoning $0.045
        Map<TokenType, BigDecimal> rates = new EnumMap<>(TokenType.class);
        rates.put(TokenType.PROMPT, new BigDecimal("0.015"));
        rates.put(TokenType.COMPLETION, new BigDecimal("0.060"));
        rates.put(TokenType.REASONING, new BigDecimal("0.045"));
        PricingPlan plan = new PricingPlan("o1-preview", rates, Cost.DEFAULT_CURRENCY);

        // Usage: Prompt 1000, Completion 500, Reasoning 1500 (Total 3000)
        Map<TokenType, Long> counts = new EnumMap<>(TokenType.class);
        counts.put(TokenType.PROMPT, 1000L);
        counts.put(TokenType.COMPLETION, 500L);
        counts.put(TokenType.REASONING, 1500L);
        TokenUsage usage = new TokenUsage(counts, Map.of());

        // When
        Cost cost = calculator.calculate(usage, plan);

        // Then
        // 1000 * 0.015 / 1000 = 0.015
        //  500 * 0.060 / 1000 = 0.030
        // 1500 * 0.045 / 1000 = 0.0675
        // Total = 0.015 + 0.030 + 0.0675 = 0.1125
        assertThat(cost.value()).isEqualByComparingTo("0.112500");
    }

    @Test
    @DisplayName("추론 토큰 요율이 명시되지 않으면 일반 출력 요율을 따라야 한다")
    void calculateReasoningTokensWithFallback() {
        // Given: Prompt $0.01, Completion $0.03 (Reasoning 없음)
        PricingPlan plan = new PricingPlan("gpt-4o", new BigDecimal("0.01"), new BigDecimal("0.03"));
        // Usage: Prompt 1000, Completion 0, Reasoning 1000
        TokenUsage usage = TokenUsage.from(1000, 0, 1000);

        // When
        Cost cost = calculator.calculate(usage, plan);

        // Then: 1000 * 0.01 / 1000 + 1000 * 0.03 / 1000 = 0.04
        assertThat(cost.value()).isEqualByComparingTo("0.040000");
    }
}
