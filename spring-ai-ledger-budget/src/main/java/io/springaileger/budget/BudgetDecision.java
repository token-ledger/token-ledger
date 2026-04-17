package io.springaileger.budget;

import java.math.BigDecimal;

/**
 * BudgetDecision은 예산 평가의 "결과 보고서"이다.
 *
 * 이 객체 하나로 다음 정보를 모두 알 수 있다:
 * - 지금 호출이 허용/경고/차단 중 무엇인지
 * - 왜 그렇게 판단했는지
 * - 현재까지 사용한 금액
 * - 설정된 예산 한도
 */
public record BudgetDecision(

    // ALLOW / WARN / BLOCK 중 하나
    BudgetState state,

    // 사람이 이해할 수 있는 판단 이유 (로그 / 에러 메시지용)
    String reason,

    // 이번 호출까지 포함한 누적 비용
    BigDecimal currentUsage,

    // 설정된 예산 한도 (예: 월 $100)
    BigDecimal limit
) {}