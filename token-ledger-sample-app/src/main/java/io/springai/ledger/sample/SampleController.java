package io.springai.ledger.sample;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SampleController {

    @GetMapping("/test/ai")
    public String testAiLogic() {
        return "AI 장부 분석 테스트 성공! (이 접속이 그라파나에 기록됩니다.)";
    }
}