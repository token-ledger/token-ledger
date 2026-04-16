package io.tokenledger.core;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * AI 모델 호출 시 발생하는 토큰 사용량 정보.
 * 세부적인 {@link TokenType} 별로 사용량을 관리합니다.
 *
 * @param tokenCounts 토큰 타입별 사용량 (Map)
 * @param metadata    추가 메타데이터 (예: 모델 정보 등)
 */
public record TokenUsage(
        Map<TokenType, Long> tokenCounts,
        Map<String, Object> metadata
) {
    public TokenUsage {
        tokenCounts = Collections.unmodifiableMap(new EnumMap<>(tokenCounts));
        metadata = (metadata != null) ? Collections.unmodifiableMap(metadata) : Map.of();
    }

    /**
     * 기본 입력/출력 토큰을 사용하는 {@link TokenUsage}를 생성합니다.
     */
    public static TokenUsage from(long prompt, long completion) {
        Map<TokenType, Long> counts = new EnumMap<>(TokenType.class);
        counts.put(TokenType.PROMPT, prompt);
        counts.put(TokenType.COMPLETION, completion);
        return new TokenUsage(counts, Map.of());
    }

    /**
     * 입력/출력/추론 토큰을 포함하는 {@link TokenUsage}를 생성합니다.
     */
    public static TokenUsage from(long prompt, long completion, long reasoning) {
        Map<TokenType, Long> counts = new EnumMap<>(TokenType.class);
        counts.put(TokenType.PROMPT, prompt);
        counts.put(TokenType.COMPLETION, completion);
        counts.put(TokenType.REASONING, reasoning);
        return new TokenUsage(counts, Map.of());
    }

    /**
     * 모든 종류의 입력/출력 토큰 수의 합계를 반환합니다.
     */
    public long promptTokens() {
        return tokenCounts.entrySet().stream()
                .filter(e -> e.getKey().isPrompt())
                .mapToLong(Map.Entry::getValue)
                .sum();
    }

    /**
     * 모든 종류의 출력(추론 포함) 토큰 수의 합계를 반환합니다.
     */
    public long completionTokens() {
        return tokenCounts.entrySet().stream()
                .filter(e -> e.getKey().isCompletion())
                .mapToLong(Map.Entry::getValue)
                .sum();
    }

    /**
     * 전체 사용 토큰 수의 합계를 반환합니다.
     */
    public long totalTokens() {
        return tokenCounts.values().stream().mapToLong(Long::longValue).sum();
    }

    /**
     * 특정 토큰 타입의 사용량을 가져옵니다. 없을 시 0을 반환합니다.
     */
    public long getCount(TokenType type) {
        return tokenCounts.getOrDefault(type, 0L);
    }
}
