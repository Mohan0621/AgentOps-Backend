package com.agentops.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agentops.DTO.AiDiagnosisRequest;
import com.agentops.DTO.AiDiagnosisResult;
import com.agentops.DTO.IncidentResponse;
import com.agentops.Services.AiDiagnosisService;
import com.agentops.Services.IncidentService;

@RestController
@RequestMapping("/api/ai")
public class AiDiagnosisController {

    @Autowired
    private AiDiagnosisService aiDiagnosisService;

    @Autowired
    private IncidentService incidentService;

    /**
     * Diagnose on-the-fly from raw logs, title, and description payload
     */
    @PostMapping("/diagnose")
    public ResponseEntity<AiDiagnosisResult> diagnoseDirectly(@RequestBody AiDiagnosisRequest request) {
        AiDiagnosisResult result = aiDiagnosisService.diagnose(request);
        return ResponseEntity.ok(result);
    }

    /**
     * Diagnose an existing incident in the system by ID
     */
    @PostMapping("/incidents/{incidentId}/diagnose")
    public ResponseEntity<AiDiagnosisResult> diagnoseIncident(@PathVariable Long incidentId) {
        IncidentResponse incident = incidentService.getIncidentById(incidentId);

        AiDiagnosisRequest request = new AiDiagnosisRequest(
                incident.getTitle(),
                incident.getDescription(),
                incident.getErrorLogs()
        );

        AiDiagnosisResult result = aiDiagnosisService.diagnose(request);
        return ResponseEntity.ok(result);
    }
}

