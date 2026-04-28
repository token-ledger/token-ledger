package io.tokenledger.autoconfigure;

import io.tokenledger.core.PricingProvider;
import io.tokenledger.springai.LedgerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Token Ledger 라이브러리의 자동 설정을 담당하는 클래스.
 */
@AutoConfiguration
@ConditionalOnClass({ChatClient.class, LedgerAdvisor.class})
@EnableConfigurationProperties(TokenLedgerProperties.class)
public class TokenLedgerAutoConfiguration {

    /**
     * 외부 설정으로부터 가격 정책을 읽어오는 PricingProvider를 등록합니다.
     */
    @Bean
    @ConditionalOnMissingBean
    public PricingProvider pricingProvider(TokenLedgerProperties properties) {
        var plans = properties.toPricingPlans();
        return () -> plans;
    }

    /**
     * LedgerAdvisor가 빈으로 등록되어 있을 경우, ChatClient.Builder를 위한 커스터마이저를 생성합니다.
     */
    @Bean
    @ConditionalOnBean(LedgerAdvisor.class)
    public LedgerChatClientCustomizer ledgerChatClientCustomizer(LedgerAdvisor ledgerAdvisor) {
        return new LedgerChatClientCustomizer(ledgerAdvisor);
    }
}
