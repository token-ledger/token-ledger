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
        // 동적 태그 생성 (기본 태그 + 사용자 전달 태그)
        Tags commonTags = Tags.of("model", event.modelId());
        if (event.tags() != null) {
            commonTags = commonTags.and(event.tags().entrySet().stream()
                    .map(e -> Tag.of(e.getKey(), e.getValue()))
                    .collect(Collectors.toList()));
        }

        final Tags finalTags = commonTags;

        // 1. 토큰 사용량 분포 및 누적 (DistributionSummary + Counter)
        event.usage().tokenCounts().forEach(((tokenType, count) -> {
            if (count > 0) {
                String typeName = tokenType.name().toLowerCase();
                
                // 분포 요약 (평균, 최대값 등 파악용)
                meterRegistry.summary("ai.token.usage.distribution", finalTags.and("token_type", typeName))
                        .record(count.doubleValue());
                
                // 총계 (누적 사용량 파악용)
                meterRegistry.counter("ai.token.usage.total", finalTags.and("token_type", typeName))
                        .increment(count);
            }
        }));

        // 2. 비용 누적 (Counter)
        meterRegistry.counter("ai.token.cost.total", 
                finalTags.and("currency", event.cost().currency().getCurrencyCode()))
                .increment(event.cost().value().doubleValue());
    }
}
