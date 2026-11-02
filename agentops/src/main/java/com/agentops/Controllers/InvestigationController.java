package com.agentops.Controllers;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agentops.DTO.CommentCreateRequest;
import com.agentops.DTO.CommentResponse;
import com.agentops.DTO.InvestigationCreateRequest;
import com.agentops.DTO.InvestigationResponse;
import com.agentops.DTO.InvestigationUpdateRequest;
import com.agentops.Services.InvestigationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class InvestigationController {

    @Autowired
    private InvestigationService investigationService;

    /**
     * Start/Create an investigation for an incident
     */
    @PostMapping("/incidents/{incidentId}/investigations")
    public ResponseEntity<InvestigationResponse> createInvestigation(
            @PathVariable Long incidentId,
            @Valid @RequestBody InvestigationCreateRequest request,
            Principal principal) {
        InvestigationResponse response = investigationService.createInvestigation(incidentId, request, principal.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get all investigations for an incident (investigation history)
     */
    @GetMapping("/incidents/{incidentId}/investigations")
    public ResponseEntity<List<InvestigationResponse>> getInvestigationsByIncident(
            @PathVariable Long incidentId) {
        return ResponseEntity.ok(investigationService.getInvestigationsByIncident(incidentId));
    }

    /**
     * Get a specific investigation with notes, hypothesis, logs, and comments
     */
    @GetMapping("/investigations/{investigationId}")
    public ResponseEntity<InvestigationResponse> getInvestigationById(
            @PathVariable Long investigationId) {
        return ResponseEntity.ok(investigationService.getInvestigationById(investigationId));
    }

    /**
     * Update investigation progress, notes, hypotheses, and attached logs
     */
    @PutMapping("/investigations/{investigationId}")
    public ResponseEntity<InvestigationResponse> updateInvestigation(
            @PathVariable Long investigationId,
            @RequestBody InvestigationUpdateRequest request) {
        return ResponseEntity.ok(investigationService.updateInvestigation(investigationId, request));
    }

    /**
     * Team collaboration: Post comment / findings to investigation timeline
     */
    @PostMapping("/investigations/{investigationId}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long investigationId,
            @Valid @RequestBody CommentCreateRequest request,
            Principal principal) {
        CommentResponse response = investigationService.addComment(investigationId, request, principal.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * View all collaboration comments on an investigation
     */
    @GetMapping("/investigations/{investigationId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(
            @PathVariable Long investigationId) {
        return ResponseEntity.ok(investigationService.getComments(investigationId));
    }
}

