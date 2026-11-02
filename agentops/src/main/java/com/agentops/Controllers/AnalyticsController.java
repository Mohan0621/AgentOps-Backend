package com.agentops.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agentops.DTO.DashboardAnalyticsResponse;
import com.agentops.Services.AnalyticsService;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    /**
     * Get aggregate dashboard metrics:
     * - Total incidents
     * - Open vs resolved incidents
     * - Critical incidents
     * - Average resolution time (MTTR)
     * - Daily incident trends
     * - AI investigation success & coverage metrics
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardAnalyticsResponse> getDashboardMetrics() {
        return ResponseEntity.ok(analyticsService.getDashboardMetrics());
    }
}

