package io.tokenledger.core.internal;

import io.tokenledger.core.PricingRegistry;
import io.tokenledger.core.PricingProvider;
import io.tokenledger.core.domain.PricingPlan;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 메모리 기반 가격 정책 저장소 구현체.
 */
class InMemoryPricingRegistry implements PricingRegistry {
    private final Map<String, PricingPlan> plans = new ConcurrentHashMap<>();

    public InMemoryPricingRegistry() {
    }

    public InMemoryPricingRegistry(List<PricingProvider> providers) {
        if (providers != null) {
            providers.stream()
                    .map(PricingProvider::getAllPlans)
                    .flatMap(Collection::stream)
                    .forEach(this::registerPlan);
        }
    }

    @Override
    public Optional<PricingPlan> getPlan(String modelId) {
        return Optional.ofNullable(plans.get(modelId));
    }

    @Override
    public void registerPlan(PricingPlan plan) {
        plans.put(plan.modelId(), plan);
    }
}
