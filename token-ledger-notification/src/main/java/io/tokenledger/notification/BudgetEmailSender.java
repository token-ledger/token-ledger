package io.tokenledger.notification;

import io.tokenledger.budget.BudgetDecision;

/**
 *  예산 알림을 "어떻게 보낼 것인가"를 정의하는 인터페이스
 *
 * - 이 인터페이스는 실제 이메일, Slack, Webhook 등
 *   다양한 채널로 확장될 수 있도록 설계됨
 * - notification 계층은 "알림을 보낼지"만 결정하고,
 *   "어떻게 보낼지"는 이 인터페이스 구현체가 담당
 */
public interface BudgetEmailSender {

  /**
   * 예산 50% 도달 시 알림
   */
  void sendHalfUsageWarning(String email, BudgetDecision decision);

  /**
   * 예산 80% 도달 시 경고 알림
   */
  void sendEightyPercentWarning(String email, BudgetDecision decision);

  /**
   * 예산 초과 시 알림
   */
  void sendExceededNotification(String email, BudgetDecision decision);
}