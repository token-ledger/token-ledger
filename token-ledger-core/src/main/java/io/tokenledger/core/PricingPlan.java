package io.tokenledger.core;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Currency;
import java.util.EnumMap;
import java.util.Map;

/**
 * 특정 모델의 가격 정책 정보.
 * {@link TokenType} 별로 1,000(1K) 토큰당 가격을 관리합니다.
 *
 * @param modelId  모델 식별자 (예: gpt-4o, claude-3-5-sonnet)
 * @param rates    1K 토큰 타입별 단가 (Map)
 * @param currency 통화 (기본값: USD)
 */
public record PricingPlan(
        String modelId,
        Map<TokenType, BigDecimal> rates,
        Currency currency
) {
    public PricingPlan {
        rates = Collections.unmodifiableMap(new EnumMap<>(rates));
        if (currency == null) {
            currency = Currency.getInstance("USD");
        }
        // 모든 단가는 0 이상이어야 함
        rates.values().forEach(v -> {
            if (v.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Price cannot be negative");
            }
        });
    }

    /**
     * 기본 입력/출력 단가와 통화를 사용하는 {@link PricingPlan}을 생성합니다.
     */
    public PricingPlan(String modelId, BigDecimal promptPricePerK, BigDecimal completionPricePerK, Currency currency) {
        this(modelId, createRates(promptPricePerK, completionPricePerK), currency);
    }

    /**
     * 기본 입력/출력 단가만 사용하는 {@link PricingPlan}을 생성합니다. 기본 통화는 USD입니다.
     */
    public PricingPlan(String modelId, BigDecimal promptPricePerK, BigDecimal completionPricePerK) {
        this(modelId, promptPricePerK, completionPricePerK, Currency.getInstance("USD"));
    }

    private static Map<TokenType, BigDecimal> createRates(BigDecimal prompt, BigDecimal completion) {
        Map<TokenType, BigDecimal> rates = new EnumMap<>(TokenType.class);
        rates.put(TokenType.PROMPT, prompt);
        rates.put(TokenType.COMPLETION, completion);
        return rates;
    }

    /**
     * 기본 입력 단가를 반환합니다. (하위 호환성)
     */
    public BigDecimal promptPricePerK() {
        return getRate(TokenType.PROMPT);
    }

    /**
     * 기본 출력 단가를 반환합니다. (하위 호환성)
     */
    public BigDecimal completionPricePerK() {
        return getRate(TokenType.COMPLETION);
    }

    /**
     * 특정 토큰 타입의 단가를 가져옵니다. 없을 시 계층 구조에 따라 대체값을 반환합니다.
     * REASONING -> COMPLETION
     * CACHED_PROMPT -> PROMPT
     * CACHED_COMPLETION -> COMPLETION
     */
    public BigDecimal getRate(TokenType type) {
        if (rates.containsKey(type)) {
            return rates.get(type);
        }

        // Fallback Logic
        return switch (type) {
            case REASONING, CACHED_COMPLETION -> rates.getOrDefault(TokenType.COMPLETION, BigDecimal.ZERO);
            case CACHED_PROMPT -> rates.getOrDefault(TokenType.PROMPT, BigDecimal.ZERO);
            default -> rates.getOrDefault(type, BigDecimal.ZERO);
        };
    }
}
