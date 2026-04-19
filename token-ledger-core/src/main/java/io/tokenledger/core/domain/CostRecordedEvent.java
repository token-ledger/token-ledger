package io.tokenledger.core.domain;

import java.util.Map;

/**
 * 토큰 사용량과 비용이 기록되었을 때 발생하는 이벤트 데이터.
 * 불변 객체(Record)로 정의하여 이벤트 전달의 안정성을 보장합니다.
 */
public record CostRecordedEvent(
    String modelId,
    TokenUsage usage,
    Cost cost,
    Map<String, String> tags
) {}
