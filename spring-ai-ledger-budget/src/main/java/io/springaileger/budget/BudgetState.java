package io.springaileger.budget;

/**
 * BudgetState는 "이번 AI 호출을 허용할지 말지"에 대한 최종 판단 결과이다.
 *
 * - ALLOW : 예산에 여유가 있어서 정상 호출 가능
 * - WARN  : 예산이 거의 찼지만, 이번 호출은 허용
 * - BLOCK : 예산을 초과했기 때문에 호출을 차단
 */

public enum BudgetState {
  ALLOW,
  WARN,
  BLOCK
}