package co.com.hermes.calendar.tenant.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hermes.internal")
public record InternalTenantProperties(String apiKey) {
}
