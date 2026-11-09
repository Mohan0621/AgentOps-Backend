package com.agentops.UI.Views;

import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.agentops.UI.ApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Inside Workspace: Platform Telemetry & AI Metrics Dashboard aligned with serko.ai's Light Editorial system.
 * Consumes GET /api/analytics/dashboard cleanly.
 */
public class AnalyticsView extends ScrollPane {

    private final ObjectMapper mapper = new ObjectMapper();

    private Label lblTotal, lblOpen, lblResolved, lblCritical;
    private Label lblMttr, lblResRate, lblAiCoverage, lblAiCount;
    private Label lblExecTime, lblManualCount;
    private ProgressBar pbAiCoverage, pbRootRate, pbResRate;
    private VBox distributionList;
    private Label lastUpdatedLbl;

    public AnalyticsView() {
        buildUI();
    }

    private void buildUI() {
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        getStyleClass().add("scroll-pane");
        setStyle("-fx-background-color: #FFFFFF;");

        VBox page = new VBox(24);
        page.setPadding(new Insets(28, 36, 48, 36));
        page.setAlignment(Pos.TOP_CENTER);
        page.setStyle("-fx-background-color: #FFFFFF;");

        VBox inner = new VBox(24);
        inner.setMaxWidth(1120);
        inner.getChildren().addAll(
            buildHeader(),
            buildKpiRows(),
            buildBreakdownAndAiRow(),
            buildPipelineStrip(),
            buildFooter()
        );

        page.getChildren().add(inner);
        setContent(page);
    }

    // ── Header ────────────────────────────────────────────────────
    private VBox buildHeader() {
        VBox hdr = new VBox(6);

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox titles = new VBox(4);
        Label eye = UITheme.eyebrowLabel("Platform Telemetry  ·  GET /api/analytics/dashboard");
        Label title = new Label("Operational Analytics");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #000000;");
        titles.getChildren().addAll(eye, title);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnRefresh = UITheme.serkoBlackPill("Refresh Metrics", false);
        btnRefresh.setPadding(new Insets(9, 20, 9, 20));
        btnRefresh.setOnAction(e -> refreshDashboard());

        row.getChildren().addAll(titles, spacer, btnRefresh);

        Label sub = new Label("Real-time incident volume, MTTR resolution speed, severity distribution, and AI investigation coverage.");
        sub.setStyle(UITheme.subHeadingStyle());

        hdr.getChildren().addAll(row, sub);
        return hdr;
    }

    // ── KPI Card Rows (4 + 4 clean grid) ──────────────────────────
    private VBox buildKpiRows() {
        VBox wrap = new VBox(14);

        HBox row1 = new HBox(14);
        lblTotal    = createWideStatCard("Total Incidents", "—", UITheme.GRAD_BLUE, row1);
        lblOpen     = createWideStatCard("Open Backlog",    "—", UITheme.COLOR_DANGER, row1);
        lblResolved = createWideStatCard("Resolved",        "—", UITheme.POS_DOT, row1);
        lblCritical = createWideStatCard("Critical Alerts", "—", UITheme.COLOR_WARNING, row1);

        HBox row2 = new HBox(14);
        lblMttr       = createWideStatCard("Avg MTTR",          "—", UITheme.GRAD_PURPLE, row2);
        lblResRate    = createWideStatCard("Resolution Rate",   "—", UITheme.POS_DOT, row2);
        lblAiCoverage = createWideStatCard("AI Coverage",       "—", UITheme.GRAD_PINK, row2);
        lblAiCount    = createWideStatCard("AI Investigations", "—", UITheme.GRAD_ORANGE, row2);

        wrap.getChildren().addAll(row1, row2);
        return wrap;
    }

    private Label createWideStatCard(String title, String val, String accent, HBox parent) {
        Label lbl = UITheme.createStatCard(title, val, accent, parent);
        VBox card = (VBox) lbl.getParent();
        card.setPrefWidth(220);
        card.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(card, Priority.ALWAYS);
        return lbl;
    }

