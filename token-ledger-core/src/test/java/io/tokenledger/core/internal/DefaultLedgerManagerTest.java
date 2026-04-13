package io.tokenledger.core.internal;

import io.tokenledger.core.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultLedgerManagerTest {

    private final PricingRegistry registry = new InMemoryPricingRegistry();
    private final CostCalculator calculator = new DefaultCostCalculator();
    private final DefaultLedgerManager manager = new DefaultLedgerManager(registry, calculator);

    @Test
    @DisplayName("모델 정책이 존재할 때 호출 기록 및 비용 계산이 정상적으로 수행되어야 한다")
    void shouldRecordAndCalculateCost() {
        // Given
        registry.registerPlan(new PricingPlan("gpt-4o", new BigDecimal("5.0"), new BigDecimal("15.0")));
        TokenUsage usage = TokenUsage.from(1000, 1000); // 1K each -> 5 + 15 = 20

        // When
        Cost cost = manager.record("gpt-4o", usage, Map.of());

        // Then
        assertThat(cost.value()).isEqualByComparingTo("20.000000");
    }

    @Test
    @DisplayName("모델 정책이 없을 경우 0원의 비용을 반환해야 한다")
    void shouldReturnZeroCostWhenPlanIsMissing() {
        // Given
        TokenUsage usage = TokenUsage.from(100, 100);

        // When
        Cost result = manager.record("unknown-model", usage, Map.of());

        // Then
        assertThat(result.value()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
