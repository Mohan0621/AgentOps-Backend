package com.agentops.UI.Views;

import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.agentops.UI.ApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * Inside Workspace: Operate-aligned Operational Telemetry & AI Metrics View.
 * Replaces repetitive 8-box grids with a single 4-figure strip + primary daily volume Canvas chart.
 * Consumes GET /api/analytics/dashboard including incidentsByDay.
 */
public class AnalyticsView extends ScrollPane {

    private final ObjectMapper mapper = new ObjectMapper();

    // 4-figure summary strip labels
    private Label lblTotalVal, lblTotalMeta;
    private Label lblOpenVal, lblOpenMeta;
    private Label lblMttrVal, lblMttrMeta;
    private Label lblAiCovVal, lblAiCovMeta;

    // Daily trend chart state
    private Canvas trendCanvas;
    private Label lblTrendRange;
    private final List<DayPoint> dayPoints = new ArrayList<>();

    // Distributions & AI rates
    private VBox distributionList;
    private ProgressBar pbAiCoverage, pbRootRate, pbResRate;
    private Label lblAiSummaryMeta;
    private Label lastUpdatedLbl;
    private Circle syncDot;

    private record DayPoint(String day, long count) {}

    public AnalyticsView() {
        buildUI();
    }

    private void buildUI() {
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        getStyleClass().add("scroll-pane");
        setStyle("-fx-background-color: " + UITheme.BG_CANVAS + ";");

        VBox page = new VBox(18);
        page.setPadding(new Insets(24, 28, 40, 28));
        page.setAlignment(Pos.TOP_CENTER);
        page.setStyle("-fx-background-color: " + UITheme.BG_CANVAS + ";");

        VBox inner = new VBox(18);
        inner.setMaxWidth(1120);
        inner.getChildren().addAll(
            buildHeader(),
            buildUnifiedMetricStrip(),
            buildAsymmetricChartAndBreakdownRow(),
            buildBottomAiAndPipelineRow(),
            buildFooter()
        );

        page.getChildren().add(inner);
        setContent(page);
    }

