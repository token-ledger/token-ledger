package io.springai.ledger.sample;

import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SampleController {

    private final ApplicationContext applicationContext;

    public SampleController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @GetMapping("/test/ai")
    public String testAiLogic() {
        return "AI 장부 분석 테스트 성공! (이 접속이 그라파나에 기록됩니다.)";
    }

    @GetMapping("/test/token-ledger/smoke")
    public Map<String, String> smoke() {
        return Map.of(
                "status", "ok",
                "starter", "token-ledger-starter"
        );
    }

    @GetMapping("/test/token-ledger/beans")
    public Map<String, Boolean> tokenLedgerBeans() {
        return Map.of(
                "ledgerManager", applicationContext.containsBean("ledgerManager"),
                "ledgerAdvisor", applicationContext.containsBean("ledgerAdvisor"),
                "pricingRegistry", applicationContext.containsBean("pricingRegistry")
        );
    }

}