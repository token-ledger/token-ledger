package io.tokenledger.springai.internal;

import io.tokenledger.budget.BudgetDecision;
import io.tokenledger.budget.BudgetEvaluator;
import io.tokenledger.budget.BudgetState;
import io.tokenledger.budget.BudgetStateStore;
import io.tokenledger.core.*;
import io.tokenledger.core.domain.*;
import io.tokenledger.springai.LedgerAdvisor;
import io.tokenledger.springai.UsageExtractor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DefaultLedgerAdvisorTest {

    @Test
    @DisplayName("AI 응답 후 사용량, 모델ID, 태그가 올바르게 LedgerManager에 기록되어야 한다")
    void recordAfterAIResponse() {
        LedgerManager ledgerManager = mock(LedgerManager.class);
        UsageExtractor extractor = mock(UsageExtractor.class);
        TokenUsage mockUsage = TokenUsage.from(100, 200);
        when(extractor.extract(any())).thenReturn(mockUsage);

        DefaultLedgerAdvisor advisor = new DefaultLedgerAdvisor(ledgerManager, extractor);

        ChatResponseMetadata metadata = ChatResponseMetadata.builder()
                .model("gpt-4o")
                .build();
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(new org.springframework.ai.chat.messages.AssistantMessage("test"))), metadata);
        Map<String, Object> context = Map.of("user_id", "user-123", "tenant_id", "tenant-abc");
        ChatClientResponse response = new ChatClientResponse(chatResponse, context);

        advisor.after(response, mock(AdvisorChain.class));

        verify(ledgerManager, times(1)).record(
                eq("gpt-4o"),
                eq(mockUsage),
                argThat(tags -> tags.get("user_id").equals("user-123") && 
                               tags.get("tenant_id").equals("tenant-abc"))
        );
    }

    @Test
    @DisplayName("AI 응답 후 비용이 계산되어 BudgetStateStore에 누적되어야 한다")
    void recordBudgetAfterAIResponse() {
        LedgerManager ledgerManager = mock(LedgerManager.class);
        UsageExtractor extractor = mock(UsageExtractor.class);
        BudgetStateStore budgetStateStore = mock(BudgetStateStore.class);
        CostCalculator costCalculator = mock(CostCalculator.class);
        PricingRegistry pricingRegistry = mock(PricingRegistry.class);

        TokenUsage mockUsage = TokenUsage.from(100, 200);
        PricingPlan mockPlan = new PricingPlan("gpt-4o", new BigDecimal("0.01"), new BigDecimal("0.03"), Currency.getInstance("USD"));
        Cost mockCost = new Cost(new BigDecimal("0.5"), Currency.getInstance("USD"));

        when(extractor.extract(any())).thenReturn(mockUsage);
        when(pricingRegistry.getPlan("gpt-4o")).thenReturn(Optional.of(mockPlan));
        when(costCalculator.calculate(mockUsage, mockPlan)).thenReturn(mockCost);

        DefaultLedgerAdvisor advisor = new DefaultLedgerAdvisor(ledgerManager, extractor, 
                null, budgetStateStore, costCalculator, pricingRegistry);

        ChatResponseMetadata metadata = ChatResponseMetadata.builder().model("gpt-4o").build();
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(new org.springframework.ai.chat.messages.AssistantMessage("test"))), metadata);
        Map<String, Object> context = Map.of("tenant_id", "tenant-abc");
        ChatClientResponse response = new ChatClientResponse(chatResponse, context);

        advisor.after(response, mock(AdvisorChain.class));

        verify(budgetStateStore, times(1)).addCost(
                argThat(tags -> tags.get("tenant_id").equals("tenant-abc")),
                argThat(amount -> amount.compareTo(new BigDecimal("0.5")) == 0)
        );
    }

    @Test
    @DisplayName("AI 호출 전 BudgetEvaluator를 통해 예산을 체크해야 한다")
    void checkBudgetBeforeAIRequest() {
        BudgetEvaluator budgetEvaluator = mock(BudgetEvaluator.class);
        DefaultLedgerAdvisor advisor = new DefaultLedgerAdvisor(mock(LedgerManager.class), mock(UsageExtractor.class),
                budgetEvaluator, null, null, null);

        ChatClientRequest request = mock(ChatClientRequest.class);
        Map<String, Object> context = Map.of("tenant_id", "tenant-abc");
        when(request.context()).thenReturn(context);

        advisor.before(request, mock(AdvisorChain.class));

        verify(budgetEvaluator, times(1)).evaluate(
                argThat(tags -> tags.get("tenant_id").equals("tenant-abc"))
        );
    }

    @Test
    @DisplayName("Advisor 이름과 순서가 기본값으로 설정되어야 한다")
    void checkAdvisorMetadata() {
        DefaultLedgerAdvisor advisor = new DefaultLedgerAdvisor(mock(LedgerManager.class), mock(UsageExtractor.class));

        assertThat(advisor.getName()).isEqualTo("LedgerAdvisor");
        assertThat(advisor.getOrder()).isEqualTo(0);
    }
}
