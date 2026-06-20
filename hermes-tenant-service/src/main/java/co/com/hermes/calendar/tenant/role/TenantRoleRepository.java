package co.com.hermes.calendar.tenant.role;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantRoleRepository extends JpaRepository<TenantRole, UUID> {

    Optional<TenantRole> findByName(String name);
}
