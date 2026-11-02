package com.agentops.Repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.agentops.Models.Incident;
import com.agentops.Models.IncidentStatus;
import com.agentops.Models.Severity;
import com.agentops.Models.User;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    @Query("SELECT i FROM Incident i WHERE i.createdBy = :user " +
           "AND (:severity IS NULL OR i.severity = :severity) " +
           "AND (:status IS NULL OR i.status = :status)")
    Page<Incident> findByCreatedByUserWithFilters(
            @Param("user") User user,
            @Param("severity") Severity severity,
            @Param("status") IncidentStatus status,
            Pageable pageable);

    @Query("SELECT i FROM Incident i WHERE " +
           "(:severity IS NULL OR i.severity = :severity) " +
           "AND (:status IS NULL OR i.status = :status)")
    Page<Incident> findAllWithFilters(
            @Param("severity") Severity severity,
            @Param("status") IncidentStatus status,
            Pageable pageable);
}

