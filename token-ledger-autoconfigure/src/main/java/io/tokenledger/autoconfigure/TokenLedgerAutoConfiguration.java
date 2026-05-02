package io.tokenledger.autoconfigure;

import io.tokenledger.core.CostCalculator;
import io.tokenledger.core.LedgerListener;
import io.tokenledger.core.LedgerManager;
import io.tokenledger.core.PricingProvider;
import io.tokenledger.core.PricingRegistry;
import io.tokenledger.core.internal.DefaultCostCalculator;
import io.tokenledger.core.internal.DefaultLedgerManager;
import io.tokenledger.core.internal.InMemoryPricingRegistry;
import io.tokenledger.springai.LedgerAdvisor;
import io.tokenledger.springai.UsageExtractor;
import io.tokenledger.springai.internal.DefaultLedgerAdvisor;
import io.tokenledger.springai.internal.DefaultUsageExtractor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
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
@ConditionalOnClass(LedgerAdvisor.class)
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
     * 메모리 기반의 가격 정책 저장소(PricingRegistry)를 등록합니다.
     */
    @Bean
    @ConditionalOnMissingBean
    public PricingRegistry pricingRegistry(ObjectProvider<PricingProvider> pricingProviders) {
        return new InMemoryPricingRegistry(pricingProviders.orderedStream().toList());
    }

    /**
     * 토큰 사용량을 바탕으로 비용을 계산하는 CostCalculator를 등록합니다.
     */
    @Bean
    @ConditionalOnMissingBean
    public CostCalculator costCalculator() {
        return new DefaultCostCalculator();
    }

    /**
     * 비용 기록 및 리스너 관리를 담당하는 LedgerManager를 등록합니다.
     */
    @Bean
    @ConditionalOnMissingBean
    public LedgerManager ledgerManager(PricingRegistry pricingRegistry,
                                       CostCalculator costCalculator,
                                       ObjectProvider<LedgerListener> ledgerListeners) {
        return new DefaultLedgerManager(pricingRegistry, costCalculator, ledgerListeners.orderedStream().toList());
    }

    /**
     * ChatClientResponse에서 토큰 사용량을 추출하는 UsageExtractor를 등록합니다.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(ChatClient.class)
    public UsageExtractor usageExtractor() {
        return new DefaultUsageExtractor();
    }

    /**
     * Spring AI 호출 전후로 비용을 측정하고 기록하는 LedgerAdvisor를 등록합니다.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(ChatClient.class)
    public LedgerAdvisor ledgerAdvisor(LedgerManager ledgerManager, UsageExtractor usageExtractor) {
        return new DefaultLedgerAdvisor(ledgerManager, usageExtractor);
    }

    /**
     * LedgerAdvisor가 빈으로 등록되어 있을 경우, ChatClient.Builder를 위한 커스터마이저를 생성합니다.
     */
    @Bean
    @ConditionalOnBean(LedgerAdvisor.class)
    @ConditionalOnClass(ChatClient.class)
    public LedgerChatClientCustomizer ledgerChatClientCustomizer(LedgerAdvisor ledgerAdvisor) {
        return new LedgerChatClientCustomizer(ledgerAdvisor);
    }
}
