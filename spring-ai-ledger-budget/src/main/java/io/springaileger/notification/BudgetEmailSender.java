package io.springaileger.notification;

import io.springaileger.budget.BudgetDecision;

/**
 * 예산 임계치 도달 시 사용자에게 알림을 전달하는 역할을 정의합니다.
 *
 * 실제 전송 방식(이메일, Slack, Webhook 등)은
 * 구현체에서 결정되며, budget/notification 로직과 분리됩니다.
 */
public interface BudgetEmailSender {

  /**
   * 예산 사용률이 50%에 도달했을 때 전송되는 알림입니다.
   */
  void sendHalfUsageWarning(String email, BudgetDecision decision);

  /**
   * 예산 사용률이 80%에 도달했을 때 전송되는 경고 알림입니다.
   */
  void sendEightyPercentWarning(String email, BudgetDecision decision);

  /**
   * 예산이 초과되어 호출이 차단되었을 때 전송되는 알림입니다.
   */
  void sendExceededNotification(String email, BudgetDecision decision);
}