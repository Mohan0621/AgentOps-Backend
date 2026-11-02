package com.agentops.DTO;

import java.time.LocalDateTime;

import com.agentops.Models.Incident;
import com.agentops.Models.Severity;
import com.agentops.Models.IncidentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentResponse {

    private Long id;
    private String title;
    private String description;
    private String errorLogs;
    private Severity severity;
    private IncidentStatus status;
    private String incidentType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long createdById;
    private String createdByEmail;

    private Long assignedToId;
    private String assignedToEmail;

    public static IncidentResponse fromEntity(Incident incident) {
        if (incident == null) return null;

        IncidentResponse response = new IncidentResponse();
        response.setId(incident.getId());
        response.setTitle(incident.getTitle());
        response.setDescription(incident.getDescription());
        response.setErrorLogs(incident.getErrorLogs());
        response.setSeverity(incident.getSeverity());
        response.setStatus(incident.getStatus());
        response.setIncidentType(incident.getIncidentType());
        response.setCreatedAt(incident.getCreatedAt());
        response.setUpdatedAt(incident.getUpdatedAt());

        if (incident.getCreatedBy() != null) {
            response.setCreatedById(incident.getCreatedBy().getId());
            response.setCreatedByEmail(incident.getCreatedBy().getEmail());
        }

        if (incident.getAssignedTo() != null) {
            response.setAssignedToId(incident.getAssignedTo().getId());
            response.setAssignedToEmail(incident.getAssignedTo().getEmail());
        }

        return response;
    }
}

