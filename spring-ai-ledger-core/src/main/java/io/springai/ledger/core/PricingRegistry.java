package io.springai.ledger.core;

import java.util.Optional;

/**
 * AI 모델별 가격 정책을 관리하는 저장소 인터페이스.
 */
public interface PricingRegistry {
    /**
     * 모델 이름으로 등록된 가격 정책을 조회합니다.
     * @param modelName 모델 이름
     * @return 가격 정책 (존재하지 않을 경우 empty)
     */
    Optional<PricingPlan> getPlan(String modelName);

    /**
     * 새로운 가격 정책을 등록하거나 업데이트합니다.
     * @param plan 가격 정책
     */
    void registerPlan(PricingPlan plan);
}
