package io.springaileger.budget;

import java.math.BigDecimal;
import java.util.Map;


/**
 * AI 호출 전 예산 초과 여부를 판단하는 인터페이스입니다.
 *
 * 구현체는 현재까지 누적된 비용과
 * 이번 호출로 발생할 비용을 기준으로
 * 호출을 허용하거나 차단하는 역할을 합니다.
 */


public interface BudgetEvaluator {

  BudgetDecision evaluate(
      Map<String, String> tags,
      BigDecimal costAmount
  );
}