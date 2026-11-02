package com.agentops.DTO;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.agentops.Models.Investigation;
import com.agentops.Models.InvestigationStatus;
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
public class InvestigationResponse {

    private Long id;
    private Long incidentId;
    private String title;
    private String notes;
    private String hypothesis;
    private String attachedLogs;
    private String rootCauseAnalysis;
    private String resolutionSteps;
    private InvestigationStatus progress;
    private boolean aiGenerated;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long investigatedById;
    private String investigatedByEmail;

    private List<CommentResponse> comments;

    public static InvestigationResponse fromEntity(Investigation inv) {
        if (inv == null)
            return null;

        List<CommentResponse> commentList = inv.getComments() != null
                ? inv.getComments().stream().map(CommentResponse::fromEntity).collect(Collectors.toList())
                : Collections.emptyList();

        return InvestigationResponse.builder()
                .id(inv.getId())
                .incidentId(inv.getIncident() != null ? inv.getIncident().getId() : null)
                .title(inv.getTitle())
                .notes(inv.getNotes())
                .hypothesis(inv.getHypothesis())
                .attachedLogs(inv.getAttachedLogs())
                .rootCauseAnalysis(inv.getRootCauseAnalysis())
                .resolutionSteps(inv.getResolutionSteps())
                .progress(inv.getProgress())
                .aiGenerated(inv.isAiGenerated())
                .createdAt(inv.getCreatedAt())
                .updatedAt(inv.getUpdatedAt())
                .investigatedById(inv.getInvestigatedBy() != null ? inv.getInvestigatedBy().getId() : null)
                .investigatedByEmail(inv.getInvestigatedBy() != null ? inv.getInvestigatedBy().getEmail() : null)
                .comments(commentList)
                .build();
    }
}
