package io.tokenledger.autoconfigure;

import io.tokenledger.springai.LedgerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientCustomizer;

/**
 * ChatClient.Builder에 LedgerAdvisor를 자동으로 주입하는 커스터마이저.
 */
public class LedgerChatClientCustomizer implements ChatClientCustomizer {

    private final LedgerAdvisor ledgerAdvisor;

    public LedgerChatClientCustomizer(LedgerAdvisor ledgerAdvisor) {
        this.ledgerAdvisor = ledgerAdvisor;
    }

    @Override
    public void customize(ChatClient.Builder builder) {
        builder.defaultAdvisors(ledgerAdvisor);
    }
}