    // ── Distribution + AI Performance Cards ───────────────────────
    private HBox buildBreakdownAndAiRow() {
        HBox row = new HBox(18);
        row.setFillHeight(true);

        // Left Card: Severity & Status Breakdown
        VBox distCard = new VBox(14);
        distCard.setPadding(new Insets(24));
        distCard.setStyle(UITheme.cardStyle());
        distCard.setEffect(UITheme.softCardShadow());
        HBox.setHgrow(distCard, Priority.ALWAYS);

        Label dEye = UITheme.eyebrowLabel("Distribution");
        Label dTitle = new Label("Incidents by Severity & Status");
        dTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #000000;");

        distributionList = new VBox(10);
        renderDistributions(0, mapper.createObjectNode(), mapper.createObjectNode());
        distCard.getChildren().addAll(dEye, dTitle, distributionList);

        // Right Card: Ice Sky AI Quality Card
        VBox aiCard = new VBox(16);
        aiCard.setPrefWidth(420);
        aiCard.setMinWidth(380);
        aiCard.setPadding(new Insets(24));
        aiCard.setStyle("-fx-background-color: " + UITheme.BG_ICE + "; -fx-background-radius: 22;");

        Label aBadge = UITheme.positiveBadge("Groq + Gemini Core");
        Label aTitle = new Label("AI Investigation Performance");
        aTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #000000;");

        pbAiCoverage = styledBar();
        VBox m1 = metricBarBlock("AI Incident Coverage", pbAiCoverage);

        pbRootRate = styledBar();
        VBox m2 = metricBarBlock("Root Cause Isolation Rate", pbRootRate);

        pbResRate = styledBar();
        VBox m3 = metricBarBlock("Overall Resolution Rate", pbResRate);

        HBox subStats = new HBox(12);
        VBox statA = miniWhiteStat("AVG PIPELINE SPEED", "—");
        lblExecTime = (Label) statA.getChildren().get(1);
        VBox statB = miniWhiteStat("MANUAL AUDITS", "—");
        lblManualCount = (Label) statB.getChildren().get(1);
        HBox.setHgrow(statA, Priority.ALWAYS);
        HBox.setHgrow(statB, Priority.ALWAYS);
        subStats.getChildren().addAll(statA, statB);

        aiCard.getChildren().addAll(aBadge, aTitle, m1, m2, m3, subStats);

        row.getChildren().addAll(distCard, aiCard);
        return row;
    }

    private ProgressBar styledBar() {
        ProgressBar pb = new ProgressBar(0);
        pb.setMaxWidth(Double.MAX_VALUE);
        pb.setPrefHeight(9);
        return pb;
    }

    private VBox metricBarBlock(String title, ProgressBar pb) {
        VBox b = new VBox(6);
        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 600; -fx-text-fill: #1F2937;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label val = new Label("0.0%");
        val.setStyle("-fx-font-size: 12.5px; -fx-font-weight: bold; -fx-text-fill: #000000;");
        top.getChildren().addAll(t, sp, val);
        b.getChildren().addAll(top, pb);
        return b;
    }

    private VBox miniWhiteStat(String tag, String value) {
        VBox c = new VBox(4);
        c.setPadding(new Insets(12, 14, 12, 14));
        c.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 14;");
        Label t = new Label(tag);
        t.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #71717A;");
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #000000;");
        c.getChildren().addAll(t, v);
        return c;
    }

    // ── Horizontal 6-Agent Pipeline Card with Serko Dotted Trajectory ──
    private VBox buildPipelineStrip() {
        VBox card = new VBox(14);
        card.setPadding(new Insets(22, 24, 22, 24));
        card.setStyle(UITheme.softCardStyle());

        HBox head = new HBox(10);
        head.setAlignment(Pos.CENTER_LEFT);
        head.getChildren().addAll(UITheme.eyebrowLabel("Multi-Agent Orchestration"), UITheme.positiveBadge("Active Architecture"));

        HBox stages = new HBox(10);
        stages.setAlignment(Pos.CENTER);

        String[] names = {
            "1. Log Analysis", "2. Error Classify", "3. Severity Score",
            "4. Root Cause", "5. Resolution", "6. Final Report"
        };
        for (int i = 0; i < names.length; i++) {
            Label pill = new Label(names[i]);
            pill.setStyle(
                "-fx-background-color: #FFFFFF; -fx-text-fill: #000000; -fx-font-size: 12px; " +
                "-fx-font-weight: bold; -fx-padding: 8 14; -fx-background-radius: 999;"
            );
            stages.getChildren().add(pill);
            if (i < names.length - 1) {
                stages.getChildren().add(UITheme.dottedDivider(24));
            }
        }

        card.getChildren().addAll(head, stages);
        return card;
    }

