package io.tokenledger.micrometer.internal;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.tokenledger.core.*;
import io.tokenledger.core.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MicroCostMetricsPublisherTest {

    private MeterRegistry meterRegistry;
    private MicroCostMetricsPublisher publisher;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        publisher = new MicroCostMetricsPublisher(meterRegistry);
    }

    @Test
    @DisplayName("비용 기록 이벤트 발생 시 토큰 사용량과 비용 메트릭이 올바르게 발행되어야 한다")
    void shouldPublishMetricsWhenEventRecorded() {
        // Given
        TokenUsage usage = TokenUsage.from(100, 200);
        Cost cost = new Cost(new BigDecimal("0.5"), Currency.getInstance("USD"));
        Map<String, String> tags = Map.of("tenant_id", "tenant-1");
        CostRecordedEvent event = new CostRecordedEvent("gpt-4o", usage, cost, tags);

        // When
        publisher.onRecord(event);

        // Then: 토큰 사용량 카운터 확인
        assertThat(meterRegistry.find("ai.token.usage.total")
                .tag("model", "gpt-4o")
                .tag("token_type", "prompt")
                .tag("tenant_id", "tenant-1")
                .counter().count()).isEqualTo(100.0);

        assertThat(meterRegistry.find("ai.token.usage.total")
                .tag("model", "gpt-4o")
                .tag("token_type", "completion")
                .tag("tenant_id", "tenant-1")
                .counter().count()).isEqualTo(200.0);

        // Then: 토큰 사용량 분포(Summary) 확인
        assertThat(meterRegistry.find("ai.token.usage.distribution")
                .tag("model", "gpt-4o")
                .tag("token_type", "prompt")
                .summary().max()).isEqualTo(100.0);

        // Then: 비용 카운터 확인
        assertThat(meterRegistry.find("ai.token.cost.total")
                .tag("model", "gpt-4o")
                .tag("currency", "USD")
                .counter().count()).isEqualTo(0.5);
    }
}
