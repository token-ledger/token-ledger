package io.tokenledger.autoconfigure;

import io.tokenledger.core.domain.PricingPlan;
import io.tokenledger.core.domain.TokenType;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Map;
import java.util.Objects;

/**
 * 개별 AI 모델에 대한 가격 설정 정보.
 *
 * @param modelId  AI 모델 식별자 (예: gpt-4o, Claude-3-5-sonnet). 이 값은 ChatResponse의 model_id와 매칭되어야 합니다.
 * @param rates    토큰 타입별 1,000(1K) 토큰당 가격 설정. (PROMPT, COMPLETION 등)
 * @param currency 가격 정산에 사용할 통화 단위 (기본값: USD).
 */
public record PricingPlanProperties(
        String modelId,
        Map<TokenType, BigDecimal> rates,
        String currency
) {

    public PricingPlanProperties {
        rates = Objects.requireNonNullElse(rates, Map.of());
        currency = Objects.requireNonNullElse(currency, "USD");
    }

    public PricingPlan toPricingPlan() {
        return new PricingPlan(
                modelId,
                rates,
                Currency.getInstance(currency)
        );
    }
}
