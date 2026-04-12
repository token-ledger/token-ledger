package io.springailedger.budget;

// 서비스별 예산 정책을 담는 클래스
// "고객지원 챗봇은 하루 $100까지, 80% 넘으면 경고" 같은 규칙을 정의
// 한번 정해진 정책은 바뀌면 안 되므로 모든 필드가 final
public class BudgetPolicy {

  // 어떤 서비스의 예산 정책인지 식별하는 ID
  // ex) "svc-customer-support", "svc-review-summary"
  private final String serviceId;

  // 하루 최대 허용 비용 (달러 단위)
  // ex) 100.0 = 하루 $100까지 허용
  private final double dailyLimitUsd;

  // 경고를 발생시킬 기준 (%)
  // ex) 80.0 = 80% 넘으면 WARN 상태로 전환
  private final double warningThresholdPercent;

  // 생성자 - 정책 만들 때 세 가지 값을 한번에 설정
  public BudgetPolicy(String serviceId,
                      double dailyLimitUsd,
                      double warningThresholdPercent) {
    this.serviceId = serviceId;
    this.dailyLimitUsd = dailyLimitUsd;
    this.warningThresholdPercent = warningThresholdPercent;
  }

  // getter만 있고 setter 없음
  // 한번 만든 정책은 외부에서 수정 불가 → 정책의 무결성 보장
  public String getServiceId() { return serviceId; }
  public double getDailyLimitUsd() { return dailyLimitUsd; }
  public double getWarningThresholdPercent() { return warningThresholdPercent; }
}