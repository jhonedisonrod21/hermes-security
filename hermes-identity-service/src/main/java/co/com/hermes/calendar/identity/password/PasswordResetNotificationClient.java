package co.com.hermes.calendar.identity.password;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Envía el correo de restablecimiento de contraseña a través de notification-service. Best-effort: un
 * fallo no debe filtrar si el email existe ni romper la solicitud (el usuario podrá reintentar).
 */
@Component
public class PasswordResetNotificationClient {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetNotificationClient.class);
    private static final String INTERNAL_KEY_HEADER = "X-Hermes-Internal-Key";

    private final RestClient notification;
    private final String internalApiKey;

    public PasswordResetNotificationClient(
            @Qualifier("hermesLoadBalancedRestClientBuilder") RestClient.Builder loadBalancedRestClientBuilder,
            @Value("${hermes.notification.base-url:http://hermes-notification-service}") String notificationBaseUrl,
            @Value("${hermes.internal.api-key}") String internalApiKey
    ) {
        this.notification = loadBalancedRestClientBuilder.baseUrl(notificationBaseUrl).build();
        this.internalApiKey = internalApiKey;
    }

    public void sendPasswordResetEmail(String email, String displayName, String resetUrl, int expiresMinutes) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("displayName", displayName);
        body.put("resetUrl", resetUrl);
        body.put("expiresMinutes", expiresMinutes);
        try {
            notification.post()
                    .uri("/internal/notifications/password-reset")
                    .header(INTERNAL_KEY_HEADER, internalApiKey)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException ex) {
            log.warn("Could not send password reset email: {}", ex.getMessage());
        }
    }
}