    // ── Header ────────────────────────────────────────────────────
    private HBox buildHeader() {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox titles = new VBox(3);
        Label eye = UITheme.wsSectionLabel("Platform telemetry");
        Label title = new Label("Operational analytics");
        title.setStyle(UITheme.headingStyle());
        Label sub = new Label("Incident volume over time, mean resolution latency, severity distribution, and AI coverage.");
        sub.setStyle(UITheme.subHeadingStyle());
        titles.getChildren().addAll(eye, title, sub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnRefresh = UITheme.wsSecondaryBtn("Refresh metrics", UITheme.refreshIcon(14, Color.web("#52525B")));
        btnRefresh.setOnAction(e -> refreshDashboard());

        row.getChildren().addAll(titles, spacer, btnRefresh);
        return row;
    }

    // ── Unified 4-Figure Metric Strip (Single Hairline Container) ─
    private HBox buildUnifiedMetricStrip() {
        HBox strip = new HBox(0);
        strip.setStyle(UITheme.cardStyle());

        VBox c1 = metricCell("Total incidents");
        lblTotalVal  = (Label) c1.getChildren().get(1);
        lblTotalMeta = (Label) c1.getChildren().get(2);

        VBox c2 = metricCell("Open backlog");
        lblOpenVal   = (Label) c2.getChildren().get(1);
        lblOpenMeta  = (Label) c2.getChildren().get(2);

        VBox c3 = metricCell("Avg MTTR");
        lblMttrVal   = (Label) c3.getChildren().get(1);
        lblMttrMeta  = (Label) c3.getChildren().get(2);

        VBox c4 = metricCell("AI coverage");
        lblAiCovVal  = (Label) c4.getChildren().get(1);
        lblAiCovMeta = (Label) c4.getChildren().get(2);

        for (VBox c : new VBox[]{c1, c2, c3, c4}) {
            HBox.setHgrow(c, Priority.ALWAYS);
            c.setMaxWidth(Double.MAX_VALUE);
        }

        strip.getChildren().addAll(
            c1, verticalDivider(),
            c2, verticalDivider(),
            c3, verticalDivider(),
            c4
        );
        return strip;
    }

    private VBox metricCell(String labelText) {
        VBox cell = new VBox(5);
        cell.setPadding(new Insets(16, 20, 16, 20));

        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 12px; -fx-font-weight: 500; -fx-text-fill: #71717A;");

        Label val = new Label("—");
        val.setStyle("-fx-font-family: '" + UITheme.MONO_FAMILY + "', 'Consolas', monospace; -fx-font-size: 24px; -fx-font-weight: 600; -fx-text-fill: #09090B;");

        Label meta = new Label("—");
        meta.setStyle("-fx-font-size: 12px; -fx-text-fill: #52525B;");

        cell.getChildren().addAll(label, val, meta);
        return cell;
    }

    private Region verticalDivider() {
        Region line = new Region();
        line.setMinWidth(1);
        line.setPrefWidth(1);
        line.setMaxWidth(1);
        line.setStyle("-fx-background-color: " + UITheme.WS_BORDER + ";");
        return line;
    }

    // ── Asymmetric Main Row: Wide Trend Chart Left + Breakdown Right ─
    private HBox buildAsymmetricChartAndBreakdownRow() {
        HBox row = new HBox(16);
        row.setFillHeight(true);

        // Left (~64% width): Primary Daily Volume Canvas Chart
        VBox chartCard = new VBox(12);
        chartCard.setPadding(new Insets(18, 20, 18, 20));
        chartCard.setStyle(UITheme.cardStyle());
        HBox.setHgrow(chartCard, Priority.ALWAYS);

        HBox chartHead = new HBox(10);
        chartHead.setAlignment(Pos.CENTER_LEFT);
        VBox cTitles = new VBox(2);
        Label cTitle = new Label("Incident volume by day");
        cTitle.setStyle("-fx-font-size: 14.5px; -fx-font-weight: 600; -fx-text-fill: #09090B;");
        Label cSub = new Label("Daily reported volume plotted from incident timestamps");
        cSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #71717A;");
        cTitles.getChildren().addAll(cTitle, cSub);

        Region cSp = new Region(); HBox.setHgrow(cSp, Priority.ALWAYS);
        lblTrendRange = UITheme.neutralTag("No history");
        chartHead.getChildren().addAll(cTitles, cSp, lblTrendRange);

        StackPane canvasHost = new StackPane();
        canvasHost.setMinHeight(236);
        canvasHost.setPrefHeight(236);
        trendCanvas = new Canvas(600, 236);
        trendCanvas.widthProperty().bind(canvasHost.widthProperty());
        trendCanvas.widthProperty().addListener((obs, oldW, newW) -> drawTrendChart());
        canvasHost.getChildren().add(trendCanvas);

        chartCard.getChildren().addAll(chartHead, canvasHost);

        // Right Narrow Panel: Severity & Status Distribution
        VBox distCard = new VBox(12);
        distCard.setPrefWidth(365);
        distCard.setMinWidth(325);
        distCard.setPadding(new Insets(18, 20, 18, 20));
        distCard.setStyle(UITheme.cardStyle());

        Label dTitle = new Label("Severity & status breakdown");
        dTitle.setStyle("-fx-font-size: 14.5px; -fx-font-weight: 600; -fx-text-fill: #09090B;");

        distributionList = new VBox(9);
        renderDistributions(0, mapper.createObjectNode(), mapper.createObjectNode());
        distCard.getChildren().addAll(dTitle, distributionList);

        row.getChildren().addAll(chartCard, distCard);
        return row;
    }

    // ── Bottom Row: AI Performance + Compact 6-Stage Architecture ─
    private HBox buildBottomAiAndPipelineRow() {
        HBox row = new HBox(16);
        row.setFillHeight(true);

        // AI Performance Metrics
        VBox aiCard = new VBox(12);
        aiCard.setPadding(new Insets(18, 20, 18, 20));
        aiCard.setStyle(UITheme.cardStyle());
        HBox.setHgrow(aiCard, Priority.ALWAYS);

        HBox aiTop = new HBox(10);
        aiTop.setAlignment(Pos.CENTER_LEFT);
        Label aTitle = new Label("AI investigation efficiency");
        aTitle.setStyle("-fx-font-size: 14.5px; -fx-font-weight: 600; -fx-text-fill: #09090B;");
        Region aSp = new Region(); HBox.setHgrow(aSp, Priority.ALWAYS);
        lblAiSummaryMeta = new Label("Avg runtime —  ·  — manual");
        lblAiSummaryMeta.setStyle(UITheme.monoStyle(12, UITheme.INK_MUTED));
        aiTop.getChildren().addAll(aTitle, aSp, lblAiSummaryMeta);

        pbAiCoverage = styledBar();
        VBox m1 = metricBarBlock("AI incident coverage", pbAiCoverage);

        pbRootRate = styledBar();
        VBox m2 = metricBarBlock("Root cause isolation rate", pbRootRate);

        pbResRate = styledBar();
        VBox m3 = metricBarBlock("Overall resolution rate", pbResRate);

        aiCard.getChildren().addAll(aiTop, m1, m2, m3);

        // Compact 6-Agent Reference List
        VBox pipeCard = new VBox(10);
        pipeCard.setPrefWidth(365);
        pipeCard.setMinWidth(325);
        pipeCard.setPadding(new Insets(18, 20, 18, 20));
        pipeCard.setStyle(UITheme.cardStyle());

        HBox pTop = new HBox(8, new Label("Multi-agent pipeline"), new Region());
        ((Label) pTop.getChildren().get(0)).setStyle("-fx-font-size: 14.5px; -fx-font-weight: 600; -fx-text-fill: #09090B;");
        HBox.setHgrow(pTop.getChildren().get(1), Priority.ALWAYS);
        pTop.getChildren().add(UITheme.positiveBadge("6 stages"));

        VBox stagesGrid = new VBox(6);
        String[] stages = {
            "1. Log analysis & frame extraction",
            "2. Error domain classification",
            "3. Severity & blast radius scoring",
            "4. Root cause isolation",
            "5. Remediation & guard patch",
            "6. Markdown report persistence"
        };
        for (String st : stages) {
            Label l = new Label(st);
            l.setStyle("-fx-font-size: 12px; -fx-text-fill: #52525B; -fx-padding: 2 0;");
            stagesGrid.getChildren().add(l);
        }

        pipeCard.getChildren().addAll(pTop, stagesGrid);
        row.getChildren().addAll(aiCard, pipeCard);
        return row;
    }

    private ProgressBar styledBar() {
        ProgressBar pb = new ProgressBar(0);
        pb.setMaxWidth(Double.MAX_VALUE);
        pb.setPrefHeight(7);
        return pb;
    }

    private VBox metricBarBlock(String title, ProgressBar pb) {
        VBox b = new VBox(5);
        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 500; -fx-text-fill: #27272A;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label val = new Label("0.0%");
        val.setStyle(UITheme.monoStyle(12, UITheme.INK_PRIMARY));
        top.getChildren().addAll(t, sp, val);
        b.getChildren().addAll(top, pb);
        return b;
    }

    private HBox buildFooter() {
        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_LEFT);
        syncDot = new Circle(3.2, Color.web(UITheme.INK_DIM));
        lastUpdatedLbl = new Label("Not yet refreshed");
        lastUpdatedLbl.setStyle("-fx-text-fill: #71717A; -fx-font-size: 12px;");
        footer.getChildren().addAll(syncDot, lastUpdatedLbl);
        return footer;
    }

