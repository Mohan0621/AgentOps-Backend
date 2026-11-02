package com.agentops.DTO;

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
public class DashboardAnalyticsResponse {

    // 1. Core Volume Stats
    private long totalIncidents;
    private long openIncidents;
    private long investigatingIncidents;
    private long resolvedIncidents;
    private long closedIncidents;
    private long criticalIncidents;

    // 2. Performance Metrics
    private double averageResolutionTimeHours; // Mean Time to Resolve (MTTR)
    private double resolutionRatePercentage;

    // 3. Distribution & Trends
    private Map<String, Long> incidentsBySeverity; // LOW, MEDIUM, HIGH, CRITICAL
    private Map<String, Long> incidentsByStatus;   // OPEN, INVESTIGATING, RESOLVED, CLOSED
    private Map<String, Long> incidentsByDay;      // Date string -> count (e.g. "2026-11-02": 5)

    // 4. AI Investigation Success Metrics
    private AiInvestigationMetrics aiMetrics;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AiInvestigationMetrics {
        private long totalAiInvestigations;
        private long manualInvestigations;
        private double aiCoveragePercentage;
        private double aiRootCauseFoundRatePercentage;
        private double averageAgentExecutionTimeSeconds;
    }
}

