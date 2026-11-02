package com.agentops.Controllers;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.agentops.DTO.IncidentCreateRequest;
import com.agentops.DTO.IncidentResponse;
import com.agentops.DTO.IncidentUpdateRequest;
import com.agentops.Models.IncidentStatus;
import com.agentops.Models.Severity;
import com.agentops.Services.IncidentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    @Autowired
    private IncidentService incidentService;

    /**
     * 1. Create Incident
     */
    @PostMapping
    public ResponseEntity<IncidentResponse> createIncident(
            @Valid @RequestBody IncidentCreateRequest request,
            Principal principal) {
        IncidentResponse created = incidentService.createIncident(request, principal.getName());
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * 2. View Incidents - Get incident by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<IncidentResponse> getIncidentById(@PathVariable Long id) {
        return ResponseEntity.ok(incidentService.getIncidentById(id));
    }

    /**
     * 2. View Incidents - Get incidents created by current user (with filtering, pagination & sorting)
     * e.g., /api/incidents/my?severity=HIGH&status=OPEN&page=0&size=10&sort=createdAt,desc
     */
    @GetMapping("/my")
    public ResponseEntity<Page<IncidentResponse>> getMyIncidents(
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) IncidentStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Principal principal) {
        return ResponseEntity.ok(incidentService.getMyIncidents(principal.getName(), severity, status, pageable));
    }

    /**
     * 2. View Incidents - Get all incidents (with filtering, pagination & sorting)
     * e.g., /api/incidents?severity=CRITICAL&status=INVESTIGATING&page=0&size=10&sort=createdAt,desc
     */
    @GetMapping
    public ResponseEntity<Page<IncidentResponse>> getAllIncidents(
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) IncidentStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(incidentService.getAllIncidents(severity, status, pageable));
    }

    /**
     * 3. Update Incident (Title, Description, Error logs, Severity, Status, Assignee)
     */
    @PutMapping("/{id}")
    public ResponseEntity<IncidentResponse> updateIncident(
            @PathVariable Long id,
            @RequestBody IncidentUpdateRequest request,
            Principal principal) {
        return ResponseEntity.ok(incidentService.updateIncident(id, request, principal.getName()));
    }

    /**
     * 4. Delete Incident (Restricted to its creator)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteIncident(
            @PathVariable Long id,
            Principal principal) {
        incidentService.deleteIncident(id, principal.getName());
        return ResponseEntity.ok("Incident deleted successfully");
    }
}

