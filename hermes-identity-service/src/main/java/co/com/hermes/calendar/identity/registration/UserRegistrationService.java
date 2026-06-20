package co.com.hermes.calendar.identity.registration;

import co.com.hermes.calendar.identity.auth.InternalAuthProperties;
import co.com.hermes.calendar.identity.role.Role;
import co.com.hermes.calendar.shared.contract.TenantProvisioningRequest;
import co.com.hermes.calendar.shared.contract.TenantProvisioningResponse;
import co.com.hermes.calendar.shared.security.HermesInternalHeaders;
import co.com.hermes.calendar.identity.role.RoleRepository;
import co.com.hermes.calendar.identity.user.UserAccount;
import co.com.hermes.calendar.identity.user.UserAccountRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.UUID;

@Service
public class UserRegistrationService {

    private static final String USER_ROLE = "USER";
    private static final String INTERNAL_KEY_HEADER = HermesInternalHeaders.INTERNAL_KEY;

    private final UserAccountRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final InternalAuthProperties internalProperties;
    private final RestClient tenantClient;

    public UserRegistrationService(
            UserAccountRepository users,
            RoleRepository roles,
            PasswordEncoder passwordEncoder,
            InternalAuthProperties internalProperties,
            RegistrationProperties registrationProperties,
            @Qualifier("hermesLoadBalancedRestClientBuilder") RestClient.Builder restClientBuilder
    ) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.internalProperties = internalProperties;
        this.tenantClient = restClientBuilder.baseUrl(registrationProperties.tenantBaseUrl()).build();
    }

    @Transactional
    public UserRegistrationResponse register(UserRegistrationRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByUsernameIgnoreCaseOrEmailIgnoreCase(email, email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        Role userRole = roles.findByName(USER_ROLE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "USER role is not configured"));

        UserAccount user = UserAccount.registeredUser(UUID.randomUUID(), email, passwordEncoder.encode(request.password()), userRole);
        TenantProvisioningResponse tenant = provisionTenant(user.getId(), email);
        user.assignTenant(tenant.tenantId());

        UserAccount saved = users.save(user);
        return new UserRegistrationResponse(saved.getId(), saved.getEmail(), tenant.tenantId(), tenant.tenantSlug(), USER_ROLE);
    }

    private TenantProvisioningResponse provisionTenant(UUID userId, String email) {
        TenantProvisioningResponse response = tenantClient.post()
                .uri("/internal/registrations/default-tenant")
                .header(INTERNAL_KEY_HEADER, internalProperties.apiKey())
                .body(new TenantProvisioningRequest(userId, email))
                .retrieve()
                .body(TenantProvisioningResponse.class);

        if (response == null || response.tenantId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Tenant provisioning failed");
        }
        return response;
    }
}
