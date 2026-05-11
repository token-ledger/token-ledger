package io.tokenledger.sample;

import io.tokenledger.budget.BudgetDecision;
import io.tokenledger.budget.BudgetEvaluator;
import io.tokenledger.budget.BudgetStateStore;
import io.tokenledger.budget.exception.BudgetExceededException;
import io.tokenledger.core.LedgerManager;
import io.tokenledger.core.domain.Cost;
import io.tokenledger.core.domain.TokenUsage;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SampleController {

    private final ApplicationContext applicationContext;
    private final LedgerManager ledgerManager;
    private final ObjectProvider<BudgetEvaluator> budgetEvaluator;
    private final ObjectProvider<BudgetStateStore> budgetStateStore;

    public SampleController(ApplicationContext applicationContext,
                            LedgerManager ledgerManager,
                            ObjectProvider<BudgetEvaluator> budgetEvaluator,
                            ObjectProvider<BudgetStateStore> budgetStateStore) {
        this.applicationContext = applicationContext;
        this.ledgerManager = ledgerManager;
        this.budgetEvaluator = budgetEvaluator;
        this.budgetStateStore = budgetStateStore;
    }

    @GetMapping("/test/ai")
    public String testAiLogic() {
        return "AI 장부 분석 테스트 성공! (이 접속이 그라파나에 기록됩니다.)";
    }

    @GetMapping("/test/token-ledger/smoke")
    public Map<String, String> smoke() {
        return Map.of(
                "status", "ok",
                "starter", "token-ledger-starter"
        );
    }

    @GetMapping("/test/token-ledger/beans")
    public Map<String, Boolean> tokenLedgerBeans() {
        return Map.of(
                "ledgerManager", applicationContext.containsBean("ledgerManager"),
                "ledgerAdvisor", applicationContext.containsBean("ledgerAdvisor"),
                "pricingRegistry", applicationContext.containsBean("pricingRegistry"),
                "microCostMetricsPublisher", applicationContext.containsBean("microCostMetricsPublisher"),
                "budgetEvaluator", applicationContext.containsBean("budgetEvaluator"),
                "budgetStateStore", applicationContext.containsBean("budgetStateStore")
        );
    }

    @GetMapping("/test/token-ledger/record")
    public Map<String, String> recordTokenLedgerEvent() {
        Cost cost = ledgerManager.record(
                "gpt-4o-mini",
                TokenUsage.from(1_000, 2_000),
                Map.of("tenant_id", "sample-tenant", "user_id", "sample-user")
        );

        return Map.of(
                "modelId", "gpt-4o-mini",
                "cost", cost.value().toPlainString(),
                "currency", cost.currency().getCurrencyCode()
        );
    }

    @GetMapping("/test/token-ledger/budget")
    public Map<String, String> budget() {
        BudgetEvaluator evaluator = budgetEvaluator.getIfAvailable();
        BudgetStateStore stateStore = budgetStateStore.getIfAvailable();

        if (evaluator == null || stateStore == null) {
            return Map.of("enabled", "false");
        }

        Map<String, String> tags = Map.of("tenant_id", "budget-sample-tenant");
        BudgetDecision initialDecision = evaluator.evaluate(tags, new BigDecimal("0.001"));
        stateStore.addCost(tags, new BigDecimal("0.0045"));

        try {
            evaluator.evaluate(tags, new BigDecimal("0.001"));
            return Map.of(
                    "enabled", "true",
                    "initialState", initialDecision.state().name(),
                    "blockedState", "NONE"
            );
        } catch (BudgetExceededException exception) {
            BudgetDecision blockedDecision = exception.getDecision();
            return Map.of(
                    "enabled", "true",
                    "initialState", initialDecision.state().name(),
                    "blockedState", blockedDecision.state().name(),
                    "currentUsage", blockedDecision.currentUsage().toPlainString(),
                    "limit", blockedDecision.limit().toPlainString()
            );
        }
    }

}
