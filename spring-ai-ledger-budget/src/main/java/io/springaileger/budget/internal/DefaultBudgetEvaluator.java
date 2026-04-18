package io.springaileger.budget.internal;

import io.springaileger.budget.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * BudgetEvaluator의 기본 구현체입니다.
 *
 * 판단 기준:
 * - 80% 미만  → ALLOW
 * - 80% 이상  → WARN
 * - 100% 이상 → BLOCK (예외 발생)
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

    // ✅ 현재까지 누적 비용
    BigDecimal accumulated = store.getAccumulatedCost(tags);

    // ✅ 이번 호출까지 포함한 비용
    BigDecimal nextUsage = accumulated.add(costAmount);

    // ✅ 경고 기준 (80%)
    BigDecimal warnThreshold =
        monthlyLimit.multiply(new BigDecimal("0.8"));

    /* =====================
       1️⃣ 차단 (BLOCK)
       ===================== */
    if (nextUsage.compareTo(monthlyLimit) >= 0) {

      BudgetDecision decision = new BudgetDecision(
          BudgetState.BLOCK,
          "월 예산 초과로 AI 호출이 차단되었습니다",
          nextUsage,
          monthlyLimit
      );

      throw new BudgetExceededException(decision);
    }

    /* =====================
       2️⃣ 경고 (WARN)
       ===================== */
    if (nextUsage.compareTo(warnThreshold) >= 0) {

      store.addCost(tags, costAmount);

      return new BudgetDecision(
          BudgetState.WARN,
          "월 예산의 80%에 도달했습니다",
          nextUsage,
          monthlyLimit
      );
    }

    /* =====================
       3️⃣ 허용 (ALLOW)
       ===================== */
    store.addCost(tags, costAmount);

    return new BudgetDecision(
        BudgetState.ALLOW,
        "예산 범위 내입니다",
        nextUsage,
        monthlyLimit
    );
  }
}