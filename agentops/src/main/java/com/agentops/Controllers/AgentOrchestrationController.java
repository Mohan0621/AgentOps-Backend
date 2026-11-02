package com.agentops.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agentops.DTO.MultiAgentInvestigationReport;
import com.agentops.Services.MultiAgentOrchestratorService;

@RestController
@RequestMapping("/api/agents")
public class AgentOrchestrationController {

    @Autowired
    private MultiAgentOrchestratorService orchestratorService;

    /**
     * Trigger the full 6-Agent LangGraph Investigation Workflow on an incident
     */
    @PostMapping("/incidents/{incidentId}/investigate")
    public ResponseEntity<MultiAgentInvestigationReport> runMultiAgentInvestigation(
            @PathVariable Long incidentId) {
        MultiAgentInvestigationReport report = orchestratorService.runInvestigationPipeline(incidentId);
        return ResponseEntity.ok(report);
    }
}

