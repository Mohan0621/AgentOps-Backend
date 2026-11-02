package com.agentops.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agentops.DTO.IncidentCreateRequest;
import com.agentops.DTO.IncidentResponse;
import com.agentops.DTO.IncidentUpdateRequest;
import com.agentops.Models.Incident;
import com.agentops.Models.IncidentStatus;
import com.agentops.Models.Severity;
import com.agentops.Models.User;
import com.agentops.Repositories.AuthRepo;
import com.agentops.Repositories.IncidentRepository;

@Service
public class IncidentService {

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private AuthRepo authRepo;

    private User getUserByEmail(String email) {
        return authRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    @Transactional
    public IncidentResponse createIncident(IncidentCreateRequest request, String currentUserEmail) {
        User creator = getUserByEmail(currentUserEmail);

        Incident incident = new Incident();
        incident.setTitle(request.getTitle());
        incident.setDescription(request.getDescription());
        incident.setErrorLogs(request.getErrorLogs());
        incident.setSeverity(request.getSeverity() != null ? request.getSeverity() : Severity.MEDIUM);
        incident.setStatus(request.getStatus() != null ? request.getStatus() : IncidentStatus.OPEN);
        incident.setIncidentType(request.getIncidentType());
        incident.setCreatedBy(creator);

        if (request.getAssignedToUserId() != null) {
            User assignee = authRepo.findById(request.getAssignedToUserId())
                    .orElseThrow(() -> new RuntimeException("Assignee user not found with ID: " + request.getAssignedToUserId()));
            incident.setAssignedTo(assignee);
        }

        Incident saved = incidentRepository.save(incident);
        return IncidentResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public IncidentResponse getIncidentById(Long id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incident not found with ID: " + id));
        return IncidentResponse.fromEntity(incident);
    }

    @Transactional(readOnly = true)
    public Page<IncidentResponse> getMyIncidents(String currentUserEmail,
                                                Severity severity,
                                                IncidentStatus status,
                                                Pageable pageable) {
        User user = getUserByEmail(currentUserEmail);
        return incidentRepository.findByCreatedByUserWithFilters(user, severity, status, pageable)
                .map(IncidentResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<IncidentResponse> getAllIncidents(Severity severity,
                                                 IncidentStatus status,
                                                 Pageable pageable) {
        return incidentRepository.findAllWithFilters(severity, status, pageable)
                .map(IncidentResponse::fromEntity);
    }

    @Transactional
    public IncidentResponse updateIncident(Long id, IncidentUpdateRequest request, String currentUserEmail) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incident not found with ID: " + id));

        User currentUser = getUserByEmail(currentUserEmail);

        // Authorization check: only the creator or the assigned user can update the incident
        boolean isCreator = incident.getCreatedBy() != null && incident.getCreatedBy().getId().equals(currentUser.getId());
        boolean isAssignee = incident.getAssignedTo() != null && incident.getAssignedTo().getId().equals(currentUser.getId());

        if (!isCreator && !isAssignee) {
            throw new RuntimeException("Forbidden: You are not authorized to update this incident. Only the creator or the assigned owner can make changes.");
        }

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            incident.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            incident.setDescription(request.getDescription());
        }
        if (request.getErrorLogs() != null) {
            incident.setErrorLogs(request.getErrorLogs());
        }
        if (request.getSeverity() != null) {
            incident.setSeverity(request.getSeverity());
        }
        if (request.getStatus() != null) {
            incident.setStatus(request.getStatus());
        }
        if (request.getIncidentType() != null) {
            incident.setIncidentType(request.getIncidentType());
        }
        if (request.getAssignedToUserId() != null) {
            User assignee = authRepo.findById(request.getAssignedToUserId())
                    .orElseThrow(() -> new RuntimeException("Assignee user not found with ID: " + request.getAssignedToUserId()));
            incident.setAssignedTo(assignee);
        }

        Incident updated = incidentRepository.save(incident);
        return IncidentResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteIncident(Long id, String currentUserEmail) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incident not found with ID: " + id));

        User currentUser = getUserByEmail(currentUserEmail);

        // Authorization check: only the creator can delete
        if (!incident.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Forbidden: You are not authorized to delete this incident");
        }

        incidentRepository.delete(incident);
    }
}

