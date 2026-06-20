package co.com.hermes.calendar.identity.registration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hermes.registration")
public record RegistrationProperties(String tenantBaseUrl) {

    public RegistrationProperties {
        if (tenantBaseUrl == null || tenantBaseUrl.isBlank()) {
            tenantBaseUrl = "http://hermes-tenant-service";
        }
    }
}
