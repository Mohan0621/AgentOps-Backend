package com.agentops.DTO;

import com.agentops.Models.Severity;
import com.agentops.Models.IncidentStatus;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IncidentCreateRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private String errorLogs;

    private Severity severity = Severity.MEDIUM;

    private IncidentStatus status = IncidentStatus.OPEN;

    private String incidentType;

    private Long assignedToUserId;
}

