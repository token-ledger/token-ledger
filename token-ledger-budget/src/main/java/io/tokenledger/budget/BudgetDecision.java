package io.tokenledger.budget;

import java.math.BigDecimal;

/**
 *  예산 평가 결과를 나타내는 객체
 *
 * - state: ALLOW / WARN / BLOCK 상태
 * - threshold: 현재 도달한 예산 임계치
 * - reason: 상태 설명
 * - currentUsage: 현재 사용량
 * - limit: 총 예산
 */
public record BudgetDecision(
    BudgetState state,
    BudgetThreshold threshold,
    String reason,
    BigDecimal currentUsage,
    BigDecimal limit
) {}
