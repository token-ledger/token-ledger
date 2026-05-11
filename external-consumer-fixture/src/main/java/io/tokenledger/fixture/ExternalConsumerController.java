package io.tokenledger.fixture;

import io.tokenledger.core.LedgerManager;

import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExternalConsumerController {

    private final ApplicationContext applicationContext;
    private final LedgerManager ledgerManager;

    public ExternalConsumerController(ApplicationContext applicationContext, LedgerManager ledgerManager) {
        this.applicationContext = applicationContext;
        this.ledgerManager = ledgerManager;
    }

    @GetMapping("/test/token-ledger/published")
    public Map<String, Object> published() {
        return Map.of(
                "status", "ok",
                "ledgerManager", ledgerManager.getClass().getName(),
                "pricingRegistry", applicationContext.containsBean("pricingRegistry"),
                "ledgerAdvisor", applicationContext.containsBean("ledgerAdvisor"),
                "microCostMetricsPublisher", applicationContext.containsBean("microCostMetricsPublisher")
        );
    }
}
