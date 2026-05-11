package io.springaileger.notification;

import io.springaileger.budget.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

/**
 * BudgetNotificationService가
 * 예산 임계치에 따라 알림을 한 번만 보내는지 검증하는 테스트
 */
class BudgetNotificationServiceTest {

  @Test
  void should_send_notification_only_once_per_threshold() {
    // given
    BudgetEmailSender emailSender = mock(BudgetEmailSender.class);
    NotificationStateStore stateStore = new InMemoryNotificationStateStore();

    BudgetNotificationService service =
        new BudgetNotificationService(emailSender, stateStore);

    BudgetDecision decision = new BudgetDecision(
        BudgetState.WARN,
        BudgetThreshold.WARNING,
        "80% 도달",
        BigDecimal.valueOf(80),
        BigDecimal.valueOf(100)
    );

    // when: 같은 decision을 두 번 전달
    service.notifyIfNeeded("user@test.com", decision);
    service.notifyIfNeeded("user@test.com", decision);

    // then: 메일은 한 번만 전송
    verify(emailSender, times(1))
        .sendEightyPercentWarning("user@test.com", decision);
  }
}