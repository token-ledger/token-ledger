package io.tokenledger.autoconfigure;

import io.tokenledger.core.domain.PricingPlan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Token Ledger의 설정을 담당하는 프로퍼티 클래스.
 */
@ConfigurationProperties(prefix = "token-ledger")
public class TokenLedgerProperties {

    private boolean enabled = true;

    @NestedConfigurationProperty
    private PricingProperties pricing = new PricingProperties();

    @NestedConfigurationProperty
    private MetricsProperties metrics = new MetricsProperties();

    @NestedConfigurationProperty
    private BudgetProperties budget = new BudgetProperties();

    @NestedConfigurationProperty
    private NotificationProperties notification = new NotificationProperties();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public PricingProperties getPricing() {
        return pricing;
    }

    public void setPricing(PricingProperties pricing) {
        this.pricing = pricing;
    }

    public MetricsProperties getMetrics() {
        return metrics;
    }

    public void setMetrics(MetricsProperties metrics) {
        this.metrics = metrics;
    }

    public BudgetProperties getBudget() {
        return budget;
    }

    public void setBudget(BudgetProperties budget) {
        this.budget = budget;
    }

    public NotificationProperties getNotification() {
        return notification;
    }

    public void setNotification(NotificationProperties notification) {
        this.notification = notification;
    }

    public List<PricingPlan> toPricingPlans() {
        if (pricing == null || pricing.getPlans() == null) {
            return List.of();
        }

        return pricing.getPlans()
                      .stream()
                      .map(PricingPlanProperties::toPricingPlan)
                      .toList();
    }

    public static class PricingProperties {
        private List<PricingPlanProperties> plans = new ArrayList<>();

        public List<PricingPlanProperties> getPlans() {
            return plans;
        }

        public void setPlans(List<PricingPlanProperties> plans) {
            this.plans = plans;
        }
    }

    public static class MetricsProperties {
        private boolean enabled = true;
        private Set<String> tagWhitelist = new HashSet<>(List.of("tenant_id"));

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Set<String> getTagWhitelist() {
            return tagWhitelist;
        }

        public void setTagWhitelist(Set<String> tagWhitelist) {
            this.tagWhitelist = tagWhitelist;
        }
    }

    public static class BudgetProperties {
        private boolean enabled = false;
        private java.math.BigDecimal monthlyLimit = new java.math.BigDecimal("10.00");

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public java.math.BigDecimal getMonthlyLimit() {
            return monthlyLimit;
        }

        public void setMonthlyLimit(java.math.BigDecimal monthlyLimit) {
            this.monthlyLimit = monthlyLimit;
        }
    }

    public static class NotificationProperties {
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
