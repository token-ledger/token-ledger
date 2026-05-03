package io.tokenledger.budget.internal;

import io.tokenledger.budget.BudgetEvaluator;
import io.tokenledger.budget.BudgetStateStore;

/**
 * 예산 제어 컴포넌트 생성을 위한 팩토리 클래스입니다.
 */
public final class LedgerBudgetComponents {

    private LedgerBudgetComponents() {
    }

    public static BudgetStateStore inMemoryBudgetStateStore() {
        return new InMemoryBudgetStateStore();
    }

    public static BudgetEvaluator defaultBudgetEvaluator(BudgetStateStore store, java.math.BigDecimal monthlyLimit) {
        return new DefaultBudgetEvaluator(store, monthlyLimit);
    }
}
