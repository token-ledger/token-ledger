package io.tokenledger.springai;

import io.tokenledger.core.TokenUsage;
import org.springframework.ai.chat.client.ChatClientResponse;

/**
 * AI 응답({@link ChatClientResponse})에서 토큰 사용량 정보를 추출하는 인터페이스.
 */
public interface UsageExtractor {
    TokenUsage extract(ChatClientResponse response);
}
