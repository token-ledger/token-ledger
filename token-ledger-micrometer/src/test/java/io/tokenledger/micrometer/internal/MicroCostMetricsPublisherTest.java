package io.tokenledger.micrometer.internal;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.tokenledger.core.Cost;
import io.tokenledger.core.TokenUsage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MicroCostMetricsPublisherTest {

    private SimpleMeterRegistry registry;
    private MicroCostMetricsPublisher publisher;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        publisher = new MicroCostMetricsPublisher(registry);
    }

    @Test
    @DisplayName("비용과 사용량 정보가 주어지면 Micrometer 메트릭이 정상적으로 발행되어야 한다")
    void shouldPublishMetricsWithCorrectTags() {
        // Given
        String model = "gpt-4o";
        TokenUsage usage = TokenUsage.from(100, 200, 50); // Prompt 100, Completion 200, Reasoning 50
        Cost cost = Cost.of(new BigDecimal("0.75"));

        // When
        publisher.publish(model, usage, cost);

        // Then
        // 1. 토큰 사용량 메트릭 검증 (Prompt)
        assertThat(registry.get("ai.token.usage.total")
                .tag("model", model)
                .tag("token_type", "prompt")
                .counter().count()).isEqualTo(100.0);

        // 2. 토큰 사용량 메트릭 검증 (Reasoning)
        assertThat(registry.get("ai.token.usage.total")
                .tag("model", model)
                .tag("token_type", "reasoning")
                .counter().count()).isEqualTo(50.0);

        // 3. 비용 메트릭 검증
        assertThat(registry.get("ai.token.cost.total")
                .tag("model", model)
                .tag("currency", "USD")
                .counter().count()).isEqualTo(0.75);
    }

    @Test
    @DisplayName("사용량이 0인 토큰 타입은 메트릭을 발행하지 않아야 한다")
    void shouldNotPublishMetricsForZeroUsage() {
        // Given
        String model = "gpt-3.5-turbo";
        TokenUsage usage = TokenUsage.from(100, 0); // Completion이 0인 경우
        Cost cost = Cost.of(new BigDecimal("0.1"));

        // When
        publisher.publish(model, usage, cost);

        // Then
        // Prompt는 존재해야 함
        assertThat(registry.find("ai.token.usage.total")
                .tag("token_type", "prompt")
                .counter()).isNotNull();

        // Completion은 존재하지 않아야 함 (검색 시 null)
        assertThat(registry.find("ai.token.usage.total")
                .tag("token_type", "completion")
                .counter()).isNull();
    }
}
