package co.com.hermes.calendar.tenant.membership;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantMembershipRepository extends JpaRepository<TenantMembership, UUID> {

    List<TenantMembership> findByUserIdAndStatusOrderByCreatedAtAsc(UUID userId, String status);

    boolean existsByTenant_IdAndUserId(UUID tenantId, UUID userId);

    Optional<TenantMembership> findByTenant_IdAndUserId(UUID tenantId, UUID userId);

    Page<TenantMembership> findByTenant_Id(UUID tenantId, Pageable pageable);
}
