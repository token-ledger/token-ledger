package io.tokenledger.micrometer;

import java.util.Set;

/**
 * Micrometer metric publishing options.
 *
 * @param allowedTagKeys metric tag keys allowed beyond the built-in low-cardinality tags
 */
public record MetricsOptions(Set<String> allowedTagKeys) {

    public static final Set<String> DEFAULT_ALLOWED_TAG_KEYS = Set.of("tenant_id");

    public MetricsOptions {
        allowedTagKeys = normalize(allowedTagKeys);
    }

    public static MetricsOptions defaults() {
        return new MetricsOptions(DEFAULT_ALLOWED_TAG_KEYS);
    }

    public static MetricsOptions withAllowedTagKeys(Set<String> allowedTagKeys) {
        return new MetricsOptions(allowedTagKeys);
    }

    private static Set<String> normalize(Set<String> allowedTagKeys) {
        if (allowedTagKeys == null) {
            return DEFAULT_ALLOWED_TAG_KEYS;
        }
        return Set.copyOf(allowedTagKeys);
    }
}
