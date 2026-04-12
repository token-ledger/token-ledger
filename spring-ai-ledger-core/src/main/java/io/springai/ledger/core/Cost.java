package io.springai.ledger.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

/**
 * 계산된 AI 호출 비용 정보.
 *
 * @param value    비용 (BigDecimal, 소수점 6자리 권장)
 * @param currency 통화 (기본값: USD)
 */
public record Cost(
        BigDecimal value,
        Currency currency
) {
    public static final Currency DEFAULT_CURRENCY = Currency.getInstance("USD");

    public Cost {
        value = value.setScale(6, RoundingMode.HALF_UP);
    }

    public static Cost of(BigDecimal value) {
        return new Cost(value, DEFAULT_CURRENCY);
    }

    public static Cost zero() {
        return of(BigDecimal.ZERO);
    }

    public Cost add(Cost other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add costs with different currencies");
        }
        return new Cost(this.value.add(other.value), this.currency);
    }
}
