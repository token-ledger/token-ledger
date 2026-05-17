package io.tokenledger.notification;

import io.tokenledger.budget.BudgetDecision;
import io.tokenledger.budget.BudgetThreshold;

/**
 * ✅ 예산 판단 결과를 기반으로 "알림을 보낼지 말지" 결정하는 서비스
 *
 * 역할:
 * - BudgetDecision을 전달받음 (이미 계산된 결과)
 * - 현재 임계치를 확인
 * - 이전에 알림을 보냈는지 확인
 * - 필요 시 이메일 전송
 *
 * 👉 중요한 설계 포인트
 * - budget: 판단만 담당
 * - notification: 행동(알림) 담당
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
   *  알림 필요 여부 판단 후 전송
   *
   * 흐름:
   * 1. 현재 임계치 확인
   * 2. 이전 알림 임계치 조회
   * 3. 더 높은 임계치일 경우만 알림 전송
   * 4. 알림 상태 업데이트
   */
  public void notifyIfNeeded(
      String targetId,
      BudgetDecision decision
  ) {
    BudgetThreshold current = decision.threshold();
    BudgetThreshold lastNotified =
        stateStore.getLastNotifiedThreshold(targetId);

    //  이전에 이미 보낸 임계치라면 알림 스킵
    if (current.ordinal() <= lastNotified.ordinal()) {
      return;
    }

    //  임계치별 알림 전송
    switch (current) {
      case HALF ->
          emailSender.sendHalfUsageWarning(targetId, decision);

      case WARNING ->
          emailSender.sendEightyPercentWarning(targetId, decision);

      case EXCEEDED ->
          emailSender.sendExceededNotification(targetId, decision);

      default -> {
        // NONE: 알림 필요 없음
        return;
      }
    }

    //  알림 발송 후 상태 업데이트 (중복 방지 핵심)
    stateStore.updateLastNotifiedThreshold(targetId, current);
  }
}