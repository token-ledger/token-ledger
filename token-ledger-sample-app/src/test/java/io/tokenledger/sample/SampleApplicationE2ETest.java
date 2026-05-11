package io.tokenledger.sample;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "token-ledger.enabled=true",
                "token-ledger.pricing.plans[0].model-id=gpt-4o-mini",
                "token-ledger.pricing.plans[0].currency=USD",
                "token-ledger.pricing.plans[0].rates.PROMPT=0.00015",
                "token-ledger.pricing.plans[0].rates.COMPLETION=0.00060",
                "token-ledger.metrics.enabled=true",
                "token-ledger.metrics.tag-whitelist[0]=tenant_id",
                "management.endpoints.web.exposure.include=prometheus,health"
        }
)
class SampleApplicationE2ETest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Test
    void starterEndpointsAndPrometheusMetricsWorkEndToEnd() throws Exception {
        HttpResponse<String> smoke = get("/test/token-ledger/smoke");
        assertThat(smoke.statusCode()).isEqualTo(200);
        assertThat(smoke.body())
                .contains("\"status\":\"ok\"")
                .contains("\"starter\":\"token-ledger-starter\"");

        HttpResponse<String> beans = get("/test/token-ledger/beans");
        assertThat(beans.statusCode()).isEqualTo(200);
        assertThat(beans.body())
                .contains("\"ledgerManager\":true")
                .contains("\"ledgerAdvisor\":true")
                .contains("\"pricingRegistry\":true")
                .contains("\"microCostMetricsPublisher\":true");

        HttpResponse<String> record = get("/test/token-ledger/record");
        assertThat(record.statusCode()).isEqualTo(200);
        assertThat(record.body())
                .contains("\"modelId\":\"gpt-4o-mini\"")
                .contains("\"cost\":\"0.001350\"")
                .contains("\"currency\":\"USD\"");

        HttpResponse<String> prometheus = get("/actuator/prometheus");
        assertThat(prometheus.statusCode()).isEqualTo(200);
        assertThat(prometheus.body())
                .contains("ai_token_usage_total")
                .contains("ai_token_usage_distribution")
                .contains("ai_token_cost_total")
                .contains("tenant_id=\"sample-tenant\"")
                .doesNotContain("user_id=\"sample-user\"");
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
