package io.tokenledger.core;

import io.tokenledger.core.domain.CostRecordedEvent;

/**
 * 비용 기록 이벤트를 수신하는 리스너 인터페이스.
 * 옵저버 패턴의 Observer 역할을 수행하며, 순수 자바 기반으로 동작합니다.
 */
@FunctionalInterface
public interface LedgerListener {
    /**
     * 비용 기록 이벤트가 발생했을 때 호출됩니다.
     * @param event 발생한 비용 기록 이벤트
     */
    void onRecord(CostRecordedEvent event);
}
