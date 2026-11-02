package com.agentops.DTO;

import com.agentops.Models.InvestigationStatus;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvestigationCreateRequest {

    @NotBlank(message = "Investigation title is required")
    private String title;

    private String notes;

    private String hypothesis;

    private String attachedLogs;

    private String rootCauseAnalysis;

    private String resolutionSteps;

    private InvestigationStatus progress = InvestigationStatus.STARTED;
}

