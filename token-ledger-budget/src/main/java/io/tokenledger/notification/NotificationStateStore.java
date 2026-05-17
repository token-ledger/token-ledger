package io.tokenledger.notification;

import io.tokenledger.budget.BudgetThreshold;

/**
 *  알림 중복 발송을 방지하기 위한 상태 저장소
 *
 * 왜 필요한가?
 * - 같은 임계치 상태가 반복 호출될 경우
 *   매번 이메일을 보내는 문제 발생
 *
 * 해결 방법:
 * - "마지막으로 알림을 보낸 임계치"를 저장
 * - 이전보다 높은 임계치일 때만 알림 전송
 */
public interface NotificationStateStore {

  /**
   * 특정 대상(targetId)의 마지막 알림 임계치 조회
   */
  BudgetThreshold getLastNotifiedThreshold(String targetId);

  /**
   * 알림 발송 후, 최신 임계치로 업데이트
   */
  void updateLastNotifiedThreshold(String targetId, BudgetThreshold threshold);
}
