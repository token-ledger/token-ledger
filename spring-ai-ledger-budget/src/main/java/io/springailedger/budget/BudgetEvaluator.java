package io.springailedger.budget;

// 핵심 클래스 - 비용을 누적하고 예산 초과 여부를 판단
// BudgetAccumulator로 비용을 쌓고
// BudgetPolicy 기준으로 현재 상태(OK/WARN/BLOCK)를 반환
public class BudgetEvaluator {

  // 누적 비용을 저장하는 BudgetAccumulator
  // 직접 new로 만들지 않고 외부에서 주입받음 (의존성 주입)
  // 이렇게 하면 테스트할 때 가짜 Accumulator를 넣어서 테스트 가능
  private final BudgetAccumulator accumulator;

  public BudgetEvaluator(BudgetAccumulator accumulator) {
    this.accumulator = accumulator;
  }

  // 비용을 누적하고 현재 예산 상태를 판단해서 반환하는 핵심 메서드
  // policy: 이 서비스의 예산 정책 (한도, 경고 기준)
  // costUsd: 이번 AI 호출로 발생한 비용 (달러)
  public BudgetStatus evaluate(BudgetPolicy policy, double costUsd) {

    // 1. 이번 호출 비용을 누적
    accumulator.add(policy.getServiceId(), costUsd);

    // 2. 지금까지 이 서비스가 누적한 총 비용 조회
    double total = accumulator.getAccumulated(policy.getServiceId());

    // 3. 한도 대비 몇 % 사용했는지 계산
    // ex) 총 $85 사용 / 한도 $100 * 100 = 85%
    double usagePercent = (total / policy.getDailyLimitUsd()) * 100.0;

    // 4. 사용률 기준으로 상태 판단
    if (usagePercent >= 100.0) {
      // 한도 100% 초과 → 차단
      // ex) $110 사용 / $100 한도 = 110% → BLOCK
      return BudgetStatus.BLOCK;

    } else if (usagePercent >= policy.getWarningThresholdPercent()) {
      // 경고 기준(80%) 이상 → 경고
      // ex) $85 사용 / $100 한도 = 85% → WARN
      return BudgetStatus.WARN;

    } else {
      // 경고 기준 미만 → 정상
      // ex) $50 사용 / $100 한도 = 50% → OK
      return BudgetStatus.OK;
    }
  }
}