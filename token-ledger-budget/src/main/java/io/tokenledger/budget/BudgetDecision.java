package io.tokenledger.budget;

import java.math.BigDecimal;


/**
 * 예산 평가 결과를 나타내는 값 객체입니다.
 * <p>
 * 호출 가능 여부와
 * 판단에 필요한 최소한의 정보를 담습니다.
 */

public record BudgetDecision(
    BudgetState state,
    String reason,
    BigDecimal currentUsage,
    BigDecimal limit
) {}