    // ── Footer ────────────────────────────────────────────────────
    private HBox buildFooter() {
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);
        lastUpdatedLbl = new Label("●  Not yet refreshed");
        lastUpdatedLbl.setStyle("-fx-text-fill: #71717A; -fx-font-size: 11.5px;");
        footer.getChildren().add(lastUpdatedLbl);
        return footer;
    }

    // ── Refresh Metrics from Backend ──────────────────────────────
    public void refreshDashboard() {
        lastUpdatedLbl.setText("●  Refreshing dashboard metrics...");

        new Thread(() -> {
            try {
                HttpResponse<String> res = ApiClient.getDashboardMetrics();
                if (res.statusCode() == 200) {
                    JsonNode root = mapper.readTree(res.body());

                    long   total      = root.path("totalIncidents").asLong(0);
                    long   open       = root.path("openIncidents").asLong(0);
                    long   resolved   = root.path("resolvedIncidents").asLong(0);
                    long   critical   = root.path("criticalIncidents").asLong(0);
                    double mttr       = root.path("averageResolutionTimeHours").asDouble(0.0);
                    double resRate    = root.path("resolutionRatePercentage").asDouble(0.0);

                    JsonNode ai       = root.path("aiMetrics");
                    double aiCov      = ai.path("aiCoveragePercentage").asDouble(0.0);
                    double rootRate   = ai.path("aiRootCauseFoundRatePercentage").asDouble(0.0);
                    double avgSec     = ai.path("averageAgentExecutionTimeSeconds").asDouble(0.0);
                    long   aiCount    = ai.path("totalAiInvestigations").asLong(0);
                    long   manCount   = ai.path("manualInvestigations").asLong(0);

                    JsonNode bySev    = root.path("incidentsBySeverity");
                    JsonNode byStatus = root.path("incidentsByStatus");

                    Platform.runLater(() -> {
                        lblTotal.setText(String.valueOf(total));
                        lblOpen.setText(String.valueOf(open));
                        lblResolved.setText(String.valueOf(resolved));
                        lblCritical.setText(String.valueOf(critical));

                        lblMttr.setText(String.format("%.1f hrs", mttr));
                        lblResRate.setText(String.format("%.1f%%", resRate));
                        lblAiCoverage.setText(String.format("%.1f%%", aiCov));
                        lblAiCount.setText(String.valueOf(aiCount));

                        updateBarBlock(pbAiCoverage, aiCov);
                        updateBarBlock(pbRootRate, rootRate);
                        updateBarBlock(pbResRate, resRate);
                        lblExecTime.setText(String.format("%.1fs", avgSec));
                        lblManualCount.setText(String.valueOf(manCount));

                        renderDistributions(total, bySev, byStatus);

                        lastUpdatedLbl.setText(
                            "●  Last synced  ·  " +
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss, dd MMM yyyy"))
                        );
                    });
                } else {
                    Platform.runLater(() -> lastUpdatedLbl.setText("●  Failed to load analytics (" + res.statusCode() + ")"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> lastUpdatedLbl.setText("●  Connection error: " + e.getMessage()));
            }
        }).start();
    }

    private void updateBarBlock(ProgressBar pb, double pct) {
        double clamped = Math.max(0.0, Math.min(1.0, pct / 100.0));
        pb.setProgress(clamped);
        VBox parent = (VBox) pb.getParent();
        HBox top = (HBox) parent.getChildren().get(0);
        Label valLbl = (Label) top.getChildren().get(2);
        valLbl.setText(String.format("%.1f%%", pct));
    }

    private void renderDistributions(long total, JsonNode bySev, JsonNode byStatus) {
        distributionList.getChildren().clear();
        long denom = Math.max(1, total);

        distributionList.getChildren().add(sectionSubLabel("BY SEVERITY"));
        for (String key : new String[]{"CRITICAL", "HIGH", "MEDIUM", "LOW"}) {
            long c = bySev.path(key).asLong(0);
            distributionList.getChildren().add(distRow(UITheme.severityBadge(key), c, denom));
        }

        distributionList.getChildren().add(sectionSubLabel("BY STATUS"));
        for (String key : new String[]{"OPEN", "INVESTIGATING", "RESOLVED", "CLOSED"}) {
            long c = byStatus.path(key).asLong(0);
            distributionList.getChildren().add(distRow(UITheme.statusBadge(key), c, denom));
        }
    }

    private Label sectionSubLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 10.5px; -fx-font-weight: bold; -fx-text-fill: #9CA3AF; -fx-padding: 6 0 0 0;");
        return l;
    }

    private HBox distRow(Label badge, long count, long total) {
        HBox r = new HBox(12);
        r.setAlignment(Pos.CENTER_LEFT);
        badge.setMinWidth(112);
        badge.setAlignment(Pos.CENTER);

        ProgressBar bar = new ProgressBar((double) count / total);
        bar.setPrefHeight(8);
        bar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(bar, Priority.ALWAYS);

        Label countLbl = new Label(String.valueOf(count));
        countLbl.setMinWidth(32);
        countLbl.setAlignment(Pos.CENTER_RIGHT);
        countLbl.setStyle("-fx-font-size: 12.5px; -fx-font-weight: bold; -fx-text-fill: #000000;");

        r.getChildren().addAll(badge, bar, countLbl);
        return r;
    }
}
