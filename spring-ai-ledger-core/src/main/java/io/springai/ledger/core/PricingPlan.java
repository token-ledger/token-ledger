package io.springai.ledger.core;

import java.math.BigDecimal;

/**
 * 특정 모델의 가격 정책 정보.
 * 1,000,000(1M) 토큰당 가격을 기준으로 정의합니다.
 *
 * @param modelName            모델 식별자 (예: gpt-4o, claude-3-5-sonnet)
 * @param promptPricePer1M     1M 프롬프트 토큰당 가격
 * @param completionPricePer1M 1M 응답 토큰당 가격
 */
public record PricingPlan(
        String modelName,
        BigDecimal promptPricePer1M,
        BigDecimal completionPricePer1M
) {
    public PricingPlan {
        if (promptPricePer1M.compareTo(BigDecimal.ZERO) < 0 ||
            completionPricePer1M.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
    }
}
