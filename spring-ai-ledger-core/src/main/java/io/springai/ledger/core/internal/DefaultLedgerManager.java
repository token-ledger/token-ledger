package io.springai.ledger.core.internal;

import io.springai.ledger.core.*;

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
    public Cost record(String modelName, TokenUsage usage, Map<String, String> tags) {
        Optional<PricingPlan> planOpt = pricingRegistry.getPlan(modelName);

        Cost cost = planOpt
                .map(plan -> costCalculator.calculate(usage, plan))
                .orElse(Cost.zero());

        // Note: 향후 여기서 Micrometer 메트릭 발행 및 Budget 체크 로직이 호출될 예정입니다.
        return cost;
    }
}
