package io.tokenledger.core;

import io.tokenledger.core.domain.PricingPlan;
import java.util.Collection;

/**
 * 가격 정책 공급자 인터페이스.
 * 'StaticPricingProvider(YAML)'와 'RemotePricingProvider(API)'의 공통 부모.
 */
public interface PricingProvider {
    /**
     * @return 공급자가 관리하는 모든 가격 정책 목록
     */
    Collection<PricingPlan> getAllPlans();
}
