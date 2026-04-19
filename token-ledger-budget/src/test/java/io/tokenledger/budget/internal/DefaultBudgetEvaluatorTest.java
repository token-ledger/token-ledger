package io.tokenledger.budget.internal;

import io.tokenledger.budget.*;
import io.tokenledger.budget.exception.BudgetExceededException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefaultBudgetEvaluatorTest {

  @Test
  void should_return_allow_when_usage_is_below_80_percent() {
    // given
    BudgetStateStore store = new InMemoryBudgetStateStore();
    BudgetEvaluator evaluator =
        new DefaultBudgetEvaluator(store, BigDecimal.valueOf(100));

    Map<String, String> tags = Map.of("tenant_id", "test");

    // when
    BudgetDecision decision =
        evaluator.evaluate(tags, BigDecimal.valueOf(50));

    // then
    assertEquals(BudgetState.ALLOW, decision.state());
  }

  @Test
  void should_return_warn_when_usage_exceeds_80_percent() {
    // given
    BudgetStateStore store = new InMemoryBudgetStateStore();
    BudgetEvaluator evaluator =
        new DefaultBudgetEvaluator(store, BigDecimal.valueOf(100));

    Map<String, String> tags = Map.of("tenant_id", "test");

    // when
    BudgetDecision decision =
        evaluator.evaluate(tags, BigDecimal.valueOf(85));

    // then
    assertEquals(BudgetState.WARN, decision.state());
  }

  @Test
  void should_throw_exception_when_usage_exceeds_limit() {
    // given
    BudgetStateStore store = new InMemoryBudgetStateStore();
    BudgetEvaluator evaluator =
        new DefaultBudgetEvaluator(store, BigDecimal.valueOf(100));

    Map<String, String> tags = Map.of("tenant_id", "test");

    // when & then
    assertThrows(
        BudgetExceededException.class,
        () -> evaluator.evaluate(tags, BigDecimal.valueOf(120))
    );
  }

  @Test
  void should_evaluate_current_status_without_cost_amount() {
    // given
    BudgetStateStore store = new InMemoryBudgetStateStore();
    BudgetEvaluator evaluator =
        new DefaultBudgetEvaluator(store, BigDecimal.valueOf(100));

    Map<String, String> tags = Map.of("tenant_id", "test");
    store.addCost(tags, BigDecimal.valueOf(90));

    // when
    BudgetDecision decision = evaluator.evaluate(tags);

    // then
    assertEquals(BudgetState.WARN, decision.state());
    assertEquals(BigDecimal.valueOf(90), decision.currentUsage());
  }

  @Test
  void evaluate_should_be_pure_function() {
    // given
    BudgetStateStore store = new InMemoryBudgetStateStore();
    BudgetEvaluator evaluator =
        new DefaultBudgetEvaluator(store, BigDecimal.valueOf(100));

    Map<String, String> tags = Map.of("tenant_id", "test");

    // when
    evaluator.evaluate(tags, BigDecimal.valueOf(50));
    evaluator.evaluate(tags, BigDecimal.valueOf(50));

    // then
    // Still ALLOW because addCost was not called
    assertEquals(BigDecimal.ZERO, store.getAccumulatedCost(tags));
    assertEquals(BudgetState.ALLOW, evaluator.evaluate(tags, BigDecimal.valueOf(50)).state());
  }
}
