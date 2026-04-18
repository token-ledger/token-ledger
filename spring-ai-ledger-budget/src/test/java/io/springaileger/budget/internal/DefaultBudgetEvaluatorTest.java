package io.springaileger.budget.internal;

import io.springaileger.budget.*;
import io.springaileger.budget.internal.DefaultBudgetEvaluator;
import io.springaileger.budget.internal.InMemoryBudgetStateStore;
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
}
