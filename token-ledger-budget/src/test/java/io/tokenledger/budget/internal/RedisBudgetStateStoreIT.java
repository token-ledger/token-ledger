package io.tokenledger.budget.internal;

import io.tokenledger.budget.BudgetEvaluator;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.Map;

class RedisBudgetStateStoreIT {

  @Test
  void redis_should_store_and_accumulate_cost() {

    // Redis 연결
    var connectionFactory =
        new LettuceConnectionFactory("localhost", 6379);
    connectionFactory.afterPropertiesSet();

    var redisTemplate = new StringRedisTemplate(connectionFactory);
    redisTemplate.afterPropertiesSet();

    // Redis 기반 저장소
    var store = new RedisBudgetStateStore(redisTemplate);

    // 예산 평가기
    BudgetEvaluator evaluator =
        new DefaultBudgetEvaluator(store, BigDecimal.valueOf(100));

    var tags = Map.of("user", "test");

    // 비용 누적
    store.addCost(tags, BigDecimal.valueOf(30));
    store.addCost(tags, BigDecimal.valueOf(20));
    store.addCost(tags, BigDecimal.valueOf(10));

    var decision = evaluator.evaluate(tags);

    // 출력
    System.out.println("Redis 저장 값: " + decision.currentUsage());

  }
}