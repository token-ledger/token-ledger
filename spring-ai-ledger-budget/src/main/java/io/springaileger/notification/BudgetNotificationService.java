package io.springaileger.notification;

import io.springaileger.budget.BudgetDecision;
import io.springaileger.budget.BudgetThreshold;

/**
 * 예산 평가 결과를 기반으로
 * 사용자 알림을 발송할지 여부를 판단하는 서비스입니다.
 *
 * - 임계치(50%, 80%, 100%) 도달 시 알림 전송
 * - 동일 임계치에 대해서는 1회만 알림 발송
 *
 * 예산 판단 로직(budget)과 알림 실행 로직을 분리하기 위한 계층입니다.
 */
public class BudgetNotificationService {

  private final BudgetEmailSender emailSender;
  private final NotificationStateStore stateStore;

  public BudgetNotificationService(
      BudgetEmailSender emailSender,
      NotificationStateStore stateStore
  ) {
    this.emailSender = emailSender;
    this.stateStore = stateStore;
  }

  /**
   * 예산 판단 결과를 바탕으로 필요 시 알림을 전송합니다.
   *
   * 이미 알림을 보낸 임계치 이하의 상태인 경우
   * 중복 알림을 방지하기 위해 아무 작업도 하지 않습니다.
   */
  public void notifyIfNeeded(
      String targetId,
      BudgetDecision decision
  ) {
    BudgetThreshold current = decision.threshold();
    BudgetThreshold lastNotified =
        stateStore.getLastNotifiedThreshold(targetId);

    // 이미 알림을 보낸 임계치라면 종료
    if (current.ordinal() <= lastNotified.ordinal()) {
      return;
    }

    switch (current) {
      case HALF ->
          emailSender.sendHalfUsageWarning(targetId, decision);

      case WARNING ->
          emailSender.sendEightyPercentWarning(targetId, decision);

      case EXCEEDED ->
          emailSender.sendExceededNotification(targetId, decision);

      default -> {
        return;
      }
    }

    // 알림 발송 후 상태 갱신
    stateStore.updateLastNotifiedThreshold(targetId, current);
  }
}