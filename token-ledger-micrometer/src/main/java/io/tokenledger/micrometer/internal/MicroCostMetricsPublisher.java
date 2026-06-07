package io.tokenledger.micrometer.internal;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.tokenledger.core.domain.CostRecordedEvent;
import io.tokenledger.core.LedgerListener;
import io.tokenledger.micrometer.MetricsOptions;

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
        this(meterRegistry, MetricsOptions.defaults());
    }

    public MicroCostMetricsPublisher(MeterRegistry meterRegistry, Set<String> allowedTagKeys) {
        this(meterRegistry, MetricsOptions.withAllowedTagKeys(allowedTagKeys));
    }

    public MicroCostMetricsPublisher(MeterRegistry meterRegistry, MetricsOptions options) {
        this.meterRegistry = meterRegistry;
        MetricsOptions resolvedOptions = (options != null) ? options : MetricsOptions.defaults();
        this.allowedTagKeys = resolvedOptions.allowedTagKeys();
    }

    @Override
    public void onRecord(CostRecordedEvent event) {
        Tags commonTags = Tags.of("model", event.modelId())
                .and(allowedTags(event.tags()));

        final Tags finalTags = commonTags;

        event.usage().tokenCounts().forEach(((tokenType, count) -> {
            if (count > 0) {
                String typeName = tokenType.name().toLowerCase();
                DistributionSummary.builder("ai.token.usage.distribution")
                        .description("Distribution of AI token usage per recorded model call")
                        .baseUnit("tokens")
                        .tags(finalTags.and("token_type", typeName))
                        .register(meterRegistry)
                        .record(count.doubleValue());
                Counter.builder("ai.token.usage.total")
                        .description("Total number of AI tokens recorded")
                        .baseUnit("tokens")
                        .tags(finalTags.and("token_type", typeName))
                        .register(meterRegistry)
                        .increment(count);
            }
        }));
        Counter.builder("ai.token.cost.total")
                .description("Total estimated AI token cost")
                .baseUnit("currency")
                .tags(finalTags.and("currency", event.cost().currency().getCurrencyCode()))
                .register(meterRegistry)
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
