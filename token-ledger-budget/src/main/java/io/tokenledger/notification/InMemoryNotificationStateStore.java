package io.tokenledger.notification;

import io.tokenledger.budget.BudgetThreshold;

import java.util.concurrent.ConcurrentHashMap;

/**
 *  NotificationStateStore의 메모리 기반 구현체
 *
 * 특징:
 * - 애플리케이션 재시작 시 데이터 초기화됨
 * - 테스트 및 로컬 개발 환경에 적합
 *
 */
public class InMemoryNotificationStateStore implements NotificationStateStore {

  private final ConcurrentHashMap<String, BudgetThreshold> store =
      new ConcurrentHashMap<>();

  @Override
  public BudgetThreshold getLastNotifiedThreshold(String targetId) {
    // 기본값: NONE (아직 알림 보낸 적 없음)
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