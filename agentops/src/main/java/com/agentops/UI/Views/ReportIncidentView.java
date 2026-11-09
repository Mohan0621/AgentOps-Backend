package com.agentops.UI.Views;

import java.net.http.HttpResponse;

import com.agentops.UI.ApiClient;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Inside Workspace: Clean Light Editorial Form to Report Incidents + Error Stack Traces.
 * Calls POST /api/incidents with all 6 fields (title, description, errorLogs, severity, status, incidentType).
 */
public class ReportIncidentView extends ScrollPane {

    private final Runnable onIncidentCreated;
    private final Runnable onViewQueue;

    private TextField txtTitle;
    private TextField txtType;
    private ComboBox<String> cmbSeverity;
    private ComboBox<String> cmbStatus;
    private TextArea txtDesc;
    private TextArea txtLogs;
    private Label statusLbl;
    private Button btnSubmit;
    private Button btnOpenQueueAfterSave;

    public ReportIncidentView(Runnable onIncidentCreated) {
        this(onIncidentCreated, null);
    }

    public ReportIncidentView(Runnable onIncidentCreated, Runnable onViewQueue) {
        this.onIncidentCreated = onIncidentCreated;
        this.onViewQueue = onViewQueue;
        buildUI();
    }

    private void buildUI() {
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        getStyleClass().add("scroll-pane");
        setStyle("-fx-background-color: #FFFFFF;");

        VBox page = new VBox(22);
        page.setPadding(new Insets(28, 36, 48, 36));
        page.setStyle("-fx-background-color: #FFFFFF;");
        page.setAlignment(Pos.TOP_CENTER);

        VBox inner = new VBox(22);
        inner.setMaxWidth(1040);

        inner.getChildren().addAll(buildHeader(), buildTwoColumnContent());
        page.getChildren().add(inner);
        setContent(page);
    }

    private VBox buildHeader() {
        VBox hdr = new VBox(6);
        Label eyebrow = UITheme.eyebrowLabel("Capture Anomaly  ·  POST /api/incidents");
        Label title = new Label("Report New Incident");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #000000;");
        Label sub = new Label("Log an exception, stack trace, or outage details for instant Groq / Gemini triage and 6-agent investigation.");
        sub.setWrapText(true);
        sub.setStyle(UITheme.subHeadingStyle());
        hdr.getChildren().addAll(eyebrow, title, sub);
        return hdr;
    }

    private HBox buildTwoColumnContent() {
        HBox row = new HBox(22);
        row.setAlignment(Pos.TOP_LEFT);

        VBox formCard = buildFormCard();
        HBox.setHgrow(formCard, Priority.ALWAYS);

        VBox sideGuide = buildSideGuideCard();
        sideGuide.setPrefWidth(310);
        sideGuide.setMinWidth(285);

        row.getChildren().addAll(formCard, sideGuide);
        return row;
    }

    private VBox buildFormCard() {
        VBox card = new VBox(18);
        card.setPadding(new Insets(26, 28, 28, 28));
        card.setStyle(UITheme.cardStyle());
        card.setEffect(UITheme.softCardShadow());

        // Title + Type row
        txtTitle = styledTextField("e.g. NullPointerException in Payment Gateway");
        txtType  = styledTextField("e.g. Database, Security, API, Memory");
        GridPane row1 = twoColumnGrid(
            UITheme.formField("Incident Title *", txtTitle),
            UITheme.formField("Incident Type", txtType)
        );

        // Quick type suggestion pills
        HBox typeChips = new HBox(6);
        typeChips.setAlignment(Pos.CENTER_LEFT);
        Label tHint = new Label("Quick type:");
        tHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF; -fx-font-weight: 600;");
        typeChips.getChildren().add(tHint);
        for (String t : new String[]{"DATABASE", "APPLICATION_BUG", "AUTHENTICATION", "NETWORK", "MEMORY_LEAK"}) {
            Button chip = new Button(t);
            chip.setMnemonicParsing(false);
            chip.setCursor(Cursor.HAND);
            chip.setStyle("-fx-background-color: #F6F6F6; -fx-text-fill: #4D4D4D; -fx-font-size: 10.5px; -fx-font-weight: 600; -fx-padding: 4 10; -fx-background-radius: 999;");
            chip.setOnAction(e -> txtType.setText(t));
            typeChips.getChildren().add(chip);
        }

        // Severity + Status row
        cmbSeverity = styledCombo("LOW", "MEDIUM", "HIGH", "CRITICAL");
        cmbSeverity.setValue("HIGH");
        cmbStatus = styledCombo("OPEN", "INVESTIGATING", "RESOLVED", "CLOSED");
        cmbStatus.setValue("OPEN");
        GridPane row2 = twoColumnGrid(
            UITheme.formField("Severity", cmbSeverity),
            UITheme.formField("Initial Status", cmbStatus)
        );

        // Description
        txtDesc = styledTextArea(3, "What happened? Which endpoint or customer flow is affected?");

        // Stack Trace / Error Logs
        txtLogs = styledTextArea(8, "Paste Java stack traces, SQL exceptions, or raw container error logs here...");
        txtLogs.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 12.5px;");

        // Status Feedback Label
        statusLbl = new Label("");
        statusLbl.setWrapText(true);
        statusLbl.setVisible(false);
        statusLbl.setManaged(false);

        // Action Buttons
        btnSubmit = UITheme.serkoBlackPill("Submit Incident", true);
        btnSubmit.setPadding(new Insets(11, 24, 11, 24));
        btnSubmit.setOnAction(e -> handleSubmit());

        Button btnClear = UITheme.serkoWhitePill("Clear Form", false);
        btnClear.setPadding(new Insets(11, 20, 11, 20));
        btnClear.setOnAction(e -> clearForm());

        btnOpenQueueAfterSave = UITheme.serkoRainbowPill("View in Queue", true);
        btnOpenQueueAfterSave.setPadding(new Insets(10, 18, 10, 18));
        btnOpenQueueAfterSave.setVisible(false);
        btnOpenQueueAfterSave.setManaged(false);
        btnOpenQueueAfterSave.setOnAction(e -> {
            if (onViewQueue != null) onViewQueue.run();
        });

        HBox actions = new HBox(12, btnSubmit, btnClear, btnOpenQueueAfterSave);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(4, 0, 0, 0));

