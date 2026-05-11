package io.springaileger.budget;

/**
 * 예산 사용률에 따른 임계치 단계입니다.
 */
public enum BudgetThreshold {

  NONE,       // 임계치 미도달
  HALF,       // 50%
  WARNING,    // 80%
  EXCEEDED    // 100%
}