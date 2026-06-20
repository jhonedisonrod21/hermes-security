package co.com.hermes.calendar.auth.session;

import jakarta.validation.constraints.NotBlank;

public record SessionLoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {
}
