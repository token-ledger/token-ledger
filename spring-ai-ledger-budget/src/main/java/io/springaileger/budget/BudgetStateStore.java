package io.springaileger.budget;

import java.math.BigDecimal;
import java.util.Map;

/**
 * BudgetStateStore는 "지금까지 얼마를 썼는지"를 관리하는 역할을 한다.
 *
 * 예:
 * - tenant A가 이번 달에 얼마를 썼는지
 * - user B가 오늘 얼마를 썼는지
 *
 * MVP에서는 InMemory 구현만으로 충분하다.
 */
public interface BudgetStateStore {

  /**
   * 현재까지 누적된 비용을 반환한다.
   */
  BigDecimal getAccumulatedCost(Map<String, String> tags);

  /**
   * 새로운 비용을 누적한다.
   */
  void addCost(Map<String, String> tags, BigDecimal amount);
}