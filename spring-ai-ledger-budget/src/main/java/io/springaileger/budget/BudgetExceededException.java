package io.springaileger.budget;
/**
 * BudgetExceededException은 "LLM 호출을 멈추기 위해" 던지는 예외이다.
 *
 * 이 예외가 던져지는 순간:
 * - Spring AI 호출 체인이 중단된다
 * - 실제 LLM API 요청은 나가지 않는다
 *
 * 즉, 이것이 바로 비용 기반 Circuit Breaker 역할을 한다.
 */
public class BudgetExceededException extends RuntimeException {

  // 어떤 판단으로 차단되었는지 담고 있음
  private final BudgetDecision decision;

  public BudgetExceededException(BudgetDecision decision) {
    // Exception 메시지로 reason을 사용
    super(decision.reason());
    this.decision = decision;
  }

  public BudgetDecision getDecision() {
    return decision;
  }
}