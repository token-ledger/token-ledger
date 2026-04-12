package io.springailedger.budget;

public class BudgetEvaluator {

  private final BudgetAccumulator accumulator;

  public BudgetEvaluator(BudgetAccumulator accumulator) {
    this.accumulator = accumulator;
  }

  public BudgetStatus evaluate(BudgetPolicy policy, double costUsd) {

    accumulator.add(policy.getServiceId(), costUsd);

    double total = accumulator.getAccumulated(policy.getServiceId());

    double usagePercent = (total / policy.getDailyLimitUsd()) * 100.0;

    if (usagePercent >= 100.0) {
      return BudgetStatus.BLOCK;
    } else if (usagePercent >= policy.getWarningThresholdPercent()) {
      return BudgetStatus.WARN;
    } else {
      return BudgetStatus.OK;
    }
  }
}