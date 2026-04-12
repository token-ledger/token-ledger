package io.springailedger.budget;

public class BudgetPolicy {

  private final String serviceId;
  private final double dailyLimitUsd;
  private final double warningThresholdPercent;

  public BudgetPolicy(String serviceId,
                      double dailyLimitUsd,
                      double warningThresholdPercent) {
    this.serviceId = serviceId;
    this.dailyLimitUsd = dailyLimitUsd;
    this.warningThresholdPercent = warningThresholdPercent;
  }

  public String getServiceId() { return serviceId; }
  public double getDailyLimitUsd() { return dailyLimitUsd; }
  public double getWarningThresholdPercent() { return warningThresholdPercent; }
}