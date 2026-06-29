package co.com.hermes.calendar.identity.bootstrap;

import co.com.hermes.calendar.identity.role.Role;
import co.com.hermes.calendar.identity.role.RoleRepository;
import co.com.hermes.calendar.identity.user.UserAccount;
import co.com.hermes.calendar.identity.user.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Crea el administrador del sistema SOLO en el perfil {@code local}, con una credencial de
 * desarrollo. Las migraciones Flyway no siembran credenciales (ver V1), de modo que dev/prod
 * nunca arrancan con una cuenta de password conocido; allí el admin se da de alta fuera de banda.
 */
@Component
@Profile("local")
public class LocalAdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalAdminSeeder.class);

    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000100");
    private static final String ADMIN_EMAIL = "admin@hermes.local";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String SYSTEM_ADMIN_ROLE = "SYSTEM_ADMIN";

    private final UserAccountRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;

    public LocalAdminSeeder(UserAccountRepository users, RoleRepository roles, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
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
                ADMIN_ID, ADMIN_EMAIL, passwordEncoder.encode(ADMIN_PASSWORD), systemAdmin);
        admin.setName("Administrador del sistema");
        users.save(admin);
        log.info("Seeded local SYSTEM_ADMIN account: {}", ADMIN_EMAIL);
    }
}
