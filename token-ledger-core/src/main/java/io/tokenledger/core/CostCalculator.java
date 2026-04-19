package io.tokenledger.core;

/**
 * 사용량과 가격 정책을 바탕으로 비용을 계산하는 인터페이스.
 */
public interface CostCalculator {
    /**
     * 토큰 사용량과 가격 정책을 기반으로 비용을 산출합니다.
     * @param usage 토큰 사용량
     * @param plan  가격 정책
     * @return 산출된 비용
     */
    Cost calculate(TokenUsage usage, PricingPlan plan);
}
