package io.tokenledger.core;

/**
 * AI 모델 호출 시 발생하는 토큰의 세부 유형.
 */
public enum TokenType {
    /** 일반 입력 (Prompt) */
    PROMPT,
    /** 일반 출력 (Completion) */
    COMPLETION,
    /** 추론 (Reasoning) - 주로 출력 계열 */
    REASONING,
    /** 캐시된 입력 (Cached Prompt) */
    CACHED_PROMPT,
    /** 캐시된 출력 (Cached Completion) */
    CACHED_COMPLETION;

    /**
     * 해당 토큰 타입이 입력(Prompt) 계열인지 확인합니다.
     */
    public boolean isPrompt() {
        return this == PROMPT || this == CACHED_PROMPT;
    }

    /**
     * 해당 토큰 타입이 출력(Completion) 계열인지 확인합니다.
     */
    public boolean isCompletion() {
        return this == COMPLETION || this == REASONING || this == CACHED_COMPLETION;
    }
}
