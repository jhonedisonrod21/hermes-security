package co.com.hermes.calendar.auth.session;

import java.util.List;
import java.util.UUID;

public record SessionLoginResponse(
        UUID userId,
        UUID tenantId,
        String tenantSlug,
        String tenantName,
        String username,
        String email,
        List<String> roles,
        List<String> permissions
) {
}
