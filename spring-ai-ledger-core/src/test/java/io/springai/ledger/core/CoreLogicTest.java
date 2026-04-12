package io.springai.ledger.core;

import io.springai.ledger.core.internal.DefaultCostCalculator;
import io.springai.ledger.core.internal.DefaultLedgerManager;
import io.springai.ledger.core.internal.InMemoryPricingRegistry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CoreLogicTest {

    @Test
    void testCostCalculation() {
        // Given
        PricingRegistry registry = new InMemoryPricingRegistry();
        CostCalculator calculator = new DefaultCostCalculator();
        LedgerManager manager = new DefaultLedgerManager(registry, calculator);

        String model = "gpt-4o";
        // prompt: $5.00 / 1M, completion: $15.00 / 1M
        registry.registerPlan(new PricingPlan(model, BigDecimal.valueOf(5.00), BigDecimal.valueOf(15.00)));

        TokenUsage usage = TokenUsage.from(1000, 2000); // 1000 prompt, 2000 completion

        // When
        Cost cost = manager.record(model, usage, Map.of());

        // Then
        // prompt cost = 1000 * 5 / 1,000,000 = 0.005000
        // completion cost = 2000 * 15 / 1,000,000 = 0.030000
        // total = 0.035000
        assertThat(cost.value()).isEqualByComparingTo("0.035000");
    }

    @Test
    void testMissingPlan() {
        // Given
        PricingRegistry registry = new InMemoryPricingRegistry();
        CostCalculator calculator = new DefaultCostCalculator();
        LedgerManager manager = new DefaultLedgerManager(registry, calculator);

        TokenUsage usage = TokenUsage.from(1000, 2000);

        // When
        Cost cost = manager.record("unknown-model", usage, Map.of());

        // Then
        assertThat(cost.value()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
