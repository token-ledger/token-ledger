package io.tokenledger.springai.internal;

import io.tokenledger.budget.BudgetEvaluator;
import io.tokenledger.budget.BudgetStateStore;
import io.tokenledger.core.CostCalculator;
import io.tokenledger.core.LedgerManager;
import io.tokenledger.core.PricingRegistry;
import io.tokenledger.springai.LedgerAdvisor;
import io.tokenledger.springai.UsageExtractor;

/**
 * Spring AI 어댑터 컴포넌트 생성을 위한 팩토리 클래스입니다.
 */
public final class LedgerSpringAiComponents {

    private LedgerSpringAiComponents() {
    }

    public static UsageExtractor defaultUsageExtractor() {
        return new DefaultUsageExtractor();
    }

    public static LedgerAdvisor defaultLedgerAdvisor(
            LedgerManager ledgerManager,
            UsageExtractor usageExtractor
    ) {
        return new DefaultLedgerAdvisor(ledgerManager, usageExtractor);
    }

    public static LedgerAdvisor defaultLedgerAdvisor(
            LedgerManager ledgerManager,
            UsageExtractor usageExtractor,
            BudgetEvaluator budgetEvaluator,
            BudgetStateStore budgetStateStore,
            CostCalculator costCalculator,
            PricingRegistry pricingRegistry
    ) {
        return new DefaultLedgerAdvisor(
                ledgerManager,
                usageExtractor,
                budgetEvaluator,
                budgetStateStore,
                costCalculator,
                pricingRegistry
        );
    }
}
