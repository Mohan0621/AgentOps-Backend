package com.agentops.UI.Views;

import java.net.http.HttpResponse;

import com.agentops.UI.ApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class AnalyticsView extends VBox {

    private final ObjectMapper mapper = new ObjectMapper();
    private Label lblTotalIncidents;
    private Label lblOpenResolved;
    private Label lblCriticalCount;
    private Label lblMttr;
    private Label lblAiCoverage;

    public AnalyticsView() {
        setupUI();
    }

    private void setupUI() {
        setSpacing(20);
        setPadding(new Insets(25));
        setStyle("-fx-background-color: " + UITheme.BG_DARK + ";");

        Label heading = new Label("IncidentIQ Real-Time Operations Telemetry");
        heading.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        HBox cards = new HBox(15);
        lblTotalIncidents = UITheme.createStatCard("Total Incidents", "0", UITheme.ACCENT_PRIMARY, cards);
        lblOpenResolved = UITheme.createStatCard("Open / Resolved", "0 / 0", "#3a0ca3", cards);
        lblCriticalCount = UITheme.createStatCard("Critical Incidents", "0", UITheme.COLOR_DANGER, cards);
        lblMttr = UITheme.createStatCard("Average MTTR", "0.0 hrs", UITheme.ACCENT_PURPLE, cards);
        lblAiCoverage = UITheme.createStatCard("AI Auto-Diagnosis", "0.0%", UITheme.ACCENT_CYAN, cards);

        Button btnRefresh = new Button("↻ Refresh Analytics");
        btnRefresh.setStyle("-fx-background-color: #2b2b40; -fx-text-fill: white; -fx-padding: 8 18;");
        btnRefresh.setOnAction(e -> refreshDashboard());

        getChildren().addAll(heading, cards, btnRefresh);
    }

    public void refreshDashboard() {
        new Thread(() -> {
            try {
                HttpResponse<String> res = ApiClient.getDashboardMetrics();
                if (res.statusCode() == 200) {
                    JsonNode root = mapper.readTree(res.body());
                    long total = root.path("totalIncidents").asLong();
                    long open = root.path("openIncidents").asLong();
                    long resolved = root.path("resolvedIncidents").asLong();
                    long critical = root.path("criticalIncidents").asLong();
                    double mttr = root.path("averageResolutionTimeHours").asDouble();
                    double aiCoverage = root.path("aiMetrics").path("aiCoveragePercentage").asDouble();

                    Platform.runLater(() -> {
                        lblTotalIncidents.setText(String.valueOf(total));
                        lblOpenResolved.setText(open + " / " + resolved);
                        lblCriticalCount.setText(String.valueOf(critical));
                        lblMttr.setText(mttr + " hrs");
                        lblAiCoverage.setText(aiCoverage + "%");
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}

