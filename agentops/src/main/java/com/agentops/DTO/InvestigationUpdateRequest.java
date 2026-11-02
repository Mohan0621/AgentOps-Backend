package com.agentops.DTO;

import com.agentops.Models.InvestigationStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvestigationUpdateRequest {

    private String title;

    private String notes;

    private String hypothesis;

    private String attachedLogs;

    private String rootCauseAnalysis;

    private String resolutionSteps;

    private InvestigationStatus progress;
}

