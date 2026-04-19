package io.tokenledger.budget;

import java.math.BigDecimal;
import java.util.Map;


/**
 * AI 호출 전 예산 초과 여부를 판단하는 인터페이스입니다.
 * <p>
 * 구현체는 현재까지 누적된 비용과
 * 이번 호출로 발생할 비용을 기준으로
 * 호출을 허용하거나 차단하는 역할을 합니다.
 */
public interface BudgetEvaluator {

  /**
   * 단순히 현재의 누적 비용이 예산 한도를 초과했는지만 판단합니다. (부수 효과 없음)
   */
  BudgetDecision evaluate(Map<String, String> tags);

  /**
   * 이번 호출로 발생할 예상 비용을 포함하여 예산 초과 여부를 판단합니다. (부수 효과 없음)
   */
  BudgetDecision evaluate(
      Map<String, String> tags,
      BigDecimal costAmount
  );
}
