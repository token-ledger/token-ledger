package io.tokenledger.core.internal;

import io.tokenledger.core.*;

import java.util.Map;
import java.util.Optional;

/**
 * 기본 LedgerManager 구현체.
 * 가격 저장소와 계산기를 조율하여 비용을 기록하고 관리합니다.
 */
public class DefaultLedgerManager implements LedgerManager {
    private final PricingRegistry pricingRegistry;
    private final CostCalculator costCalculator;

    public DefaultLedgerManager(PricingRegistry pricingRegistry, CostCalculator costCalculator) {
        this.pricingRegistry = pricingRegistry;
        this.costCalculator = costCalculator;
    }

    @Override
    public Cost record(String modelId, TokenUsage usage, Map<String, String> tags) {
        Optional<PricingPlan> planOpt = pricingRegistry.getPlan(modelId);

        Cost cost = planOpt
                .map(plan -> costCalculator.calculate(usage, plan))
                .orElse(Cost.zero());

        return cost;
    }
}
