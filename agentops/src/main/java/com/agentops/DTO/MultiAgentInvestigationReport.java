package com.agentops.DTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
public class MultiAgentInvestigationReport {

    private Long incidentId;
    private LocalDateTime timestamp;

    // 1. Log Analysis Agent Output
    private LogAnalysisResult logAnalysis;

    // 2. Error Classification Agent Output
    private ErrorClassificationResult errorClassification;

    // 3. Severity Agent Output
    private SeverityEvaluationResult severityEvaluation;

    // 4. Root Cause Agent Output
    private RootCauseResult rootCauseAnalysis;

    // 5. Resolution Agent Output
    private ResolutionResult resolutionPlan;

    // 6. Report Agent Output
    private FinalSummaryReport finalReport;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LogAnalysisResult {
        private String keyAnomaly;
        private List<String> extractedStackTraces;
        private String failingMethod;
        private Map<String, String> logMetadata;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ErrorClassificationResult {
        private String primaryCategory; // e.g. DATABASE, MEMORY, NETWORK, AUTHENTICATION, RUNTIME_NULL
        private String subType;
        private boolean isTransient;
        private double confidenceScore;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SeverityEvaluationResult {
        private String assessedSeverity; // LOW, MEDIUM, HIGH, CRITICAL
        private String blastRadius;
        private String userImpact;
        private String justification;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RootCauseResult {
        private String primaryHypothesis;
        private String underlyingCause;
        private String affectedComponent;
        private List<String> contributingFactors;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResolutionResult {
        private String immediateMitigation;
        private List<String> remediationSteps;
        private String suggestedCodePatch;
        private String preventativeAction;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FinalSummaryReport {
        private String executiveSummary;
        private String incidentTimeline;
        private String nextSteps;
        private String formattedMarkdownReport;
    }
}

