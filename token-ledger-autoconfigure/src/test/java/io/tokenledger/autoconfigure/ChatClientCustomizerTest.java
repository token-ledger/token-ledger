package io.tokenledger.autoconfigure;

import io.tokenledger.springai.LedgerAdvisor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class ChatClientCustomizerTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TokenLedgerAutoConfiguration.class));

    @Test
    @DisplayName("Advisor가 존재할 때 ChatClementCustomizer를 등록해야 합니다.")
    void shouldRegisterChatClientCustomizerWhenAdvisorExists() {
        this.contextRunner
                .withUserConfiguration(TestAdvisorConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatClientCustomizer.class);
                    
                    ChatClientCustomizer customizer = context.getBean(ChatClientCustomizer.class);
                    assertThat(customizer).isInstanceOf(LedgerChatClientCustomizer.class);
                });
    }

    @Configuration
    static class TestAdvisorConfiguration {
        @Bean
        public LedgerAdvisor ledgerAdvisor() {
            return (response, chain) -> response;
        }
    }
}
