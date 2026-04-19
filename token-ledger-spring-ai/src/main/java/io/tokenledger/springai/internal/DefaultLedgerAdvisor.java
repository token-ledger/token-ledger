package io.tokenledger.springai.internal;

import io.tokenledger.core.LedgerManager;
import io.tokenledger.core.TokenUsage;
import io.tokenledger.springai.LedgerAdvisor;
import io.tokenledger.springai.UsageExtractor;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;

import java.util.HashMap;
import java.util.Map;

/**
 * 기본 {@link LedgerAdvisor} 구현체.
 * {@link UsageExtractor}를 사용하여 토큰 사용량을 추출하고,
 * 그 결과를 {@link LedgerManager}에 기록하는 핵심 비즈니스 로직을 수행합니다.
 */
public class DefaultLedgerAdvisor implements LedgerAdvisor {

    private final LedgerManager ledgerManager;
    private final UsageExtractor usageExtractor;

    public DefaultLedgerAdvisor(LedgerManager ledgerManager, UsageExtractor usageExtractor) {
        this.ledgerManager = ledgerManager;
        this.usageExtractor = usageExtractor;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        TokenUsage usage = usageExtractor.extract(response);
        
        String modelId = extractModelId(response);
        Map<String, String> tags = extractTags(response);

        ledgerManager.record(modelId, usage, tags);

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
}
