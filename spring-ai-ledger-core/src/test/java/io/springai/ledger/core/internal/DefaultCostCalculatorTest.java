package io.springai.ledger.core.internal;

import io.springai.ledger.core.Cost;
import io.springai.ledger.core.PricingPlan;
import io.springai.ledger.core.TokenUsage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultCostCalculatorTest {

    private final DefaultCostCalculator calculator = new DefaultCostCalculator();

    @Test
    @DisplayName("1,000 토큰당 단가를 기준으로 정확한 비용을 계산해야 한다")
    void shouldCalculateCorrectCost() {
        // Given: prompt $0.01/1K, completion $0.03/1K
        PricingPlan plan = new PricingPlan("gpt-4",
                new BigDecimal("0.01"), new BigDecimal("0.03"), Currency.getInstance("USD"));

        // 500 prompt, 500 completion -> (0.005 + 0.015) = 0.02
        TokenUsage usage = TokenUsage.from(500, 500);

        // When
        Cost result = calculator.calculate(usage, plan);

        // Then
        assertThat(result.value()).isEqualByComparingTo("0.020000");
        assertThat(result.currency().getCurrencyCode()).isEqualTo("USD");
    }
}
