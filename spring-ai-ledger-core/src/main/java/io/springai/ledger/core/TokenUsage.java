package io.springai.ledger.core;

import java.util.Collections;
import java.util.Map;

/**
 * AI 모델 호출 시 발생하는 토큰 사용량 정보.
 *
 * @param promptTokens     프롬프트(입력) 토큰 수
 * @param completionTokens 응답(출력) 토큰 수
 * @param totalTokens      전체 사용 토큰 수
 * @param metadata         추가 메타데이터 (예: cached tokens, reasoning tokens)
 */
public record TokenUsage(
        long promptTokens,
        long completionTokens,
        long totalTokens,
        Map<String, Object> metadata
) {
    public TokenUsage {
        metadata = (metadata != null) ? Collections.unmodifiableMap(metadata) : Map.of();
        if (promptTokens < 0 || completionTokens < 0 || totalTokens < 0) {
            throw new IllegalArgumentException("Tokens cannot be negative");
        }
    }

    public static TokenUsage from(long prompt, long completion) {
        return new TokenUsage(prompt, completion, prompt + completion, Map.of());
    }

    public static TokenUsage from(long prompt, long completion, Map<String, Object> metadata) {
        return new TokenUsage(prompt, completion, prompt + completion, metadata);
    }
}
