package io.tokenledger.autoconfigure;

import io.tokenledger.core.domain.PricingPlan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * Token Ledger의 설정을 담당하는 불변 프로퍼티 레코드.
 */
@ConfigurationProperties(prefix = "token-ledger")
public record TokenLedgerProperties(@DefaultValue PricingProperties pricing) {
    /**
     * @param plans 모델별 상세 가격 정책 목록.
     */
    public record PricingProperties(@DefaultValue List<PricingPlanProperties> plans) {
    }

    public List<PricingPlan> toPricingPlans() {
        if (pricing == null || pricing.plans() == null) {
            return List.of();
        }

        return pricing.plans()
                      .stream()
                      .map(PricingPlanProperties::toPricingPlan)
                      .toList();
    }
}
