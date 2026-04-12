package io.springailedger.budget;

public enum BudgetStatus {
  OK,     // 정상 - 예산의 80% 미만 사용
  WARN,   // 경고 - 예산의 80% 이상 사용
  BLOCK   // 차단 - 예산 100% 초과
}