package io.tokenledger.core.internal;

import io.tokenledger.core.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class DefaultLedgerManagerTest {

    private PricingRegistry registry;
    private CostCalculator calculator;
    private CostMetricsPublisher publisher;
    private DefaultLedgerManager manager;

    @BeforeEach
    void setUp() {
        registry = new InMemoryPricingRegistry();
        calculator = new DefaultCostCalculator();
        publisher = Mockito.mock(CostMetricsPublisher.class);
        manager = new DefaultLedgerManager(registry, calculator, publisher);
    }

    @Test
    @DisplayName("모델 정책이 존재할 때 호출 기록 및 비용 계산이 정상적으로 수행되어야 한다")
    void shouldRecordAndCalculateCost() {
        registry.registerPlan(new PricingPlan("gpt-4o", new BigDecimal("5.0"), new BigDecimal("15.0")));
        TokenUsage usage = TokenUsage.from(1000, 1000);

        Cost cost = manager.record("gpt-4o", usage, Map.of());

        assertThat(cost.value()).isEqualByComparingTo("20.000000");
        verify(publisher).publish(eq("gpt-4o"), eq(usage), eq(cost));
    }

    @Test
    @DisplayName("모델 정책이 없을 경우 0원의 비용을 반환하고 메트릭을 발행해야 한다")
    void shouldReturnZeroCostWhenPlanIsMissing() {
        TokenUsage usage = TokenUsage.from(100, 100);

        Cost result = manager.record("unknown-model", usage, Map.of());

        assertThat(result.value()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(publisher).publish(eq("unknown-model"), eq(usage), any(Cost.class));
    }
}
