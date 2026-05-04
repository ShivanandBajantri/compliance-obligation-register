package com.internship.tool.repository;

import com.internship.tool.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import jakarta.persistence.QueryHint;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Paginated audit log — avoids loading the entire table into memory.
     * The idx_audit_changed_at index makes ORDER BY changed_at efficient.
     */
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    Page<AuditLog> findAll(Pageable pageable);

    /**
     * Fetch all audit entries for a specific entity (e.g. to show history
     * for one obligation). Uses the idx_audit_entity_id index.
     */
    @Query("SELECT a FROM AuditLog a WHERE a.entityType = :entityType AND a.entityId = :entityId " +
           "ORDER BY a.changedAt DESC")
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    List<AuditLog> findByEntityTypeAndEntityId(@Param("entityType") String entityType,
                                                @Param("entityId") Long entityId);
}
