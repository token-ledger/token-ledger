//package io.tokenledger.budget.test;
//
//import io.tokenledger.notification.BudgetEmailSender;
//import io.tokenledger.notification.BudgetNotificationService;
//import io.tokenledger.notification.InMemoryNotificationStateStore;
//import io.tokenledger.notification.NotificationStateStore;
//import io.tokenledger.notification.SmtpBudgetEmailSender;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.mail.javamail.JavaMailSender;
//
///**
// * 예산 알림 관련 빈 설정 클래스
// * - SmtpBudgetEmailSender : 실제 메일 전송 담당
// * - NotificationStateStore: 중복 알림 방지를 위한 상태 저장
// * - BudgetNotificationService: 알림 전송 여부 판단 및 실행
// */
//@Configuration
//public class NotificationConfig {
//
//  /** application.yml 의 token-ledger.notification.from 값 주입 */
//  @Value("${token-ledger.notification.from}")
//  private String fromAddress;
//
//  /**
//   * SMTP 메일 전송 구현체 빈 등록
//   * JavaMailSender 는 spring-boot-starter-mail 이 자동으로 생성해줌
//   */
//  @Bean
//  public BudgetEmailSender budgetEmailSender(JavaMailSender mailSender) {
//    return new SmtpBudgetEmailSender(mailSender, fromAddress);
//  }
//
//  /**
//   * 알림 상태 저장소 빈 등록
//   * InMemory 구현체 사용 (재시작 시 초기화됨)
//   */
//  @Bean
//  public NotificationStateStore notificationStateStore() {
//    return new InMemoryNotificationStateStore();
//  }
//
//  /**
//   * 알림 서비스 빈 등록
//   * 임계치 초과 시 메일 전송 여부 결정
//   */
//  @Bean
//  public BudgetNotificationService budgetNotificationService(
//      BudgetEmailSender budgetEmailSender,
//      NotificationStateStore notificationStateStore
//  ) {
//    return new BudgetNotificationService(budgetEmailSender, notificationStateStore);
//  }
//}