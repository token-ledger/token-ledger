package io.tokenledger.micrometer.internal;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.tokenledger.core.domain.CostRecordedEvent;
import io.tokenledger.core.LedgerListener;

import java.util.stream.Collectors;

/**
 * Micrometer 기반의 비용 메트릭 리스너.
 * {@link LedgerListener}를 구현하여 비용 기록 이벤트를 가로채고,
 * 이를 Prometheus 등 모니터링 시스템으로 전송합니다.
 */
public class MicroCostMetricsPublisher implements LedgerListener {

    private final MeterRegistry meterRegistry;

    public MicroCostMetricsPublisher(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void onRecord(CostRecordedEvent event) {
        Tags commonTags = Tags.of("model", event.modelId());
        if (event.tags() != null) {
            commonTags = commonTags.and(event.tags().entrySet().stream()
                    .map(e -> Tag.of(e.getKey(), e.getValue()))
                    .collect(Collectors.toList()));
        }

        final Tags finalTags = commonTags;

        event.usage().tokenCounts().forEach(((tokenType, count) -> {
            if (count > 0) {
                String typeName = tokenType.name().toLowerCase();
                meterRegistry.summary("ai.token.usage.distribution", finalTags.and("token_type", typeName))
                        .record(count.doubleValue());
                meterRegistry.counter("ai.token.usage.total", finalTags.and("token_type", typeName))
                        .increment(count);
            }
        }));
        meterRegistry.counter("ai.token.cost.total", 
                finalTags.and("currency", event.cost().currency().getCurrencyCode()))
                .increment(event.cost().value().doubleValue());
    }
}
