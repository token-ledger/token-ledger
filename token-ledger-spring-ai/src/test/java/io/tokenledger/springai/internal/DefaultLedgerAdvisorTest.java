package io.tokenledger.springai.internal;

import io.tokenledger.core.LedgerManager;
import io.tokenledger.core.TokenUsage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DefaultLedgerAdvisorTest {

    @Test
    @DisplayName("AI 응답 후 사용량, 모델ID, 태그가 올바르게 LedgerManager에 기록되어야 한다")
    void recordAfterAIResponse() {
        LedgerManager ledgerManager = mock(LedgerManager.class);
        DefaultUsageExtractor extractor = mock(DefaultUsageExtractor.class);
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
    @DisplayName("Advisor 이름과 순서가 기본값으로 설정되어야 한다")
    void checkAdvisorMetadata() {
        DefaultLedgerAdvisor advisor = new DefaultLedgerAdvisor(mock(LedgerManager.class), mock(DefaultUsageExtractor.class));

        assertThat(advisor.getName()).isEqualTo("LedgerAdvisor");
        assertThat(advisor.getOrder()).isEqualTo(0);
    }
}
