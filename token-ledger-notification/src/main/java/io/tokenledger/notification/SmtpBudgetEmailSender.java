package io.tokenledger.notification;

import io.tokenledger.budget.BudgetDecision;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

public class SmtpBudgetEmailSender implements BudgetEmailSender {

  private final JavaMailSender mailSender;
  private final String fromAddress;

  public SmtpBudgetEmailSender(JavaMailSender mailSender, String fromAddress) {
    this.mailSender = mailSender;
    this.fromAddress = fromAddress;
  }

  @Override
  public void sendHalfUsageWarning(String email, BudgetDecision decision) {
    send(
        email,
        "[Token Ledger] 예산 50% 도달 알림",
        """
        예산의 50%%에 도달했습니다.

        현재 사용량 : %s
        예산 한도   : %s
        """.formatted(decision.currentUsage(), decision.limit())
    );
  }

  @Override
  public void sendEightyPercentWarning(String email, BudgetDecision decision) {
    send(
        email,
        "[Token Ledger] ⚠️ 예산 80% 경고",
        """
        예산의 80%%에 도달했습니다. 사용량을 확인하세요.

        현재 사용량 : %s
        예산 한도   : %s
        """.formatted(decision.currentUsage(), decision.limit())
    );
  }

  @Override
  public void sendExceededNotification(String email, BudgetDecision decision) {
    send(
        email,
        "[Token Ledger] 예산 초과",
        """
        예산을 초과했습니다. LLM 호출이 차단됩니다.

        현재 사용량 : %s
        예산 한도   : %s
        """.formatted(decision.currentUsage(), decision.limit())
    );
  }

  private void send(String to, String subject, String text) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(fromAddress);
    message.setTo(to);
    message.setSubject(subject);
    message.setText(text);
    mailSender.send(message);
  }
}