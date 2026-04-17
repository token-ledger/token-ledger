package io.springaileger.budget;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * InMemoryBudgetStateStore는
 * 메모리 안에서 예산 사용량을 누적하는 가장 단순한 구현체이다.
 *
 * ✅ 대학 프로젝트 / MVP에 매우 적합
 * ❌ 서버 재시작 시 데이터는 사라짐 (실서비스면 Redis/DB 사용)
 */
public class InMemoryBudgetStateStore implements BudgetStateStore {

  /**
   * key : 예산 식별자 (예: tenant_id)
   * value : 지금까지 사용한 총 비용
   */
  private final Map<String, BigDecimal> store = new ConcurrentHashMap<>();

  /**
   * tags에서 어떤 값을 기준으로 예산을 나눌지 결정한다.
   *
   * 여기서는 tenant_id 기준으로 예산을 관리한다.
   */
  private String key(Map<String, String> tags) {
    return tags.getOrDefault("tenant_id", "default");
  }

  @Override
  public BigDecimal getAccumulatedCost(Map<String, String> tags) {
    // 아직 사용 기록이 없으면 0원
    return store.getOrDefault(key(tags), BigDecimal.ZERO);
  }

  @Override
  public void addCost(Map<String, String> tags, BigDecimal amount) {
    // 기존 값에 amount를 더함
    store.merge(key(tags), amount, BigDecimal::add);
  }
}