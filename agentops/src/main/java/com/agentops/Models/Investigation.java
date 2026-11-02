package com.agentops.Models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "investigations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Investigation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String hypothesis;

    @Lob
    @Column(name = "attached_logs", columnDefinition = "LONGTEXT")
    private String attachedLogs;

    @Lob
    @Column(name = "root_cause_analysis", columnDefinition = "LONGTEXT")
    private String rootCauseAnalysis;

    @Lob
    @Column(name = "resolution_steps", columnDefinition = "LONGTEXT")
    private String resolutionSteps;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvestigationStatus progress = InvestigationStatus.STARTED;

    @Column(name = "ai_generated")
    private boolean aiGenerated = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationship: INVESTIGATION N -> 1 INCIDENT
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    @JsonIgnore
    private Incident incident;

    // User who owns or is driving this investigation
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investigated_by_user_id")
    private User investigatedBy;

    // Relationship: INVESTIGATION 1 -> N COMMENTS (Team collaboration & timeline)
    @OneToMany(mappedBy = "investigation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.progress == null) {
            this.progress = InvestigationStatus.STARTED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
