package io.springaileger.notification;

import io.springaileger.budget.BudgetThreshold;

import java.util.concurrent.ConcurrentHashMap;

/**
 * NotificationStateStore의 인메모리 구현체입니다.
 *
 * 로컬 실행 및 테스트 환경에서 사용하기 위한 기본 구현이며,
 * 프로덕션에서는 DB 또는 외부 저장소 구현체로 교체 가능합니다.
 */
public class InMemoryNotificationStateStore
    implements NotificationStateStore {

  private final ConcurrentHashMap<String, BudgetThreshold> store =
      new ConcurrentHashMap<>();

  @Override
  public BudgetThreshold getLastNotifiedThreshold(String targetId) {
    return store.getOrDefault(targetId, BudgetThreshold.NONE);
  }

  @Override
  public void updateLastNotifiedThreshold(
      String targetId,
      BudgetThreshold threshold
  ) {
    store.put(targetId, threshold);
  }
}