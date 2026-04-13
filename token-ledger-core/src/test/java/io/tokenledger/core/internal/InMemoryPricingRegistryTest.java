package io.tokenledger.core.internal;

import io.tokenledger.core.PricingPlan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryPricingRegistryTest {

    private final InMemoryPricingRegistry registry = new InMemoryPricingRegistry();

    @Test
    @DisplayName("가격 정책을 등록하고 모델 ID로 조회할 수 있어야 한다")
    void shouldRegisterAndGetPlan() {
        // Given
        PricingPlan plan = new PricingPlan("claude-3",
                new BigDecimal("0.015"), new BigDecimal("0.075"), Currency.getInstance("USD"));

        // When
        registry.registerPlan(plan);
        Optional<PricingPlan> retrieved = registry.getPlan("claude-3");

        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().modelId()).isEqualTo("claude-3");
        assertThat(retrieved.get().promptPricePerK()).isEqualByComparingTo("0.015");
    }

    @Test
    @DisplayName("등록되지 않은 모델 조회 시 빈 Optional을 반환해야 한다")
    void shouldReturnEmptyWhenNotFound() {
        // When
        Optional<PricingPlan> retrieved = registry.getPlan("non-existent");

        // Then
        assertThat(retrieved).isEmpty();
    }
}