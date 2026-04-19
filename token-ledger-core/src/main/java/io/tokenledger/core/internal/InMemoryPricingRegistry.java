package io.tokenledger.core.internal;

import io.tokenledger.core.PricingPlan;
import io.tokenledger.core.PricingRegistry;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 메모리 기반 가격 정책 저장소 구현체.
 */
public class InMemoryPricingRegistry implements PricingRegistry {
    private final Map<String, PricingPlan> plans = new ConcurrentHashMap<>();

    @Override
    public Optional<PricingPlan> getPlan(String modelId) {
        return Optional.ofNullable(plans.get(modelId));
    }

    @Override
    public void registerPlan(PricingPlan plan) {
        plans.put(plan.modelId(), plan);
    }
}
