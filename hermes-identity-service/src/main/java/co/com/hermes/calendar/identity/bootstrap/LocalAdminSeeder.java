package co.com.hermes.calendar.identity.bootstrap;

import co.com.hermes.calendar.identity.role.Role;
import co.com.hermes.calendar.identity.role.RoleRepository;
import co.com.hermes.calendar.identity.user.UserAccount;
import co.com.hermes.calendar.identity.user.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Crea el administrador del sistema en los perfiles {@code local} y {@code dev} (este último, el despliegue
 * contenerizado de prueba/PoC). La credencial se inyecta desde configuración ({@code hermes.local-admin.password}):
 * en {@code local} tiene un valor de conveniencia (application-local.yml); en {@code dev} es obligatoria y
 * llega por variable de entorno ({@code HERMES_LOCAL_ADMIN_PASSWORD}, fail-fast en identity-dev.yml), de modo
 * que no hay ningún password en el código fuente. Las migraciones Flyway no siembran credenciales (ver V1);
 * en producción no se activa este seeder y el admin se da de alta fuera de banda.
 */
@Component
@Profile({"local", "dev"})
public class LocalAdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalAdminSeeder.class);

    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000100");
    private static final String ADMIN_EMAIL = "admin@hermes.local";
    private static final String SYSTEM_ADMIN_ROLE = "SYSTEM_ADMIN";

    private final UserAccountRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;

    public LocalAdminSeeder(UserAccountRepository users, RoleRepository roles, PasswordEncoder passwordEncoder,
                            @Value("${hermes.local-admin.password}") String adminPassword) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (users.existsByUsernameIgnoreCaseOrEmailIgnoreCase(ADMIN_EMAIL, ADMIN_EMAIL)) {
            return;
        }
        Role systemAdmin = roles.findByName(SYSTEM_ADMIN_ROLE).orElse(null);
        if (systemAdmin == null) {
            log.warn("Local admin seed skipped: SYSTEM_ADMIN role not found");
            return;
        }
        UserAccount admin = UserAccount.registeredUser(
                ADMIN_ID, ADMIN_EMAIL, passwordEncoder.encode(adminPassword), systemAdmin);
        admin.setName("Administrador del sistema");
        users.save(admin);
        log.info("Seeded local SYSTEM_ADMIN account: {}", ADMIN_EMAIL);
    }
}
