package io.tokenledger.notification;

import io.tokenledger.budget.BudgetThreshold;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 메모리 기반 상태 저장소
 */
public class InMemoryNotificationStateStore implements NotificationStateStore {

  private final Map<String, BudgetThreshold> store = new ConcurrentHashMap<>();

  private String key(String targetId, String window) {
    return targetId + ":" + window;
  }

  @Override
  public BudgetThreshold getLastNotifiedThreshold(
      String targetId,
      String budgetWindow
  ) {
    return store.getOrDefault(
        key(targetId, budgetWindow),
        BudgetThreshold.NONE
    );
  }

  @Override
  public void updateLastNotifiedThreshold(
      String targetId,
      String budgetWindow,
      BudgetThreshold threshold
  ) {
    store.put(key(targetId, budgetWindow), threshold);
  }
}