package io.springai.ledger.core;

/**
 * AI 모델 호출 시 발생하는 토큰 사용량 정보.
 *
 * @param promptTokens     프롬프트(입력) 토큰 수
 * @param completionTokens 응답(출력) 토큰 수
 * @param totalTokens      전체 사용 토큰 수
 */
public record TokenUsage(
        long promptTokens,
        long completionTokens,
        long totalTokens
) {
    public TokenUsage {
        if (promptTokens < 0 || completionTokens < 0 || totalTokens < 0) {
            throw new IllegalArgumentException("Tokens cannot be negative");
        }
    }

    public static TokenUsage from(long prompt, long completion) {
        return new TokenUsage(prompt, completion, prompt + completion);
    }
}
