package io.tokenledger.micrometer.internal;

import io.micrometer.core.instrument.MeterRegistry;
import io.tokenledger.core.Cost;
import io.tokenledger.core.TokenUsage;
import io.tokenledger.core.CostMetricsPublisher;

public class MicroCostMetricsPublisher implements CostMetricsPublisher {

    private final MeterRegistry meterRegistry;

    public MicroCostMetricsPublisher(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void publish(String model, TokenUsage usage, Cost cost) {
        usage.tokenCounts().forEach(((tokenType, count) ->{
            if(count>0){
                meterRegistry.counter("ai.token.usage.total",
                        "model",model,
                        "token_type",tokenType.name().toLowerCase()).increment(count);
            }
        }));
        meterRegistry.counter("ai.token.cost.total",
                "model",model,
                "currency", cost.currency().getCurrencyCode()).increment(cost.value().doubleValue());
    }
}
