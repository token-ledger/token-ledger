package io.tokenledger.notification;

import io.tokenledger.budget.BudgetDecision;
import io.tokenledger.budget.BudgetState;
import io.tokenledger.budget.BudgetThreshold;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.Mockito.*;

/**
 * BudgetNotificationService 동작 검증 테스트
 */
class BudgetNotificationServiceTest {

  @Test
  void threshold_increase_triggers_event_only_once_per_window() {

    BudgetNotificationHandler handler = mock(BudgetNotificationHandler.class);
    NotificationStateStore store = new InMemoryNotificationStateStore();

    BudgetNotificationService service =
        new BudgetNotificationService(handler, store);

    String targetId = "user1";
    String window = "2026-06";

    BudgetDecision decision50 = new BudgetDecision(
        BudgetState.WARN,
        BudgetThreshold.HALF,
        "50%",
        BigDecimal.valueOf(50),
        BigDecimal.valueOf(100)
    );

    BudgetDecision decision80 = new BudgetDecision(
        BudgetState.WARN,
        BudgetThreshold.WARNING,
        "80%",
        BigDecimal.valueOf(80),
        BigDecimal.valueOf(100)
    );

    // 50% 최초 → 발생
    service.notifyIfNeeded(decision50, targetId, window, Map.of());

    // 50% 반복 → 발생 안됨
    service.notifyIfNeeded(decision50, targetId, window, Map.of());

    // 80% → 발생
    service.notifyIfNeeded(decision80, targetId, window, Map.of());

    // 총 2번 발생해야 함 (50, 80)
    verify(handler, times(2)).handle(any());
  }

  @Test
  void same_threshold_should_not_trigger_duplicate_event() {

    BudgetNotificationHandler handler = mock(BudgetNotificationHandler.class);
    NotificationStateStore store = new InMemoryNotificationStateStore();

    BudgetNotificationService service =
        new BudgetNotificationService(handler, store);

    String targetId = "user1";
    String window = "2026-06";

    BudgetDecision decision50 = new BudgetDecision(
        BudgetState.WARN,
        BudgetThreshold.HALF,
        "50%",
        BigDecimal.valueOf(50),
        BigDecimal.valueOf(100)
    );

    service.notifyIfNeeded(decision50, targetId, window, Map.of());
    service.notifyIfNeeded(decision50, targetId, window, Map.of());

    verify(handler, times(1)).handle(any());
  }

  @Test
  void new_window_should_allow_notification_again() {

    BudgetNotificationHandler handler = mock(BudgetNotificationHandler.class);
    NotificationStateStore store = new InMemoryNotificationStateStore();

    BudgetNotificationService service =
        new BudgetNotificationService(handler, store);

    String targetId = "user1";

    BudgetDecision decision50 = new BudgetDecision(
        BudgetState.WARN,
        BudgetThreshold.HALF,
        "50%",
        BigDecimal.valueOf(50),
        BigDecimal.valueOf(100)
    );

    // 6월
    service.notifyIfNeeded(decision50, targetId, "2026-06", Map.of());

    // 7월 → 다시 발생해야 함
    service.notifyIfNeeded(decision50, targetId, "2026-07", Map.of());

    verify(handler, times(2)).handle(any());
  }
}