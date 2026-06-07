package io.tokenledger.notification;

import io.tokenledger.budget.BudgetThreshold;

/**
 * 알림 중복 방지를 위한 상태 저장소
 */
public interface NotificationStateStore {

  BudgetThreshold getLastNotifiedThreshold(
      String targetId,
      String budgetWindow
  );

  void updateLastNotifiedThreshold(
      String targetId,
      String budgetWindow,
      BudgetThreshold threshold
  );
}