package io.springaileger.notification;

import io.springaileger.budget.BudgetThreshold;

/**
 * 예산 알림의 중복 발송을 방지하기 위해
 * 마지막으로 알림을 보낸 임계치를 저장하는 저장소 인터페이스입니다.
 *
 * 사용자/테넌트 식별자 기준으로 알림 상태를 관리합니다.
 */
public interface NotificationStateStore {

  /**
   * 지정된 대상에 대해 마지막으로 알림을 보낸 임계치를 반환합니다.
   */
  BudgetThreshold getLastNotifiedThreshold(String targetId);

  /**
   * 지정된 대상의 마지막 알림 임계치를 갱신합니다.
   */
  void updateLastNotifiedThreshold(String targetId, BudgetThreshold threshold);
}