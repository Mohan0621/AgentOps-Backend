package com.agentops.DTO;

import com.agentops.Models.Severity;
import com.agentops.Models.IncidentStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IncidentUpdateRequest {

    private String title;

    private String description;

    private String errorLogs;

    private Severity severity;

    private IncidentStatus status;

    private String incidentType;

    private Long assignedToUserId;
}

