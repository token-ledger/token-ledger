package io.springailedger.budget;

// 예산 판단 결과를 나타내는 타입
// enum = 미리 정해진 값만 가질 수 있는 특수한 클래스
// 예산 상태는 딱 3가지만 존재하기 때문에 enum 사용
public enum BudgetStatus {
  OK,     // 정상 - 예산 80% 미만 사용 중
  WARN,   // 경고 - 예산 80% 이상 사용 중 (곧 한도 초과)
  BLOCK   // 차단 - 예산 100% 초과 (API 호출 막아야 함)
}