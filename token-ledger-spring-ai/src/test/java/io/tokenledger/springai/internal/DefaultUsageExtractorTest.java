package io.tokenledger.springai.internal;

import io.tokenledger.core.domain.TokenType;
import io.tokenledger.core.domain.TokenUsage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultUsageExtractorTest {

    private final DefaultUsageExtractor extractor = new DefaultUsageExtractor();

    @Test
    @DisplayName("표준 사용량 정보가 포함된 응답에서 토큰 정보를 추출해야 한다")
    void extractStandardUsage() {
        Usage usage = mock(Usage.class);
        when(usage.getPromptTokens()).thenReturn(100);
        when(usage.getCompletionTokens()).thenReturn(200);

        ChatResponseMetadata metadata = ChatResponseMetadata.builder()
                .usage(usage)
                .build();

        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(new org.springframework.ai.chat.messages.AssistantMessage("test"))), metadata);
        ChatClientResponse response = new ChatClientResponse(chatResponse, Map.of());

        TokenUsage result = extractor.extract(response);

        assertThat(result.getCount(TokenType.PROMPT)).isEqualTo(100L);
        assertThat(result.getCount(TokenType.COMPLETION)).isEqualTo(200L);
    }

    @Test
    @DisplayName("메타데이터에 추론 토큰이 포함된 경우 이를 별도로 식별해야 한다")
    void extractReasoningUsage() {
        Usage usage = mock(Usage.class);
        when(usage.getPromptTokens()).thenReturn(100);
        when(usage.getCompletionTokens()).thenReturn(200);

        ChatResponseMetadata metadata = ChatResponseMetadata.builder()
                .usage(usage)
                .keyValue("completion_tokens_details", Map.of("reasoning_tokens", 150))
                .build();

        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(new org.springframework.ai.chat.messages.AssistantMessage("test"))), metadata);
        ChatClientResponse response = new ChatClientResponse(chatResponse, Map.of());

        TokenUsage result = extractor.extract(response);

        assertThat(result.getCount(TokenType.PROMPT)).isEqualTo(100L);
        assertThat(result.getCount(TokenType.COMPLETION)).isEqualTo(200L);
        assertThat(result.getCount(TokenType.REASONING)).isEqualTo(150L);
    }
}
