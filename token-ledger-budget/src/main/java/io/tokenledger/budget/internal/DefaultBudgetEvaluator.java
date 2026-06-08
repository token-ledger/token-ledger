package io.tokenledger.budget.internal;

import io.tokenledger.budget.BudgetDecision;
import io.tokenledger.budget.BudgetEvaluator;
import io.tokenledger.budget.BudgetState;
import io.tokenledger.budget.BudgetStateStore;
import io.tokenledger.budget.BudgetThreshold;
import io.tokenledger.budget.exception.BudgetExceededException;

import java.math.BigDecimal;
import java.util.Map;

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

    BigDecimal currentUsage = store.getAccumulatedCost(tags);
    BigDecimal newUsage = currentUsage.add(costAmount);

    BigDecimal halfThreshold = monthlyLimit.multiply(BigDecimal.valueOf(0.5));
    BigDecimal warningThreshold = monthlyLimit.multiply(BigDecimal.valueOf(0.8));

    if (newUsage.compareTo(monthlyLimit) >= 0) {
      BudgetDecision decision = new BudgetDecision(
          BudgetState.BLOCK,
          BudgetThreshold.EXCEEDED,
          "월 예산을 초과했습니다",
          newUsage,
          monthlyLimit
      );
      throw new BudgetExceededException(decision);
    }

    // ✅ 80% 이상
    if (newUsage.compareTo(warningThreshold) >= 0) {
      return new BudgetDecision(
          BudgetState.WARN,
          BudgetThreshold.WARNING,
          "월 예산의 80% 이상 사용",
          newUsage,
          monthlyLimit
      );
    }

    // ✅ 50% 이상
    if (newUsage.compareTo(halfThreshold) >= 0) {
      return new BudgetDecision(
          BudgetState.ALLOW,
          BudgetThreshold.HALF,
          "월 예산의 50% 이상 사용",
          newUsage,
          monthlyLimit
      );
    }

    // ✅ 정상
    return new BudgetDecision(
        BudgetState.ALLOW,
        BudgetThreshold.NONE,
        "정상 범위 사용",
        newUsage,
        monthlyLimit
    );
  }

  @Override
  public BudgetDecision evaluate(Map<String, String> tags) {

    BigDecimal usage = store.getAccumulatedCost(tags);

    BigDecimal halfThreshold = monthlyLimit.multiply(BigDecimal.valueOf(0.5));
    BigDecimal warningThreshold = monthlyLimit.multiply(BigDecimal.valueOf(0.8));

    if (usage.compareTo(monthlyLimit) >= 0) {
      return new BudgetDecision(
          BudgetState.BLOCK,
          BudgetThreshold.EXCEEDED,
          "월 예산을 초과했습니다",
          usage,
          monthlyLimit
      );
    }

    if (usage.compareTo(warningThreshold) >= 0) {
      return new BudgetDecision(
          BudgetState.WARN,
          BudgetThreshold.WARNING,
          "월 예산의 80% 이상 사용",
          usage,
          monthlyLimit
      );
    }

    if (usage.compareTo(halfThreshold) >= 0) {
      return new BudgetDecision(
          BudgetState.ALLOW,
          BudgetThreshold.HALF,
          "월 예산의 50% 이상 사용",
          usage,
          monthlyLimit
      );
    }

    return new BudgetDecision(
        BudgetState.ALLOW,
        BudgetThreshold.NONE,
        "정상 범위 사용",
        usage,
        monthlyLimit
    );
  }
}
