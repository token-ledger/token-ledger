package io.tokenledger.micrometer.internal;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.tokenledger.core.domain.CostRecordedEvent;
import io.tokenledger.core.LedgerListener;

import java.util.Map;
import java.util.Set;

/**
 * Micrometer 기반의 비용 메트릭 리스너.
 * {@link LedgerListener}를 구현하여 비용 기록 이벤트를 가로채고,
 * 이를 Prometheus 등 모니터링 시스템으로 전송합니다.
 */
public class MicroCostMetricsPublisher implements LedgerListener {

    private final MeterRegistry meterRegistry;
    private final Set<String> allowedTagKeys;

    public MicroCostMetricsPublisher(MeterRegistry meterRegistry) {
        this(meterRegistry, Set.of("tenant_id"));
    }

    public MicroCostMetricsPublisher(MeterRegistry meterRegistry, Set<String> allowedTagKeys) {
        this.meterRegistry = meterRegistry;
        this.allowedTagKeys = Set.copyOf(allowedTagKeys);
    }

    @Override
    public void onRecord(CostRecordedEvent event) {
        Tags commonTags = Tags.of("model", event.modelId())
                .and(allowedTags(event.tags()));

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

    private Tags allowedTags(Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Tags.empty();
        }
        return Tags.of(tags.entrySet().stream()
                .filter(entry -> allowedTagKeys.contains(entry.getKey()))
                .map(entry -> Tag.of(entry.getKey(), entry.getValue()))
                .toList());
    }
}
