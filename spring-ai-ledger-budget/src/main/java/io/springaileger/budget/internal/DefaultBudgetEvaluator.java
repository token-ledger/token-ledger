package io.springaileger.budget.internal;

import io.springaileger.budget.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * BudgetEvaluator의 기본 구현체입니다.
 *
 * 판단 기준:
 * - 50% 이상  → ALLOW (HALF)
 * - 80% 이상  → WARN (WARNING)
 * - 100% 이상 → BLOCK (EXCEEDED, 예외 발생)
 */
public class DefaultBudgetEvaluator implements BudgetEvaluator {

  private final BudgetStateStore store;
  private final BigDecimal monthlyLimit;

  public DefaultBudgetEvaluator(
      BudgetStateStore store,
      BigDecimal monthlyLimit
  ) {
    this.store = store;
    this.monthlyLimit = monthlyLimit;
  }

  @Override
  public BudgetDecision evaluate(
      Map<String, String> tags,
      BigDecimal costAmount
  ) {

    // 현재까지 누적 비용
    BigDecimal accumulated = store.getAccumulatedCost(tags);

    // 이번 호출까지 포함한 비용
    BigDecimal nextUsage = accumulated.add(costAmount);

    // 임계치 계산
    BigDecimal halfThreshold = monthlyLimit.multiply(BigDecimal.valueOf(0.5));
    BigDecimal warnThreshold = monthlyLimit.multiply(BigDecimal.valueOf(0.8));

        /* =====================
           1️⃣ 차단 (100%)
           ===================== */
    if (nextUsage.compareTo(monthlyLimit) >= 0) {

      BudgetDecision decision = new BudgetDecision(
          BudgetState.BLOCK,
          BudgetThreshold.EXCEEDED,
          "월 예산 초과로 AI 호출이 차단되었습니다",
          nextUsage,
          monthlyLimit
      );

      throw new BudgetExceededException(decision);
    }

    // 비용 누적 (차단이 아닌 경우)
    store.addCost(tags, costAmount);

        /* =====================
           2️⃣ 경고 (80%)
           ===================== */
    if (nextUsage.compareTo(warnThreshold) >= 0) {
      return new BudgetDecision(
          BudgetState.WARN,
          BudgetThreshold.WARNING,
          "월 예산의 80%에 도달했습니다",
          nextUsage,
          monthlyLimit
      );
    }

        /* =====================
           3️⃣ 절반 경고 (50%)
           ===================== */
    if (nextUsage.compareTo(halfThreshold) >= 0) {
      return new BudgetDecision(
          BudgetState.ALLOW,
          BudgetThreshold.HALF,
          "월 예산의 50%에 도달했습니다",
          nextUsage,
          monthlyLimit
      );
    }

        /* =====================
           4️⃣ 정상
           ===================== */
    return new BudgetDecision(
        BudgetState.ALLOW,
        BudgetThreshold.NONE,
        "예산 범위 내입니다",
        nextUsage,
        monthlyLimit
    );
  }
}
