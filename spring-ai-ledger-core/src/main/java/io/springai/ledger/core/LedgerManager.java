package io.springai.ledger.core;

import java.util.Map;

/**
 * AI 호출에 대한 지출을 기록하고 관리하는 통합 매니저 인터페이스.
 */
public interface LedgerManager {
    /**
     * 특정 모델의 호출 정보를 기록하고 최종 비용을 계산합니다.
     * @param modelName 모델 이름
     * @param usage     토큰 사용량
     * @param tags      추가 메타데이터 (tenant_id, user_id 등)
     * @return 산출된 비용
     */
    Cost record(String modelName, TokenUsage usage, Map<String, String> tags);
}
