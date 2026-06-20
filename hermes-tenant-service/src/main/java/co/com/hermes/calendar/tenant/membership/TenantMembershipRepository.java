package co.com.hermes.calendar.tenant.membership;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TenantMembershipRepository extends JpaRepository<TenantMembership, UUID> {

    List<TenantMembership> findByUserIdAndStatusOrderByCreatedAtAsc(UUID userId, String status);
}
