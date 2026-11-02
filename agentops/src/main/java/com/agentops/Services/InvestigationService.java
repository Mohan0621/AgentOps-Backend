package com.agentops.Services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agentops.DTO.CommentCreateRequest;
import com.agentops.DTO.CommentResponse;
import com.agentops.DTO.InvestigationCreateRequest;
import com.agentops.DTO.InvestigationResponse;
import com.agentops.DTO.InvestigationUpdateRequest;
import com.agentops.Models.Comment;
import com.agentops.Models.Incident;
import com.agentops.Models.IncidentStatus;
import com.agentops.Models.Investigation;
import com.agentops.Models.InvestigationStatus;
import com.agentops.Models.User;
import com.agentops.Repositories.AuthRepo;
import com.agentops.Repositories.CommentRepository;
import com.agentops.Repositories.IncidentRepository;
import com.agentops.Repositories.InvestigationRepository;

@Service
public class InvestigationService {

    @Autowired
    private InvestigationRepository investigationRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private AuthRepo authRepo;

    private User getUserByEmail(String email) {
        return authRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    @Transactional
    public InvestigationResponse createInvestigation(Long incidentId, InvestigationCreateRequest request, String currentUserEmail) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident not found with ID: " + incidentId));

        User investigator = getUserByEmail(currentUserEmail);

        Investigation investigation = new Investigation();
        investigation.setTitle(request.getTitle());
        investigation.setNotes(request.getNotes());
        investigation.setHypothesis(request.getHypothesis());
        investigation.setAttachedLogs(request.getAttachedLogs());
        investigation.setRootCauseAnalysis(request.getRootCauseAnalysis());
        investigation.setResolutionSteps(request.getResolutionSteps());
        investigation.setProgress(request.getProgress() != null ? request.getProgress() : InvestigationStatus.STARTED);
        investigation.setIncident(incident);
        investigation.setInvestigatedBy(investigator);

        // When investigation starts, automatically advance the parent incident status if still OPEN
        if (incident.getStatus() == IncidentStatus.OPEN) {
            incident.setStatus(IncidentStatus.INVESTIGATING);
            incidentRepository.save(incident);
        }

        Investigation saved = investigationRepository.save(investigation);
        return InvestigationResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<InvestigationResponse> getInvestigationsByIncident(Long incidentId) {
        if (!incidentRepository.existsById(incidentId)) {
            throw new RuntimeException("Incident not found with ID: " + incidentId);
        }
        return investigationRepository.findByIncidentIdOrderByCreatedAtDesc(incidentId)
                .stream()
                .map(InvestigationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InvestigationResponse getInvestigationById(Long investigationId) {
        Investigation investigation = investigationRepository.findById(investigationId)
                .orElseThrow(() -> new RuntimeException("Investigation not found with ID: " + investigationId));
        return InvestigationResponse.fromEntity(investigation);
    }

    @Transactional
    public InvestigationResponse updateInvestigation(Long investigationId, InvestigationUpdateRequest request) {
        Investigation investigation = investigationRepository.findById(investigationId)
                .orElseThrow(() -> new RuntimeException("Investigation not found with ID: " + investigationId));

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            investigation.setTitle(request.getTitle());
        }
        if (request.getNotes() != null) {
            investigation.setNotes(request.getNotes());
        }
        if (request.getHypothesis() != null) {
            investigation.setHypothesis(request.getHypothesis());
        }
        if (request.getAttachedLogs() != null) {
            investigation.setAttachedLogs(request.getAttachedLogs());
        }
        if (request.getRootCauseAnalysis() != null) {
            investigation.setRootCauseAnalysis(request.getRootCauseAnalysis());
        }
        if (request.getResolutionSteps() != null) {
            investigation.setResolutionSteps(request.getResolutionSteps());
        }
        if (request.getProgress() != null) {
            investigation.setProgress(request.getProgress());
        }

        Investigation updated = investigationRepository.save(investigation);
        return InvestigationResponse.fromEntity(updated);
    }

    @Transactional
    public CommentResponse addComment(Long investigationId, CommentCreateRequest request, String currentUserEmail) {
        Investigation investigation = investigationRepository.findById(investigationId)
                .orElseThrow(() -> new RuntimeException("Investigation not found with ID: " + investigationId));

        User author = getUserByEmail(currentUserEmail);

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setInvestigation(investigation);
        comment.setAuthor(author);

        Comment saved = commentRepository.save(comment);
        return CommentResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long investigationId) {
        if (!investigationRepository.existsById(investigationId)) {
            throw new RuntimeException("Investigation not found with ID: " + investigationId);
        }
        return commentRepository.findByInvestigationIdOrderByCreatedAtAsc(investigationId)
                .stream()
                .map(CommentResponse::fromEntity)
                .collect(Collectors.toList());
    }
}

