package io.tokenledger.notification;

import io.tokenledger.budget.BudgetState;
import io.tokenledger.budget.BudgetThreshold;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 예산 임계치 도달 시 발생하는 알림 이벤트
 */
public record BudgetNotificationEvent(
    String targetId,
    String budgetWindow,
    BudgetThreshold threshold,
    BudgetState state,
    String reason,
    BigDecimal currentUsage,
    BigDecimal limit,
    Map<String, String> tags
) {}