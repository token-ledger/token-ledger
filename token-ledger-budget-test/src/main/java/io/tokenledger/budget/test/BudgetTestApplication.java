package io.tokenledger.budget.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 예산 알림 기능 테스트용 애플리케이션
 * - Mailtrap을 통해 실제 메일 전송 여부 확인
 * - 50%, 80%, 100% 임계치별 메일 수신 테스트
 */
@SpringBootApplication
public class BudgetTestApplication {

  public static void main(String[] args) {
    SpringApplication.run(BudgetTestApplication.class, args);
  }
}