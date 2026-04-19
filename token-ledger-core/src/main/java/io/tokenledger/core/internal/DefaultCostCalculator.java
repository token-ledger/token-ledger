package io.tokenledger.core.internal;

import io.tokenledger.core.Cost;
import io.tokenledger.core.CostCalculator;
import io.tokenledger.core.PricingPlan;
import io.tokenledger.core.TokenType;
import io.tokenledger.core.TokenUsage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * 기본 비용 계산기 구현체.
 * 각 {@link TokenType} 별 단가를 적용하여 정밀하게 계산합니다.
 * 1K 토큰당 가격 정보를 사용하여 소수점 10자리까지 중간 계산 후 6자리로 최종 반올림합니다.
 */
public class DefaultCostCalculator implements CostCalculator {
    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);

    @Override
    public Cost calculate(TokenUsage usage, PricingPlan plan) {
        BigDecimal totalCostValue = BigDecimal.ZERO;

        // 사용된 모든 토큰 타입에 대해 각각의 단가를 적용하여 합산
        for (Map.Entry<TokenType, Long> entry : usage.tokenCounts().entrySet()) {
            TokenType type = entry.getKey();
            Long count = entry.getValue();

            if (count > 0) {
                BigDecimal rate = plan.getRate(type);
                BigDecimal typeCost = rate.multiply(BigDecimal.valueOf(count))
                        .divide(THOUSAND, 10, RoundingMode.HALF_UP);
                totalCostValue = totalCostValue.add(typeCost);
            }
        }

        return new Cost(totalCostValue, plan.currency());
    }
}
