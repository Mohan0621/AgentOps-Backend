package com.agentops.Repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.agentops.Models.Investigation;

@Repository
public interface InvestigationRepository extends JpaRepository<Investigation, Long> {
    List<Investigation> findByIncidentIdOrderByCreatedAtDesc(Long incidentId);
}

