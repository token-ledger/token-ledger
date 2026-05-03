package io.tokenledger.micrometer.internal;

import io.micrometer.core.instrument.MeterRegistry;
import io.tokenledger.core.LedgerListener;

import java.util.Set;

/**
 * Micrometer 메트릭 퍼블리셔 생성을 위한 팩토리 클래스입니다.
 */
public final class LedgerMicrometerComponents {

    private LedgerMicrometerComponents() {
    }

    public static LedgerListener microCostMetricsPublisher(MeterRegistry meterRegistry, Set<String> allowedTagKeys) {
        return new MicroCostMetricsPublisher(meterRegistry, allowedTagKeys);
    }
}