        card.getChildren().addAll(
            row1,
            typeChips,
            row2,
            UITheme.formField("Description", txtDesc),
            UITheme.formFieldHint("Error Logs / Stack Trace", "(analyzed automatically by the 6-agent pipeline)", txtLogs),
            statusLbl,
            actions
        );
        return card;
    }

    private VBox buildSideGuideCard() {
        VBox side = new VBox(16);

        VBox iceCard = new VBox(14);
        iceCard.setPadding(new Insets(22));
        iceCard.setStyle("-fx-background-color: " + UITheme.BG_ICE + "; -fx-background-radius: 22;");

        Label badge = UITheme.positiveBadge("AI-Ready Ingestion");
        Label h = new Label("What happens next?");
        h.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #000000;");

        iceCard.getChildren().addAll(
            badge, h,
            stepRow("1", "Persisted to Incident Queue", "Saved immediately with severity tag and reporter metadata."),
            stepRow("2", "Instant Groq / Gemini Triage", "Isolate affected classes and root cause within ~1.4 seconds."),
            stepRow("3", "6-Agent Pipeline Execution", "Or run all 6 autonomous agents for full code patches & markdown report.")
        );

        // Helpful Load Sample card for testing / demos
        VBox sampleCard = new VBox(12);
        sampleCard.setPadding(new Insets(20));
        sampleCard.setStyle(UITheme.softCardStyle());
        Label sTitle = new Label("Testing the AI Pipeline?");
        sTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #000000;");
        Label sBody = new Label("Load a realistic production HikariPool DB exhaustion stack trace in one click.");
        sBody.setWrapText(true);
        sBody.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #4D4D4D;");

        Button btnLoadSample = UITheme.serkoWhitePill("Load Sample Error Log", false);
        btnLoadSample.setMaxWidth(Double.MAX_VALUE);
        btnLoadSample.setOnAction(e -> fillSampleIncident());
        sampleCard.getChildren().addAll(sTitle, sBody, btnLoadSample);

        side.getChildren().addAll(iceCard, sampleCard);
        return side;
    }

    private HBox stepRow(String num, String title, String desc) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.TOP_LEFT);
        Label badge = new Label(num);
        badge.setMinSize(24, 24);
        badge.setAlignment(Pos.CENTER);
        badge.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #000000; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 999;");

        VBox text = new VBox(2);
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #000000;");
        Label d = new Label(desc);
        d.setWrapText(true);
        d.setStyle("-fx-font-size: 12px; -fx-text-fill: #3F3F46;");
        text.getChildren().addAll(t, d);
        row.getChildren().addAll(badge, text);
        return row;
    }

    private void fillSampleIncident() {
        txtTitle.setText("HikariPool-1 Connection Timeout in CheckoutService");
        txtType.setText("DATABASE");
        cmbSeverity.setValue("CRITICAL");
        cmbStatus.setValue("OPEN");
        txtDesc.setText("Checkout API requests began throwing HTTP 500 errors under moderate traffic spike. DB pool connections are exhausted after 30 seconds.");
        txtLogs.setText(
            "java.sql.SQLTransientConnectionException: HikariPool-1 - Connection is not available, request timed out after 30001ms.\n" +
            "\tat com.zaxxer.hikari.pool.HikariPool.createTimeoutException(HikariPool.java:696)\n" +
            "\tat com.zaxxer.hikari.pool.HikariPool.getConnection(HikariPool.java:181)\n" +
            "\tat com.agentops.checkout.PaymentLedgerDao.recordSettlement(PaymentLedgerDao.java:142)\n" +
            "Caused by: java.sql.SQLNonTransientConnectionException: Too many connections (max_connections=151)"
        );
        hideStatus();
    }

    // ── Submit Handler ────────────────────────────────────────────
    private void handleSubmit() {
        if (txtTitle.getText().isBlank()) {
            showStatus("⚠  Incident title is required.", false);
            return;
        }

        btnSubmit.setDisable(true);
        btnSubmit.setText("Submitting...");
        btnOpenQueueAfterSave.setVisible(false);
        btnOpenQueueAfterSave.setManaged(false);
        hideStatus();

        String title    = txtTitle.getText().trim();
        String desc     = txtDesc.getText().trim();
        String logs     = txtLogs.getText().trim();
        String severity = cmbSeverity.getValue();
        String status   = cmbStatus.getValue();
        String type     = txtType.getText().trim();

        new Thread(() -> {
            try {
                HttpResponse<String> res = ApiClient.createIncident(title, desc, logs, severity, status, type);
                if (res.statusCode() == 201 || res.statusCode() == 200) {
                    Platform.runLater(() -> {
                        clearForm();
                        showStatus("✓  Incident submitted! Open the Queue tab to run Quick AI or 6-Agent investigation.", true);
                        if (onViewQueue != null) {
                            btnOpenQueueAfterSave.setVisible(true);
                            btnOpenQueueAfterSave.setManaged(true);
                        }
                        if (onIncidentCreated != null) onIncidentCreated.run();
                    });
                } else {
                    Platform.runLater(() -> showStatus("✕  Submission failed (" + res.statusCode() + "): " + res.body(), false));
                }
            } catch (Exception ex) {
                Platform.runLater(() -> showStatus("✕  Connection error: " + ex.getMessage(), false));
            } finally {
                Platform.runLater(() -> {
                    btnSubmit.setDisable(false);
                    btnSubmit.setText("Submit Incident");
                });
            }
        }).start();
    }

    private void clearForm() {
        txtTitle.clear();
        txtType.clear();
        txtDesc.clear();
        txtLogs.clear();
        cmbSeverity.setValue("HIGH");
        cmbStatus.setValue("OPEN");
        hideStatus();
        btnOpenQueueAfterSave.setVisible(false);
        btnOpenQueueAfterSave.setManaged(false);
    }

    private void showStatus(String msg, boolean success) {
        statusLbl.setText(msg);
        String bg = success ? UITheme.POS_BG : "#FEE2E2";
        String fg = success ? UITheme.POS_INK : "#B91C1C";
        statusLbl.setStyle(
            "-fx-text-fill: " + fg + "; -fx-font-size: 12.5px; -fx-font-weight: bold;" +
            "-fx-background-color: " + bg + "; -fx-padding: 10 14; -fx-background-radius: 12;"
        );
        statusLbl.setVisible(true);
        statusLbl.setManaged(true);
    }

    private void hideStatus() {
        statusLbl.setVisible(false);
        statusLbl.setManaged(false);
    }

    // ── Field Builders ────────────────────────────────────────────
    private TextField styledTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setMaxWidth(Double.MAX_VALUE);
        return tf;
    }

    private TextArea styledTextArea(int rows, String prompt) {
        TextArea ta = new TextArea();
        ta.setPromptText(prompt);
        ta.setPrefRowCount(rows);
        ta.setWrapText(true);
        return ta;
    }

    @SafeVarargs
    private <T> ComboBox<T> styledCombo(T... items) {
        ComboBox<T> cb = new ComboBox<>();
        cb.getItems().addAll(items);
        cb.setMaxWidth(Double.MAX_VALUE);
        return cb;
    }

    private GridPane twoColumnGrid(VBox left, VBox right) {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        GridPane.setHgrow(left, Priority.ALWAYS);
        GridPane.setHgrow(right, Priority.ALWAYS);
        left.setMaxWidth(Double.MAX_VALUE);
        right.setMaxWidth(Double.MAX_VALUE);
        grid.addRow(0, left, right);
        grid.getColumnConstraints().addAll(percentCol(50), percentCol(50));
        return grid;
    }

    private ColumnConstraints percentCol(double pct) {
        ColumnConstraints cc = new ColumnConstraints();
        cc.setPercentWidth(pct);
        return cc;
    }
}
