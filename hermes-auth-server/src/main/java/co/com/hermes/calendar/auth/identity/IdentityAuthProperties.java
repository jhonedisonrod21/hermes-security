package co.com.hermes.calendar.auth.identity;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hermes.identity")
public record IdentityAuthProperties(String baseUrl, String internalApiKey, String tenantBaseUrl) {
}