    // ── Backend Sync + Deterministic Rendering Hook ────────────────
    public void refreshDashboard() {
        setSyncState("Refreshing dashboard metrics...", UITheme.INK_DIM);

        new Thread(() -> {
            try {
                HttpResponse<String> res = ApiClient.getDashboardMetrics();
                if (res.statusCode() == 200) {
                    JsonNode root = mapper.readTree(res.body());
                    Platform.runLater(() -> {
                        applyDashboardData(root);
                        setSyncState(
                            "Synced at " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss, dd MMM yyyy")),
                            UITheme.POS_DOT
                        );
                    });
                } else {
                    Platform.runLater(() -> setSyncState("Failed to load analytics (" + res.statusCode() + ")", UITheme.COLOR_DANGER));
                }
            } catch (Exception e) {
                Platform.runLater(() -> setSyncState("Connection error: " + e.getMessage(), UITheme.COLOR_DANGER));
            }
        }).start();
    }

    /** Populates all metrics, distributions, and the Canvas trend chart from a DashboardAnalyticsResponse JSON tree. */
    public void applyDashboardData(JsonNode root) {
        long   total    = root.path("totalIncidents").asLong(0);
        long   open     = root.path("openIncidents").asLong(0);
        long   resolved = root.path("resolvedIncidents").asLong(0);
        long   critical = root.path("criticalIncidents").asLong(0);
        double mttr     = root.path("averageResolutionTimeHours").asDouble(0.0);
        double resRate  = root.path("resolutionRatePercentage").asDouble(0.0);

        JsonNode ai     = root.path("aiMetrics");
        double aiCov    = ai.path("aiCoveragePercentage").asDouble(0.0);
        double rootRate = ai.path("aiRootCauseFoundRatePercentage").asDouble(0.0);
        double avgSec   = ai.path("averageAgentExecutionTimeSeconds").asDouble(0.0);
        long   aiCount  = ai.path("totalAiInvestigations").asLong(0);
        long   manCount = ai.path("manualInvestigations").asLong(0);

        lblTotalVal.setText(String.valueOf(total));
        lblTotalMeta.setText(resolved + " resolved · " + critical + " critical");

        lblOpenVal.setText(String.valueOf(open));
        lblOpenMeta.setText(critical > 0 ? critical + " critical alerts open" : "Active queue backlog");

        lblMttrVal.setText(String.format("%.1fh", mttr));
        lblMttrMeta.setText(String.format("Resolution rate %.1f%%", resRate));

        lblAiCovVal.setText(String.format("%.1f%%", aiCov));
        lblAiCovMeta.setText(aiCount + " AI runs · " + String.format("%.1fs", avgSec) + " avg");

        updateBarBlock(pbAiCoverage, aiCov);
        updateBarBlock(pbRootRate, rootRate);
        updateBarBlock(pbResRate, resRate);
        lblAiSummaryMeta.setText(String.format("Avg runtime %.1fs · %d manual", avgSec, manCount));

        renderDistributions(total, root.path("incidentsBySeverity"), root.path("incidentsByStatus"));

        // Bind backend incidentsByDay Map<String, Long> to Canvas chart
        dayPoints.clear();
        JsonNode byDay = root.path("incidentsByDay");
        if (byDay.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = byDay.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                dayPoints.add(new DayPoint(entry.getKey(), entry.getValue().asLong(0)));
            }
        }
        if (dayPoints.isEmpty()) {
            lblTrendRange.setText("No daily data");
        } else {
            String first = dayPoints.get(0).day();
            String last = dayPoints.get(dayPoints.size() - 1).day();
            lblTrendRange.setText(dayPoints.size() == 1 ? first : first + " – " + last);
        }
        drawTrendChart();
    }

    private void drawTrendChart() {
        double w = trendCanvas.getWidth();
        double h = trendCanvas.getHeight();
        if (w <= 20 || h <= 20) return;

        GraphicsContext g = trendCanvas.getGraphicsContext2D();
        g.clearRect(0, 0, w, h);

        double padL = 36, padR = 16, padT = 16, padB = 28;
        double plotW = w - padL - padR;
        double plotH = h - padT - padB;
        if (plotW <= 20 || plotH <= 20) return;

        if (dayPoints.isEmpty()) {
            // Subtle baseline + clean empty message
            g.setStroke(Color.web("#EAEAEA"));
            g.setLineWidth(1.0);
            g.strokeLine(padL, padT + plotH, w - padR, padT + plotH);

            g.setFill(Color.web("#71717A"));
            g.setFont(Font.font(UITheme.FONT_FAMILY, 13));
            g.setTextAlign(TextAlignment.CENTER);
            g.setTextBaseline(VPos.CENTER);
            g.fillText("No daily incident history recorded yet", padL + plotW * 0.5, padT + plotH * 0.48);
            return;
        }

        long maxCount = 1;
        for (DayPoint dp : dayPoints) {
            maxCount = Math.max(maxCount, dp.count());
        }
        long yCeiling = Math.max(4, ((maxCount + 3) / 4) * 4);

        // Horizontal hairline gridlines & tabular mono Y-ticks
        g.setFont(Font.font(UITheme.MONO_FAMILY, 11));
        g.setTextAlign(TextAlignment.RIGHT);
        g.setTextBaseline(VPos.CENTER);
        for (int step = 0; step <= 4; step++) {
            double frac = step / 4.0;
            double y = padT + plotH - frac * plotH;
            long val = Math.round(frac * yCeiling);

            g.setStroke(step == 0 ? Color.web("#E4E4E7") : Color.web("#F4F4F5"));
            g.setLineWidth(1.0);
            g.strokeLine(padL, y, w - padR, y);

            g.setFill(Color.web("#71717A"));
            g.fillText(String.valueOf(val), padL - 8, y);
        }

        int n = dayPoints.size();
        double slotW = plotW / Math.max(n, 1);
        double barW = Math.min(34, Math.max(12, slotW * 0.44));

        double[] lineX = new double[n];
        double[] lineY = new double[n];

        for (int i = 0; i < n; i++) {
            DayPoint dp = dayPoints.get(i);
            double cx = padL + (i + 0.5) * slotW;
            double barH = (dp.count() / (double) yCeiling) * plotH;
            double topY = padT + plotH - barH;
            lineX[i] = cx;
            lineY[i] = topY;

            if (barH > 0.5) {
                g.setFill(Color.rgb(27, 152, 242, 0.22));
                g.fillRoundRect(cx - barW / 2.0, topY, barW, barH, 5, 5);
                g.fillRect(cx - barW / 2.0, Math.min(padT + plotH, topY + barH * 0.5), barW, Math.min(4, barH * 0.5));
            }

            // Show X-axis label (keep readable density if many days)
            if (n <= 10 || i % Math.max(1, n / 7) == 0 || i == n - 1) {
                String shortDay = dp.day().length() >= 10 ? dp.day().substring(5) : dp.day();
                g.setFill(Color.web("#71717A"));
                g.setFont(Font.font(UITheme.MONO_FAMILY, 10.5));
                g.setTextAlign(TextAlignment.CENTER);
                g.setTextBaseline(VPos.TOP);
                g.fillText(shortDay, cx, padT + plotH + 8);
            }
        }

        // Smooth trend polyline + crisp dot markers
        if (n >= 2) {
            g.setStroke(Color.web("#1B98F2"));
            g.setLineWidth(1.8);
            g.setLineCap(StrokeLineCap.ROUND);
            g.setLineJoin(StrokeLineJoin.ROUND);
            g.strokePolyline(lineX, lineY, n);
        }
        for (int i = 0; i < n; i++) {
            g.setFill(Color.WHITE);
            g.fillOval(lineX[i] - 3.5, lineY[i] - 3.5, 7, 7);
            g.setStroke(Color.web("#1B98F2"));
            g.setLineWidth(1.8);
            g.strokeOval(lineX[i] - 3.5, lineY[i] - 3.5, 7, 7);
        }
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

        distributionList.getChildren().add(sectionSubLabel("By severity"));
        for (String key : new String[]{"CRITICAL", "HIGH", "MEDIUM", "LOW"}) {
            long c = bySev.path(key).asLong(0);
            distributionList.getChildren().add(distRow(UITheme.severityBadge(key), c, denom));
        }

        distributionList.getChildren().add(sectionSubLabel("By status"));
        for (String key : new String[]{"OPEN", "INVESTIGATING", "RESOLVED", "CLOSED"}) {
            long c = byStatus.path(key).asLong(0);
            distributionList.getChildren().add(distRow(UITheme.statusBadge(key), c, denom));
        }
    }

    private Label sectionSubLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 500; -fx-text-fill: #71717A; -fx-padding: 4 0 0 0;");
        return l;
    }

    private HBox distRow(Label badge, long count, long total) {
        HBox r = new HBox(10);
        r.setAlignment(Pos.CENTER_LEFT);
        badge.setMinWidth(102);
        badge.setAlignment(Pos.CENTER);

        ProgressBar bar = new ProgressBar((double) count / total);
        bar.setPrefHeight(6);
        bar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(bar, Priority.ALWAYS);

        Label countLbl = new Label(String.valueOf(count));
        countLbl.setMinWidth(28);
        countLbl.setAlignment(Pos.CENTER_RIGHT);
        countLbl.setStyle(UITheme.monoStyle(12, UITheme.INK_PRIMARY));

        r.getChildren().addAll(badge, bar, countLbl);
        return r;
    }

    private void setSyncState(String msg, String dotHex) {
        lastUpdatedLbl.setText(msg);
        if (syncDot != null) {
            syncDot.setFill(Color.web(dotHex));
        }
    }
}
