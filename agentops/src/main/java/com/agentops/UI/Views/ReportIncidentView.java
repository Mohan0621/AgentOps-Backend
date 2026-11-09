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
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Inside Workspace: Operate-aligned Incident Creation Form + Unified Triage Guide.
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
    private HBox statusBanner;
    private StackPane statusIconSlot;
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
        setStyle("-fx-background-color: " + UITheme.BG_CANVAS + ";");

        VBox page = new VBox(20);
        page.setPadding(new Insets(24, 28, 44, 28));
        page.setStyle("-fx-background-color: " + UITheme.BG_CANVAS + ";");
        page.setAlignment(Pos.TOP_CENTER);

        VBox inner = new VBox(18);
        inner.setMaxWidth(1040);

        inner.getChildren().addAll(buildHeader(), buildTwoColumnContent());
        page.getChildren().add(inner);
        setContent(page);
    }

    private VBox buildHeader() {
        VBox hdr = new VBox(4);
        Label eyebrow = UITheme.wsSectionLabel("Capture anomaly");
        Label title = new Label("Report new incident");
        title.setStyle(UITheme.headingStyle());
        Label sub = new Label("Log an exception, stack trace, or service disruption for Groq / Gemini triage and 6-agent root cause analysis.");
        sub.setWrapText(true);
        sub.setStyle(UITheme.subHeadingStyle());
        hdr.getChildren().addAll(eyebrow, title, sub);
        return hdr;
    }

    private HBox buildTwoColumnContent() {
        HBox row = new HBox(18);
        row.setAlignment(Pos.TOP_LEFT);

        VBox formCard = buildFormCard();
        HBox.setHgrow(formCard, Priority.ALWAYS);

        VBox sideGuide = buildSideGuideCard();
        sideGuide.setPrefWidth(298);
        sideGuide.setMinWidth(275);

        row.getChildren().addAll(formCard, sideGuide);
        return row;
    }

    private VBox buildFormCard() {
        VBox card = new VBox(16);
        card.setPadding(new Insets(22, 24, 24, 24));
        card.setStyle(UITheme.cardStyle());

        // Title + Category
        txtTitle = styledTextField("e.g. NullPointerException in Payment Gateway");
        txtType  = styledTextField("e.g. Database, Application bug, Authentication");
        GridPane row1 = twoColumnGrid(
            UITheme.formField("Incident title *", txtTitle),
            UITheme.formField("Category", txtType)
        );

        // Quick-select category chips (8px clean geometry)
        HBox typeChips = new HBox(6);
        typeChips.setAlignment(Pos.CENTER_LEFT);
        Label tHint = new Label("Quick fill:");
        tHint.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #71717A; -fx-font-weight: 500;");
        typeChips.getChildren().add(tHint);
        String[][] presetTypes = {
            {"Database",        "DATABASE"},
            {"Application bug", "APPLICATION_BUG"},
            {"Authentication",  "AUTHENTICATION"},
            {"Network",         "NETWORK"},
            {"Memory leak",     "MEMORY_LEAK"}
        };
        for (String[] pt : presetTypes) {
            Button chip = new Button(pt[0]);
            chip.setMnemonicParsing(false);
            chip.setCursor(Cursor.HAND);
            chip.setStyle(
                "-fx-background-color: #F4F4F5; -fx-text-fill: #3F3F46;" +
                "-fx-border-color: #E4E4E7; -fx-border-width: 1; -fx-border-radius: 6;" +
                "-fx-font-size: 11.5px; -fx-font-weight: 500; -fx-padding: 3 9; -fx-background-radius: 6;"
            );
            chip.setOnAction(e -> txtType.setText(pt[1]));
            typeChips.getChildren().add(chip);
        }

        // Severity + Status
        cmbSeverity = styledCombo("LOW", "MEDIUM", "HIGH", "CRITICAL");
        cmbSeverity.setValue("HIGH");
        cmbStatus = styledCombo("OPEN", "INVESTIGATING", "RESOLVED", "CLOSED");
        cmbStatus.setValue("OPEN");
        GridPane row2 = twoColumnGrid(
            UITheme.formField("Severity", cmbSeverity),
            UITheme.formField("Initial status", cmbStatus)
        );

        // Description
        txtDesc = styledTextArea(3, "What happened? Which endpoint or customer flow is impacted?");

        // Error Logs
        txtLogs = styledTextArea(8, "Paste Java stack traces, SQL exceptions, or raw container error logs...");
        txtLogs.setStyle(UITheme.monoStyle(12, "#18181B"));

        // Inline Validation / Result Banner with SVG Icon
        statusBanner = new HBox(8);
        statusBanner.setAlignment(Pos.CENTER_LEFT);
        statusBanner.setPadding(new Insets(10, 12, 10, 12));
        statusIconSlot = new StackPane();
        statusLbl = new Label("");
        statusLbl.setWrapText(true);
        statusBanner.getChildren().addAll(statusIconSlot, statusLbl);
        statusBanner.setVisible(false);
        statusBanner.setManaged(false);

        // Action Buttons (Operate 8px)
        btnSubmit = UITheme.wsPrimaryBtn("Submit incident", null);
        btnSubmit.setPadding(new Insets(9, 18, 9, 18));
        btnSubmit.setOnAction(e -> handleSubmit());

        Button btnClear = UITheme.wsSecondaryBtn("Clear form", null);
        btnClear.setOnAction(e -> clearForm());

        btnOpenQueueAfterSave = UITheme.wsSecondaryBtn("Open in queue", UITheme.inboxIcon(14, Color.web("#18181B")));
        btnOpenQueueAfterSave.setVisible(false);
        btnOpenQueueAfterSave.setManaged(false);
        btnOpenQueueAfterSave.setOnAction(e -> {
            if (onViewQueue != null) onViewQueue.run();
        });

        HBox actions = new HBox(10, btnSubmit, btnClear, btnOpenQueueAfterSave);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(4, 0, 0, 0));

        card.getChildren().addAll(
            row1,
            typeChips,
            row2,
            UITheme.formField("Description", txtDesc),
            UITheme.formFieldHint("Error logs / stack trace", "(parsed by the 6-agent pipeline)", txtLogs),
            statusBanner,
            actions
        );
        return card;
    }

    /** Unified calm reference card replacing duplicated pastel side cards. */
    private VBox buildSideGuideCard() {
        VBox card = new VBox(16);
        card.setPadding(new Insets(20));
        card.setStyle(UITheme.cardStyle());

        VBox stepsSection = new VBox(12);
        Label h = new Label("After submission");
        h.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #09090B;");

        stepsSection.getChildren().addAll(
            h,
            stepRow("1", "Saved to incident queue", "Persisted with severity metadata and reporter timestamp."),
            stepRow("2", "Quick LLM triage", "Isolate culprit classes and initial mitigation in under 2 seconds."),
            stepRow("3", "6-agent investigation", "Execute full classification, root cause, and patch generation.")
        );

        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: " + UITheme.WS_BORDER + ";");

        VBox sampleSection = new VBox(10);
        Label sTitle = new Label("Sample stack trace");
        sTitle.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 600; -fx-text-fill: #09090B;");
        Label sBody = new Label("Load a sample HikariPool DB exhaustion trace to test AI triage.");
        sBody.setWrapText(true);
        sBody.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #52525B;");

        Button btnLoadSample = UITheme.wsSecondaryBtn("Load sample error log", UITheme.terminalIcon(14, Color.web("#52525B")));
        btnLoadSample.setMaxWidth(Double.MAX_VALUE);
        btnLoadSample.setOnAction(e -> fillSampleIncident());
        sampleSection.getChildren().addAll(sTitle, sBody, btnLoadSample);

        card.getChildren().addAll(stepsSection, divider, sampleSection);
        return card;
    }

    private HBox stepRow(String num, String title, String desc) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.TOP_LEFT);
        Label badge = new Label(num);
        badge.setMinSize(22, 22);
        badge.setAlignment(Pos.CENTER);
        badge.setStyle(
            "-fx-background-color: #F4F4F5; -fx-text-fill: #09090B;" +
            "-fx-border-color: #E4E4E7; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;" +
            "-fx-font-size: 11px; -fx-font-weight: 600;"
        );

        VBox text = new VBox(2);
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 500; -fx-text-fill: #09090B;");
        Label d = new Label(desc);
        d.setWrapText(true);
        d.setStyle("-fx-font-size: 12px; -fx-text-fill: #52525B;");
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
            showStatus("Incident title is required.", false);
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
                        showStatus("Incident submitted. Open the queue to run Quick diagnosis or 6-agent investigation.", true);
                        if (onViewQueue != null) {
                            btnOpenQueueAfterSave.setVisible(true);
                            btnOpenQueueAfterSave.setManaged(true);
                        }
                        if (onIncidentCreated != null) onIncidentCreated.run();
                    });
                } else {
                    Platform.runLater(() -> showStatus("Submission failed (" + res.statusCode() + "): " + res.body(), false));
                }
            } catch (Exception ex) {
                Platform.runLater(() -> showStatus("Connection error: " + ex.getMessage(), false));
            } finally {
                Platform.runLater(() -> {
                    btnSubmit.setDisable(false);
                    btnSubmit.setText("Submit incident");
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
        String bg = success ? UITheme.POS_BG : "#FEF2F2";
        String br = success ? UITheme.POS_BORDER : "#FECACA";
        String fg = success ? UITheme.POS_INK : "#991B1B";

        statusIconSlot.getChildren().setAll(
            success ? UITheme.checkCircleIcon(15, Color.web(UITheme.POS_DOT))
                    : UITheme.alertIcon(15, Color.web("#DC2626"))
        );
        statusLbl.setStyle("-fx-text-fill: " + fg + "; -fx-font-size: 12.5px; -fx-font-weight: 500;");
        statusBanner.setStyle(
            "-fx-background-color: " + bg + "; -fx-border-color: " + br + ";" +
            "-fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;"
        );
        statusBanner.setVisible(true);
        statusBanner.setManaged(true);
    }

    private void hideStatus() {
        statusBanner.setVisible(false);
        statusBanner.setManaged(false);
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
        grid.setHgap(14);
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
