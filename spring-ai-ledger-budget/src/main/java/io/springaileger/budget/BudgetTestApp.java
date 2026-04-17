package io.springaileger.budget;

import java.math.BigDecimal;
import java.util.Map;

public class BudgetTestApp {

  public static void main(String[] args) {

    // 1️⃣ 저장소 생성 (메모리)
    BudgetStateStore store = new InMemoryBudgetStateStore();

    // 2️⃣ 월 예산 한도 설정 (예: 10달러)
    BigDecimal monthlyLimit = new BigDecimal("10.00");

    // 3️⃣ 예산 평가기 생성
    BudgetEvaluator evaluator =
        new DefaultBudgetEvaluator(store, monthlyLimit);

    // 4️⃣ 예산 식별자 (tenant)
    Map<String, String> tags = Map.of(
        "tenant_id", "tenantA"
    );

    // 5️⃣ 호출 비용 (한 번에 3달러)
    BigDecimal costPerCall = new BigDecimal("3.00");

    try {
      for (int i = 1; i <= 5; i++) {
        System.out.println("==== 호출 " + i + " ====");

        BudgetDecision decision =
            evaluator.evaluate(tags, costPerCall);

        System.out.println("상태: " + decision.state());
        System.out.println("사유: " + decision.reason());
        System.out.println("누적 사용량: " + decision.currentUsage());
        System.out.println();
      }
    } catch (BudgetExceededException e) {
      System.out.println("🚨 호출 차단!");
      System.out.println(e.getMessage());
      System.out.println("누적 사용량: " +
          e.getDecision().currentUsage());
    }
  }
}