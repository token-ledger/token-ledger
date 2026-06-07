package io.tokenledger.notification;

/**
 * 알림 이벤트를 처리하는 인터페이스
 */
public interface BudgetNotificationHandler {

  void handle(BudgetNotificationEvent event);
}