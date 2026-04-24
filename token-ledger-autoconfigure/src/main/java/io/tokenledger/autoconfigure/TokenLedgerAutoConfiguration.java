package io.tokenledger.autoconfigure;

import io.tokenledger.springai.LedgerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * Token Ledger 라이브러리의 자동 설정을 담당하는 클래스.
 */
@AutoConfiguration
@ConditionalOnClass({ChatClient.class, LedgerAdvisor.class})
public class TokenLedgerAutoConfiguration {

    /**
     * LedgerAdvisor가 빈으로 등록되어 있을 경우, ChatClient.Builder를 위한 커스터마이저를 생성합니다.
     */
    @Bean
    @ConditionalOnBean(LedgerAdvisor.class)
    public LedgerChatClientCustomizer ledgerChatClientCustomizer(LedgerAdvisor ledgerAdvisor) {
        return new LedgerChatClientCustomizer(ledgerAdvisor);
    }
}
