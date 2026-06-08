package io.tokenledger.budget.internal;

import io.tokenledger.budget.BudgetDecision;
import io.tokenledger.budget.BudgetState;
import io.tokenledger.budget.BudgetThreshold;
import io.tokenledger.budget.BudgetStateStore;
import io.tokenledger.budget.exception.BudgetExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DefaultBudgetEvaluatorTest {

  private BudgetStateStore store;
  private DefaultBudgetEvaluator evaluator;

  private static final BigDecimal MONTHLY_LIMIT = new BigDecimal("100.00");
  private static final Map<String, String> TAGS = Map.of("service", "test");

  @BeforeEach
  void setUp() {
    store = mock(BudgetStateStore.class);
    evaluator = new DefaultBudgetEvaluator(store, MONTHLY_LIMIT);
  }

  // ─── evaluate(tags, costAmount) ───────────────────────────────────────────

  @Test
  void 정상_범위_사용() {
    when(store.getAccumulatedCost(TAGS)).thenReturn(new BigDecimal("10.00"));

    BudgetDecision result = evaluator.evaluate(TAGS, new BigDecimal("10.00"));

    assertThat(result.state()).isEqualTo(BudgetState.ALLOW);
    assertThat(result.threshold()).isEqualTo(BudgetThreshold.NONE);
  }

  @Test
  void 누적_50퍼센트_이상() {
    when(store.getAccumulatedCost(TAGS)).thenReturn(new BigDecimal("40.00"));

    BudgetDecision result = evaluator.evaluate(TAGS, new BigDecimal("10.00"));

    assertThat(result.state()).isEqualTo(BudgetState.ALLOW);
    assertThat(result.threshold()).isEqualTo(BudgetThreshold.HALF);
  }

  @Test
  void 누적_80퍼센트_이상() {
    when(store.getAccumulatedCost(TAGS)).thenReturn(new BigDecimal("70.00"));

    BudgetDecision result = evaluator.evaluate(TAGS, new BigDecimal("10.00"));

    assertThat(result.state()).isEqualTo(BudgetState.WARN);
    assertThat(result.threshold()).isEqualTo(BudgetThreshold.WARNING);
  }

  @Test
  void 예산_초과시_예외_발생() {
    when(store.getAccumulatedCost(TAGS)).thenReturn(new BigDecimal("95.00"));

    assertThatThrownBy(() -> evaluator.evaluate(TAGS, new BigDecimal("10.00")))
        .isInstanceOf(BudgetExceededException.class)
        .extracting(e -> ((BudgetExceededException) e).getDecision())
        .satisfies(decision -> {
          assertThat(decision.state()).isEqualTo(BudgetState.BLOCK);
          assertThat(decision.threshold()).isEqualTo(BudgetThreshold.EXCEEDED);
        });
  }

  // ─── evaluate(tags) ───────────────────────────────────────────────────────

  @Test
  void 현재_사용량_정상() {
    when(store.getAccumulatedCost(TAGS)).thenReturn(new BigDecimal("30.00"));

    BudgetDecision result = evaluator.evaluate(TAGS);

    assertThat(result.state()).isEqualTo(BudgetState.ALLOW);
    assertThat(result.threshold()).isEqualTo(BudgetThreshold.NONE);
  }

  @Test
  void 현재_사용량_50퍼센트_이상() {
    when(store.getAccumulatedCost(TAGS)).thenReturn(new BigDecimal("50.00"));

    BudgetDecision result = evaluator.evaluate(TAGS);

    assertThat(result.state()).isEqualTo(BudgetState.ALLOW);
    assertThat(result.threshold()).isEqualTo(BudgetThreshold.HALF);
  }

  @Test
  void 현재_사용량_80퍼센트_이상() {
    when(store.getAccumulatedCost(TAGS)).thenReturn(new BigDecimal("80.00"));

    BudgetDecision result = evaluator.evaluate(TAGS);

    assertThat(result.state()).isEqualTo(BudgetState.WARN);
    assertThat(result.threshold()).isEqualTo(BudgetThreshold.WARNING);
  }

  @Test
  void 현재_사용량_100퍼센트_이상() {
    when(store.getAccumulatedCost(TAGS)).thenReturn(new BigDecimal("100.00"));

    BudgetDecision result = evaluator.evaluate(TAGS);

    assertThat(result.state()).isEqualTo(BudgetState.BLOCK);
    assertThat(result.threshold()).isEqualTo(BudgetThreshold.EXCEEDED);
  }
}
