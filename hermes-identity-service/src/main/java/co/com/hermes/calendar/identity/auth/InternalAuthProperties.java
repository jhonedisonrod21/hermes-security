package co.com.hermes.calendar.identity.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hermes.internal")
public record InternalAuthProperties(String apiKey) {
}
