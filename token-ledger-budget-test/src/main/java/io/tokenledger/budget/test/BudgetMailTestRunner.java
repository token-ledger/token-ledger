//package io.tokenledger.budget.test;
//
//import io.tokenledger.budget.BudgetDecision;
//import io.tokenledger.budget.BudgetState;
//import io.tokenledger.budget.BudgetThreshold;
//import io.tokenledger.notification.BudgetNotificationService;
//import org.springframework.boot.ApplicationArguments;
//import org.springframework.boot.ApplicationRunner;
//import org.springframework.stereotype.Component;
//
//import java.math.BigDecimal;
//import java.util.Map;
//
///**
// * 애플리케이션 시작 시 자동으로 실행되는 메일 테스트 클래스
// * - 50%, 80%, 100% 임계치별로 메일이 정상 전송되는지 확인
// * - Mailtrap 받은편지함에서 수신 여부 확인
// */
//@Component
//public class BudgetMailTestRunner implements ApplicationRunner {
//
//  private final BudgetNotificationService notificationService;
//
//  public BudgetMailTestRunner(BudgetNotificationService notificationService) {
//    this.notificationService = notificationService;
//  }
//
//  @Override
//  public void run(ApplicationArguments args) {
//
//    System.out.println("=== 예산 알림 메일 테스트 시작 ===");
//
//    // ✅ 50% 도달 테스트
//    System.out.println("[1] 50% 임계치 메일 전송 중...");
//    notificationService.notifyIfNeeded(
//        "test-tenant",
//        new BudgetDecision(
//            BudgetState.ALLOW,
//            BudgetThreshold.HALF,
//            "월 예산의 50% 이상 사용",
//            new BigDecimal("5.00"),   // 현재 사용량
//            new BigDecimal("10.00")   // 예산 한도
//        )
//    );
//    System.out.println("[1] 50% 메일 전송 완료!");
//
//    // ✅ 80% 도달 테스트
//    System.out.println("[2] 80% 임계치 메일 전송 중...");
//    notificationService.notifyIfNeeded(
//        "test-tenant",
//        new BudgetDecision(
//            BudgetState.WARN,
//            BudgetThreshold.WARNING,
//            "월 예산의 80% 이상 사용",
//            new BigDecimal("8.00"),
//            new BigDecimal("10.00")
//        )
//    );
//    System.out.println("[2] 80% 메일 전송 완료!");
//
//    // ✅ 100% 초과 테스트
//    System.out.println("[3] 100% 초과 메일 전송 중...");
//    notificationService.notifyIfNeeded(
//        "test-tenant",
//        new BudgetDecision(
//            BudgetState.BLOCK,
//            BudgetThreshold.EXCEEDED,
//            "월 예산을 초과했습니다",
//            new BigDecimal("11.00"),
//            new BigDecimal("10.00")
//        )
//    );
//    System.out.println("[3] 100% 메일 전송 완료!");
//
//    System.out.println("=== 테스트 완료 - Mailtrap 받은편지함 확인하세요! ===");
//  }
//}