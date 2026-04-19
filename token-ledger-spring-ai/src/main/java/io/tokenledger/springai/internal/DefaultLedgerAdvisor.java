package io.tokenledger.springai.internal;

import io.tokenledger.budget.BudgetEvaluator;
import io.tokenledger.budget.BudgetStateStore;
import io.tokenledger.core.*;
import io.tokenledger.core.domain.*;
import io.tokenledger.springai.LedgerAdvisor;
import io.tokenledger.springai.UsageExtractor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 기본 {@link LedgerAdvisor} 구현체.
 * {@link UsageExtractor}를 사용하여 토큰 사용량을 추출하고,
 * 그 결과를 {@link LedgerManager}에 기록하는 핵심 비즈니스 로직을 수행합니다.
 * 또한 {@link BudgetEvaluator}를 통해 예산 초과 여부를 사전에 차단하고,
 * 호출 성공 시 {@link BudgetStateStore}에 비용을 누적합니다.
 */
public class DefaultLedgerAdvisor implements LedgerAdvisor {

    private final LedgerManager ledgerManager;
    private final UsageExtractor usageExtractor;
    private final BudgetEvaluator budgetEvaluator;
    private final BudgetStateStore budgetStateStore;
    private final CostCalculator costCalculator;
    private final PricingRegistry pricingRegistry;

    public DefaultLedgerAdvisor(LedgerManager ledgerManager, UsageExtractor usageExtractor) {
        this(ledgerManager, usageExtractor, null, null, null, null);
    }

    public DefaultLedgerAdvisor(LedgerManager ledgerManager, UsageExtractor usageExtractor,
                                BudgetEvaluator budgetEvaluator, BudgetStateStore budgetStateStore,
                                CostCalculator costCalculator, PricingRegistry pricingRegistry) {
        this.ledgerManager = ledgerManager;
        this.usageExtractor = usageExtractor;
        this.budgetEvaluator = budgetEvaluator;
        this.budgetStateStore = budgetStateStore;
        this.costCalculator = costCalculator;
        this.pricingRegistry = pricingRegistry;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        if (budgetEvaluator != null) {
            Map<String, String> tags = extractTagsFromRequest(request);
            budgetEvaluator.evaluate(tags);
        }
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        TokenUsage usage = usageExtractor.extract(response);
        
        String modelId = extractModelId(response);
        Map<String, String> tags = extractTags(response);

        ledgerManager.record(modelId, usage, tags);

        // 예산 누적 처리
        if (budgetStateStore != null && costCalculator != null && pricingRegistry != null) {
            Optional<PricingPlan> plan = pricingRegistry.getPlan(modelId);
            if (plan.isPresent()) {
                Cost cost = costCalculator.calculate(usage, plan.get());
                budgetStateStore.addCost(tags, cost.value());
            }
        }

        return response;
    }

    private String extractModelId(ChatClientResponse response) {
        if (response.chatResponse() != null && response.chatResponse().getMetadata() != null) {
            String model = response.chatResponse().getMetadata().getModel();
            if (model != null && !model.isBlank()) {
                return model;
            }
        }
        return "unknown-model";
    }

    private Map<String, String> extractTags(ChatClientResponse response) {
        Map<String, String> tags = new HashMap<>();
        
        Map<String, Object> context = response.context();
        if (context != null) {
            context.forEach((k, v) -> {
                if (v instanceof String s) {
                    tags.put(k, s);
                }
            });
        }

        return tags;
    }

    private Map<String, String> extractTagsFromRequest(ChatClientRequest request) {
        Map<String, String> tags = new HashMap<>();
        Map<String, Object> context = request.context();
        if (context != null) {
            context.forEach((k, v) -> {
                if (v instanceof String s) {
                    tags.put(k, s);
                }
            });
        }
        return tags;
    }
}
