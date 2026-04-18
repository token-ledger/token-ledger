package io.springaileger.budget;

import java.math.BigDecimal;
import java.util.Map;


/**
 * 예산 판단을 위해 비용 누적 상태를 관리하는 저장소 인터페이스입니다.
 *
 * 예산 식별자별(예: tenant)로 비용을 조회하고
 * 누적하는 책임을 가집니다.
 */

public interface BudgetStateStore {

  BigDecimal getAccumulatedCost(Map<String, String> tags);

  void addCost(Map<String, String> tags, BigDecimal amount);
}