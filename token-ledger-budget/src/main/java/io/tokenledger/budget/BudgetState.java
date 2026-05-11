package io.tokenledger.budget;

/**
 * 예산 평가 결과 상태를 나타냅니다.
 * <p>
 * ALLOW : 호출 허용
 * WARN  : 예산 경고
 * BLOCK : 호출 차단
 */
public enum BudgetState {
  ALLOW,
  WARN,
  BLOCK
}
