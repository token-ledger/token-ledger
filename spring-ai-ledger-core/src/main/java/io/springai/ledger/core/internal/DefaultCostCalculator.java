package io.springai.ledger.core.internal;

import io.springai.ledger.core.Cost;
import io.springai.ledger.core.CostCalculator;
import io.springai.ledger.core.PricingPlan;
import io.springai.ledger.core.TokenUsage;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 기본 비용 계산기 구현체.
 * 1K 토큰당 가격 정보를 사용하여 소수점 10자리까지 중간 계산 후 6자리로 최종 반올림합니다.
 */
public class DefaultCostCalculator implements CostCalculator {
    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);

    @Override
    public Cost calculate(TokenUsage usage, PricingPlan plan) {
        BigDecimal promptCost = plan.promptPricePerK()
                .multiply(BigDecimal.valueOf(usage.promptTokens()))
                .divide(THOUSAND, 10, RoundingMode.HALF_UP);

        BigDecimal completionCost = plan.completionPricePerK()
                .multiply(BigDecimal.valueOf(usage.completionTokens()))
                .divide(THOUSAND, 10, RoundingMode.HALF_UP);

        return new Cost(promptCost.add(completionCost), plan.currency());
    }
}
