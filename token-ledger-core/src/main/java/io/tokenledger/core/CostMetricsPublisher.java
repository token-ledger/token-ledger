package io.tokenledger.core;

/**
 * AI 호출 비용 데이터를 Micrometer 메트릭으로 발행하는 인터페이스.
 * <p>
 * 이 인터페이스를 구현하여 Prometheus, Datadog 등 Micrometer와 연동된
 * 다양한 모니터링 시스템으로 비용 데이터를 전송할 수 있습니다.
 */
public interface CostMetricsPublisher {

    /**
     *  @param model 모델 이름 (ex: "gpt-4o")
     *  @param usage 실제 토큰 사용량 정보
     *  @param cost  최종 계산된 비용 정보
     */
    void publish(String model, TokenUsage usage, Cost cost);
}
