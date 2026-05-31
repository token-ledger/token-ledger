package io.tokenledger.budget.internal;

import io.tokenledger.budget.BudgetStateStore;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.Map;

/**
 * ✅ Redis 기반 예산 상태 저장소
 *
 * 역할:
 * - tenant별 예산 사용량 저장
 * - 서버 재시작 후에도 데이터 유지
 */
public class RedisBudgetStateStore implements BudgetStateStore {

  private final StringRedisTemplate redisTemplate;

  public RedisBudgetStateStore(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  private String key(Map<String, String> tags) {
    return "budget:usage:" + tags.toString();
  }

  @Override
  public BigDecimal getAccumulatedCost(Map<String, String> tags) {
    String value = redisTemplate.opsForValue().get(key(tags));

    if (value == null) {
      return BigDecimal.ZERO;
    }

    return new BigDecimal(value);
  }

  @Override
  public void addCost(Map<String, String> tags, BigDecimal amount) {

    String key = key(tags);

    BigDecimal current = getAccumulatedCost(tags);
    BigDecimal updated = current.add(amount);

    redisTemplate.opsForValue().set(key, updated.toString());
  }
}