package io.tokenledger.springai.internal;

import io.tokenledger.core.TokenType;
import io.tokenledger.core.TokenUsage;
import io.tokenledger.springai.UsageExtractor;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.EnumMap;
import java.util.Map;

/**
 * 기본 {@link UsageExtractor} 구현체.
 * Spring AI의 {@link Usage} 정보를 {@link TokenUsage}로 변환하며,
 * 메타데이터에 포함된 추론(Reasoning) 토큰 등을 식별하여 세분화된 사용량을 추출합니다.
 */
public class DefaultUsageExtractor implements UsageExtractor {

    @Override
    public TokenUsage extract(ChatClientResponse response) {
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse == null || chatResponse.getMetadata().getUsage() == null) {
            return TokenUsage.from(0, 0);
        }

        Usage usage = chatResponse.getMetadata().getUsage();
        Map<TokenType, Long> counts = new EnumMap<>(TokenType.class);

        long prompt = (usage.getPromptTokens() != null) ? usage.getPromptTokens() : 0L;
        long completion = (usage.getCompletionTokens() != null) ? usage.getCompletionTokens() : 0L;
        
        counts.put(TokenType.PROMPT, prompt);
        counts.put(TokenType.COMPLETION, completion);

        ChatResponseMetadata metadata = chatResponse.getMetadata();
        
        Long reasoning = extractReasoningTokens(metadata);
        if (reasoning > 0) {
            counts.put(TokenType.REASONING, reasoning);
        }

        // ChatResponseMetadata에서 Map 정보를 복사하여 TokenUsage 생성
        Map<String, Object> metadataMap = new java.util.HashMap<>();
        metadata.keySet().forEach(key -> metadataMap.put(key, metadata.get(key)));

        return new TokenUsage(counts, metadataMap);
    }

    private Long extractReasoningTokens(ChatResponseMetadata metadata) {
        // metadata는 직접 Map이 아닐 수 있으므로 get 메서드로 개별 접근
        Object details = metadata.get("completion_tokens_details");
        if (details instanceof Map<?, ?> detailsMap) {
            Object reasoning = detailsMap.get("reasoning_tokens");
            if (reasoning instanceof Number num) {
                return num.longValue();
            }
        }
        
        Object directReasoning = metadata.get("reasoning_tokens");
        if (directReasoning instanceof Number num) {
            return num.longValue();
        }

        return 0L;
    }
}
