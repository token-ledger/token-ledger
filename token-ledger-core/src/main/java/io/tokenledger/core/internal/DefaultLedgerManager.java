package io.tokenledger.core.internal;

import io.tokenledger.core.*;
import io.tokenledger.core.domain.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 기본 LedgerManager 구현체.
 * 가격 저장소와 계산기를 조율하여 비용을 기록하고, 등록된 리스너들에게 이벤트를 전파합니다.
 */
class DefaultLedgerManager implements LedgerManager {
    private final PricingRegistry pricingRegistry;
    private final CostCalculator costCalculator;
    private final List<LedgerListener> listeners = new CopyOnWriteArrayList<>();

    public DefaultLedgerManager(PricingRegistry pricingRegistry, CostCalculator costCalculator) {
        this(pricingRegistry, costCalculator, List.of());
    }

    public DefaultLedgerManager(PricingRegistry pricingRegistry, CostCalculator costCalculator, List<LedgerListener> initialListeners) {
        this.pricingRegistry = pricingRegistry;
        this.costCalculator = costCalculator;
        if (initialListeners != null) {
            this.listeners.addAll(initialListeners);
        }
    }

    /**
     * 새로운 리스너를 런타임에 등록합니다.
     */
    public void addListener(LedgerListener listener) {
        if (listener != null) {
            this.listeners.add(listener);
        }
    }

    @Override
    public Cost record(String modelId, TokenUsage usage, Map<String, String> tags) {
        Optional<PricingPlan> planOpt = pricingRegistry.getPlan(modelId);

        Cost cost = planOpt
                .map(plan -> costCalculator.calculate(usage, plan))
                .orElse(Cost.zero());

        // 이벤트 발행 (리스너들에게 전파)
        if (!listeners.isEmpty()) {
            CostRecordedEvent event = new CostRecordedEvent(modelId, usage, cost, tags);
            listeners.forEach(listener -> listener.onRecord(event));
        }

        return cost;
    }
}
