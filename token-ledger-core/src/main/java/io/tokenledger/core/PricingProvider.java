package io.tokenledger.core;

import io.tokenledger.core.domain.PricingPlan;
import java.util.Collection;

/**
 * AI 모델의 가격 정책 정보를 제공하는 공급자 인터페이스.
 * 로컬 파일(YAML), 데이터베이스, 또는 원격 API로부터 가격 정보를 가져올 수 있습니다.
 */
public interface PricingProvider {
    /**
     * 사용 가능한 모든 가격 정책 목록을 반환합니다.
     * @return 가격 정책 컬렉션
     */
    Collection<PricingPlan> getAllPlans();
}
