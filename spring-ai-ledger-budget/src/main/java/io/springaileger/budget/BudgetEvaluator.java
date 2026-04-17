package io.springaileger.budget;

import java.math.BigDecimal;
import java.util.Map;

public interface BudgetEvaluator {

  /**
   * 이번 AI 호출을 허용할지 판단한다.
   *
   * @param tags  예산 식별용 태그 (예: tenant_id)
   * @param costAmount 이번 호출 예상 비용
   * @return BudgetDecision
   */
  BudgetDecision evaluate(
      Map<String, String> tags,
      BigDecimal costAmount
  );
}