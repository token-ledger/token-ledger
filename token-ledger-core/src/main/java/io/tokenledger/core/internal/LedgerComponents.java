package io.tokenledger.core.internal;

import io.tokenledger.core.CostCalculator;
import io.tokenledger.core.LedgerListener;
import io.tokenledger.core.LedgerManager;
import io.tokenledger.core.PricingRegistry;

import java.util.List;

/**
 * Token Ledger 코어 컴포넌트 생성을 위한 팩토리 클래스입니다.
 */
public final class LedgerComponents {

    private LedgerComponents() {
    }

    public static CostCalculator defaultCostCalculator() {
        return new DefaultCostCalculator();
    }

    public static PricingRegistry inMemoryPricingRegistry() {
        return new InMemoryPricingRegistry();
    }

    public static LedgerManager defaultLedgerManager(
            PricingRegistry pricingRegistry,
            CostCalculator costCalculator,
            List<LedgerListener> listeners
    ) {
        return new DefaultLedgerManager(pricingRegistry, costCalculator, listeners);
    }
}
