package io.springai.ledger.core;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * 특정 모델의 가격 정책 정보.
 * 1,000(1K) 토큰당 가격을 기준으로 정의합니다.
 *
 * @param modelId              모델 식별자 (예: gpt-4o, claude-3-5-sonnet)
 * @param promptPricePerK      1K 프롬프트 토큰당 가격
 * @param completionPricePerK  1K 응답 토큰당 가격
 * @param currency             통화 (기본값: USD)
 */
public record PricingPlan(
        String modelId,
        BigDecimal promptPricePerK,
        BigDecimal completionPricePerK,
        Currency currency
) {
    public PricingPlan {
        if (promptPricePerK.compareTo(BigDecimal.ZERO) < 0 ||
            completionPricePerK.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
        if (currency == null) {
            currency = Currency.getInstance("USD");
        }
    }

    public PricingPlan(String modelId, BigDecimal promptPricePerK, BigDecimal completionPricePerK) {
        this(modelId, promptPricePerK, completionPricePerK, Currency.getInstance("USD"));
    }
}
