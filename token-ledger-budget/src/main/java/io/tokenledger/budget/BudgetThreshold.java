package io.tokenledger.budget;

/**
 *  예산 사용률에 따른 임계치 정의
 *
 * NONE     : 임계치 미도달
 * HALF     : 50% 도달
 * WARNING  : 80% 도달
 * EXCEEDED : 100% 초과
 *
 *  notification 모듈에서 알림 판단 기준으로 사용됨
 */
public enum BudgetThreshold {

  NONE,       // 아직 알림 필요 없음
  HALF,       // 50%
  WARNING,    // 80%
  EXCEEDED    // 100%
}