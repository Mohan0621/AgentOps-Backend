package com.agentops.UI.Views;

import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import com.agentops.UI.ApiClient;
import com.agentops.UI.Models.IncidentRow;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Inside Workspace: Incident Queue + Structured AI Investigation Workbench.
 * Light Editorial Canvas aligned with serko.ai design tokens.
 */
public class IncidentsView extends BorderPane {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ObservableList<IncidentRow> incidentList = FXCollections.observableArrayList();
    private final FilteredList<IncidentRow> filteredList = new FilteredList<>(incidentList, p -> true);
    private final TableView<IncidentRow> incidentTable = new TableView<>();

    private TextField txtSearch;
    private ComboBox<String> cmbSevFilter;
    private ComboBox<String> cmbStatFilter;

    private Label lblQueueCount;
    private Label lblOpenChip;
    private Label lblCriticalChip;
    private Label statusBarLbl;

    // Right Workbench Panel state
    private Button subTabDetails, subTabAi, subTabHistory;
    private Button btnMarkInvestigating, btnMarkResolved;
    private Button btnToolbarQuickAi, btnToolbarMultiAgent;
    private VBox detailsPane, aiPane, historyPane;
    private StackPane workbenchStack;
    private ScrollPane workbenchScroll;

    private String currentSubTab = "details";
    private String lastAiIncidentId = null;
    private Timeline pipelineStepTimeline;

    public IncidentsView() {
        setupUI();
    }

    private void setupUI() {
        setPadding(new Insets(24, 28, 18, 28));
        setStyle("-fx-background-color: #FFFFFF;");

        setTop(buildTopSection());
        setCenter(buildSplitPane());
        setBottom(buildStatusBar());
    }

    // ── Top Header + Filter Bar ───────────────────────────────────
    private VBox buildTopSection() {
        VBox wrap = new VBox(14);
        wrap.setPadding(new Insets(0, 0, 18, 0));

        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label eyebrow = UITheme.eyebrowLabel("Workspace  ·  Live Telemetry");
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Incident Queue");
        title.setStyle(UITheme.headingStyle());
        lblQueueCount = new Label("0 incidents");
        lblQueueCount.setStyle(
            "-fx-background-color: #F6F6F6; -fx-text-fill: #4D4D4D; -fx-font-size: 11.5px; " +
            "-fx-font-weight: bold; -fx-padding: 4 11; -fx-background-radius: 999;"
        );
        titleRow.getChildren().addAll(title, lblQueueCount);
        titleBox.getChildren().addAll(eyebrow, titleRow);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnRefresh = UITheme.serkoWhitePill("Refresh", false);
        btnRefresh.setPadding(new Insets(9, 18, 9, 18));
        btnRefresh.setOnAction(e -> refreshIncidents());

        btnToolbarQuickAi = UITheme.serkoBlackPill("Quick Diagnosis", true);
        btnToolbarQuickAi.setPadding(new Insets(9, 20, 9, 20));
        btnToolbarQuickAi.setOnAction(e -> triggerAiDiagnosis());

        btnToolbarMultiAgent = UITheme.serkoRainbowPill("6-Agent Investigation", true);
        btnToolbarMultiAgent.setPadding(new Insets(9, 20, 9, 20));
        btnToolbarMultiAgent.setOnAction(e -> triggerMultiAgentInvestigation());

        bar.getChildren().addAll(titleBox, spacer, btnRefresh, btnToolbarQuickAi, btnToolbarMultiAgent);

        // Search + Severity/Status filter strip
        HBox filterBar = new HBox(10);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(10, 14, 10, 14));
        filterBar.setStyle("-fx-background-color: #F6F6F6; -fx-background-radius: 16;");

        txtSearch = new TextField();
        txtSearch.setPromptText("Search incidents by #ID, title, component, or error log...");
        txtSearch.setStyle(
            "-fx-background-color: #FFFFFF; -fx-border-color: #E5E7EB; -fx-border-radius: 10; " +
            "-fx-background-radius: 10; -fx-padding: 8 14; -fx-font-size: 13px; -fx-text-fill: #000000;"
        );
        HBox.setHgrow(txtSearch, Priority.ALWAYS);
        txtSearch.textProperty().addListener((obs, o, n) -> applyFilters());

        cmbSevFilter = new ComboBox<>();
        cmbSevFilter.getItems().addAll("All Severities", "CRITICAL", "HIGH", "MEDIUM", "LOW");
        cmbSevFilter.setValue("All Severities");
        cmbSevFilter.setPrefWidth(155);
        cmbSevFilter.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E5E7EB; -fx-border-radius: 10; -fx-background-radius: 10;");
        cmbSevFilter.valueProperty().addListener((obs, o, n) -> applyFilters());

        cmbStatFilter = new ComboBox<>();
        cmbStatFilter.getItems().addAll("All Statuses", "OPEN", "INVESTIGATING", "RESOLVED", "CLOSED");
        cmbStatFilter.setValue("All Statuses");
        cmbStatFilter.setPrefWidth(160);
        cmbStatFilter.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E5E7EB; -fx-border-radius: 10; -fx-background-radius: 10;");
        cmbStatFilter.valueProperty().addListener((obs, o, n) -> applyFilters());

        Button btnResetFilter = new Button("Clear");
        btnResetFilter.setStyle("-fx-background-color: transparent; -fx-text-fill: #71717A; -fx-font-size: 12px; -fx-font-weight: 600; -fx-cursor: hand;");
        btnResetFilter.setOnAction(e -> {
            txtSearch.clear();
            cmbSevFilter.setValue("All Severities");
            cmbStatFilter.setValue("All Statuses");
        });

        filterBar.getChildren().addAll(txtSearch, cmbSevFilter, cmbStatFilter, btnResetFilter);
        wrap.getChildren().addAll(bar, filterBar);
        return wrap;
    }

    // ── Split Pane: Table Left, Structured Workbench Right ────────
    private HBox buildSplitPane() {
        HBox split = new HBox(18);
        split.setFillHeight(true);

        VBox tableCard = buildTableCard();
        VBox workbenchCard = buildWorkbenchCard();

        HBox.setHgrow(tableCard, Priority.ALWAYS);
        split.getChildren().addAll(tableCard, workbenchCard);
        return split;
    }

    private VBox buildTableCard() {
        VBox card = new VBox(0);
        card.setStyle(UITheme.cardStyle());
        card.setEffect(UITheme.softCardShadow());

        // Header strip
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 18, 14, 18));
        header.setStyle(
            "-fx-background-color: #FFFFFF;" +
            "-fx-background-radius: 18 18 0 0;" +
            "-fx-border-color: transparent transparent #F1F1F4 transparent;"
        );

        Label lbl = new Label("All Incidents");
        lbl.setStyle("-fx-text-fill: #000000; -fx-font-size: 13.5px; -fx-font-weight: bold;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        lblOpenChip = UITheme.statusBadge("OPEN");
        lblOpenChip.setText("0 Open");

        lblCriticalChip = UITheme.severityBadge("CRITICAL");
        lblCriticalChip.setText("0 Critical");

        header.getChildren().addAll(lbl, sp, lblOpenChip, lblCriticalChip);

        // TableView styled purely by global light CSS (css/app.css)
        buildTableColumns();
        SortedList<IncidentRow> sorted = new SortedList<>(filteredList);
        sorted.comparatorProperty().bind(incidentTable.comparatorProperty());
        incidentTable.setItems(sorted);
        incidentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        incidentTable.setFixedCellSize(50);

        Label emptyPlaceholder = new Label("No incidents match the current filter.\nClick Refresh or report a new incident.");
        emptyPlaceholder.setStyle("-fx-text-fill: #71717A; -fx-font-size: 13px; -fx-text-alignment: center;");
        incidentTable.setPlaceholder(emptyPlaceholder);

        VBox.setVgrow(incidentTable, Priority.ALWAYS);

        incidentTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                onIncidentSelected(newV);
            }
        });

        card.getChildren().addAll(header, incidentTable);
        return card;
    }

    @SuppressWarnings("unchecked")
    private void buildTableColumns() {
        TableColumn<IncidentRow, String> colId = col("#", 54);
        colId.setMaxWidth(72);
        colId.setCellValueFactory(c -> c.getValue().id);
        colId.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else { setText("#" + item); setStyle("-fx-text-fill: #71717A; -fx-font-weight: bold;"); }
            }
        });

        TableColumn<IncidentRow, String> colTitle = col("Incident Title", 240);
        colTitle.setCellValueFactory(c -> c.getValue().title);
        colTitle.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else { setText(item); setStyle("-fx-text-fill: #000000; -fx-font-weight: 600;"); }
            }
        });

        TableColumn<IncidentRow, String> colType = col("Type", 125);
        colType.setCellValueFactory(c -> c.getValue().type);
        colType.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label chip = new Label(item);
                    chip.setStyle("-fx-background-color: #F6F6F6; -fx-text-fill: #4D4D4D; -fx-font-size: 11px; -fx-font-weight: 600; -fx-padding: 3 9; -fx-background-radius: 999;");
                    setText(null);
                    setGraphic(chip);
                }
            }
        });

        TableColumn<IncidentRow, String> colSev = col("Severity", 104);
        colSev.setMaxWidth(120);
        colSev.setCellValueFactory(c -> c.getValue().severity);
        colSev.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else { setText(null); setGraphic(UITheme.severityBadge(item)); }
            }
        });

        TableColumn<IncidentRow, String> colStatus = col("Status", 126);
        colStatus.setMaxWidth(145);
        colStatus.setCellValueFactory(c -> c.getValue().status);
        colStatus.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else { setText(null); setGraphic(UITheme.statusBadge(item)); }
            }
        });

        incidentTable.getColumns().addAll(colId, colTitle, colType, colSev, colStatus);
    }

    // ── Right Investigation Workbench ─────────────────────────────
    private VBox buildWorkbenchCard() {
        VBox card = new VBox(0);
        card.setPrefWidth(470);
        card.setMinWidth(410);
        card.setStyle(UITheme.cardStyle());
        card.setEffect(UITheme.softCardShadow());

        // Top Bar with Quick Status Actions
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 18, 12, 18));
        header.setStyle(
            "-fx-background-color: #FFFFFF;" +
            "-fx-background-radius: 18 18 0 0;"
        );

        Label dot = new Label("●");
        dot.setStyle("-fx-text-fill: " + UITheme.POS_DOT + "; -fx-font-size: 9px;");
        Label lbl = new Label("Investigation Workbench");
        lbl.setStyle("-fx-text-fill: #000000; -fx-font-size: 13.5px; -fx-font-weight: bold;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        btnMarkInvestigating = quickActionPill("Investigate", "#E8F3FE", "#0369A1");
        btnMarkInvestigating.setOnAction(e -> updateSelectedStatus("INVESTIGATING"));

        btnMarkResolved = quickActionPill("Resolve", UITheme.POS_BG, UITheme.POS_INK);
        btnMarkResolved.setOnAction(e -> updateSelectedStatus("RESOLVED"));

        btnMarkInvestigating.setDisable(true);
        btnMarkResolved.setDisable(true);

        header.getChildren().addAll(dot, lbl, sp, btnMarkInvestigating, btnMarkResolved);

        // Segmented Sub-Tabs (#F6F6F6 pill track)
        HBox subTabBarWrap = new HBox();
        subTabBarWrap.setPadding(new Insets(0, 18, 12, 18));
        subTabBarWrap.setStyle("-fx-border-color: transparent transparent #F1F1F4 transparent;");

        HBox subTrack = new HBox(4);
        subTrack.setPadding(new Insets(4));
        subTrack.setStyle("-fx-background-color: #F6F6F6; -fx-background-radius: 999;");
        HBox.setHgrow(subTrack, Priority.ALWAYS);

        subTabDetails = makeSubTabBtn("Details & Logs", "details");
        subTabAi      = makeSubTabBtn("AI & Agents",    "ai");
        subTabHistory = makeSubTabBtn("Saved History",  "history");
        HBox.setHgrow(subTabDetails, Priority.ALWAYS);
        HBox.setHgrow(subTabAi, Priority.ALWAYS);
        HBox.setHgrow(subTabHistory, Priority.ALWAYS);
        subTabDetails.setMaxWidth(Double.MAX_VALUE);
        subTabAi.setMaxWidth(Double.MAX_VALUE);
        subTabHistory.setMaxWidth(Double.MAX_VALUE);

        subTrack.getChildren().addAll(subTabDetails, subTabAi, subTabHistory);
        subTabBarWrap.getChildren().add(subTrack);

        // Body panes inside ScrollPane
        detailsPane = new VBox(14);
        detailsPane.setPadding(new Insets(18));

        aiPane = new VBox(14);
        aiPane.setPadding(new Insets(18));

        historyPane = new VBox(12);
        historyPane.setPadding(new Insets(18));

        workbenchStack = new StackPane(detailsPane, aiPane, historyPane);
        workbenchScroll = new ScrollPane(workbenchStack);
        workbenchScroll.setFitToWidth(true);
        workbenchScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        workbenchScroll.getStyleClass().add("scroll-pane");
        VBox.setVgrow(workbenchScroll, Priority.ALWAYS);

        renderEmptySelection();
        switchSubTab("details");

        card.getChildren().addAll(header, subTabBarWrap, workbenchScroll);
        return card;
    }

    private Button quickActionPill(String text, String bg, String fg) {
        Button b = new Button(text);
        b.setCursor(Cursor.HAND);
        b.setStyle(
            "-fx-background-color: " + bg + "; -fx-text-fill: " + fg + ";" +
            "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5 11; -fx-background-radius: 999;"
        );
        return b;
    }

    private Button makeSubTabBtn(String text, String id) {
        Button b = new Button(text);
        b.setCursor(Cursor.HAND);
        b.setOnAction(e -> switchSubTab(id));
        return b;
    }

    private void switchSubTab(String tabId) {
        this.currentSubTab = tabId;
        detailsPane.setVisible("details".equals(tabId));
        detailsPane.setManaged("details".equals(tabId));
        aiPane.setVisible("ai".equals(tabId));
        aiPane.setManaged("ai".equals(tabId));
        historyPane.setVisible("history".equals(tabId));
        historyPane.setManaged("history".equals(tabId));

        styleSubTab(subTabDetails, "details".equals(tabId));
        styleSubTab(subTabAi,      "ai".equals(tabId));
        styleSubTab(subTabHistory, "history".equals(tabId));
        workbenchScroll.setVvalue(0);
    }

    private void styleSubTab(Button b, boolean active) {
        if (active) {
            b.setStyle(
                "-fx-background-color: #FFFFFF; -fx-text-fill: #000000; -fx-font-size: 12px; " +
                "-fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 999;"
            );
            DropShadow s = new DropShadow();
            s.setColor(Color.rgb(0, 0, 0, 0.06));
            s.setRadius(6);
            s.setOffsetY(1);
            b.setEffect(s);
        } else {
            b.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #71717A; -fx-font-size: 12px; " +
                "-fx-font-weight: 600; -fx-padding: 6 12; -fx-background-radius: 999;"
            );
            b.setEffect(null);
        }
    }

    // ── Status Bar ────────────────────────────────────────────────
    private HBox buildStatusBar() {
        HBox bar = new HBox(8);
        bar.setPadding(new Insets(12, 4, 0, 4));
        bar.setAlignment(Pos.CENTER_LEFT);

        statusBarLbl = new Label("●  Ready");
        statusBarLbl.setStyle("-fx-text-fill: #71717A; -fx-font-size: 11.5px; -fx-font-weight: 500;");
        bar.getChildren().add(statusBarLbl);
        return bar;
    }

    // ── Filtering & Selection ─────────────────────────────────────
    private void applyFilters() {
        String q = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase(Locale.ROOT);
        String sev = cmbSevFilter.getValue();
        String stat = cmbStatFilter.getValue();

        filteredList.setPredicate(row -> {
            boolean matchesQuery = q.isEmpty()
                || ("#" + row.id.get()).toLowerCase(Locale.ROOT).contains(q)
                || safeLower(row.title.get()).contains(q)
                || safeLower(row.type.get()).contains(q)
                || safeLower(row.description.get()).contains(q)
                || safeLower(row.errorLogs.get()).contains(q);

            boolean matchesSev = sev == null || "All Severities".equals(sev)
                || sev.equalsIgnoreCase(row.severity.get());

            boolean matchesStat = stat == null || "All Statuses".equals(stat)
                || stat.equalsIgnoreCase(row.status.get());

            return matchesQuery && matchesSev && matchesStat;
        });
    }

    private String safeLower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private void onIncidentSelected(IncidentRow row) {
        btnMarkInvestigating.setDisable(false);
        btnMarkResolved.setDisable(false);

        renderDetailsPane(row);
        if (!row.id.get().equals(lastAiIncidentId)) {
            lastAiIncidentId = row.id.get();
            renderAiIdleState(row);
        }
        loadSavedInvestigations(row);
    }

    private void renderEmptySelection() {
        detailsPane.getChildren().setAll(infoCard(
            "Select an incident",
            "Choose any incident from the queue on the left to inspect logs, run Quick AI Diagnosis, or execute the 6-agent investigation pipeline.",
            UITheme.BG_SOFT
        ));
        aiPane.getChildren().setAll(infoCard(
            "No incident selected",
            "Select an incident row to run single-shot LLM triage or the 6-agent pipeline.",
            UITheme.BG_SOFT
        ));
        historyPane.getChildren().setAll(infoCard(
            "Saved investigations",
            "Past AI and manual investigation records saved in MySQL will appear here once an incident is selected.",
            UITheme.BG_SOFT
        ));
    }

    private void renderDetailsPane(IncidentRow row) {
        detailsPane.getChildren().clear();

        // Top badges & title
        HBox metaRow = new HBox(8);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        Label idBadge = new Label("#" + row.id.get());
        idBadge.setStyle("-fx-background-color: #000000; -fx-text-fill: #FFFFFF; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 999;");
        Label typeBadge = new Label(row.type.get());
        typeBadge.setStyle("-fx-background-color: #F6F6F6; -fx-text-fill: #4D4D4D; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 999;");
        metaRow.getChildren().addAll(idBadge, UITheme.severityBadge(row.severity.get()), UITheme.statusBadge(row.status.get()), typeBadge);

        Label titleLbl = new Label(row.title.get());
        titleLbl.setWrapText(true);
        titleLbl.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #000000;");

        // Description box
        VBox descCard = new VBox(6);
        descCard.setPadding(new Insets(14));
        descCard.setStyle(UITheme.softCardStyle());
        Label dLbl = UITheme.eyebrowLabel("Description");
        Label dTxt = new Label(row.description.get().isBlank() ? "No description provided." : row.description.get());
        dTxt.setWrapText(true);
        dTxt.setStyle("-fx-font-size: 13px; -fx-text-fill: #27272A; -fx-line-spacing: 3px;");
        descCard.getChildren().addAll(dLbl, dTxt);

        // Error logs box
        VBox logsCard = new VBox(8);
        logsCard.setPadding(new Insets(14));
        logsCard.setStyle(UITheme.softCardStyle());
        Label lLbl = UITheme.eyebrowLabel("Error Logs / Stack Trace");
        TextArea ta = new TextArea(row.errorLogs.get().isBlank() ? "(no error logs attached)" : row.errorLogs.get());
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefRowCount(9);
        ta.setStyle(
            "-fx-background-color: #FFFFFF; -fx-text-fill: #111827;" +
            "-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 11.5px;" +
            "-fx-border-color: #E5E7EB; -fx-border-radius: 10; -fx-background-radius: 10;"
        );
        logsCard.getChildren().addAll(lLbl, ta);

        // Inline CTA Bar
        HBox actionRow = new HBox(10);
        Button btnQ = UITheme.serkoBlackPill("Quick AI Diagnosis", true);
        btnQ.setPadding(new Insets(9, 16, 9, 16));
        btnQ.setOnAction(e -> triggerAiDiagnosis());

        Button btnM = UITheme.serkoRainbowPill("6-Agent Pipeline", true);
        btnM.setPadding(new Insets(9, 16, 9, 16));
        btnM.setOnAction(e -> triggerMultiAgentInvestigation());
        actionRow.getChildren().addAll(btnQ, btnM);

        detailsPane.getChildren().addAll(metaRow, titleLbl, descCard, logsCard, actionRow);
    }

    private void renderAiIdleState(IncidentRow row) {
        aiPane.getChildren().clear();

        VBox heroPrompt = new VBox(12);
        heroPrompt.setPadding(new Insets(20));
        heroPrompt.setStyle("-fx-background-color: " + UITheme.BG_ICE + "; -fx-background-radius: 18;");

        Label badge = UITheme.positiveBadge("Ready for #" + row.id.get());
        Label h = new Label("Run AI Root Cause Analysis");
        h.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #000000;");
        Label desc = new Label(
            "Use Quick Diagnosis for fast Groq / Gemini root cause isolation, or run the 6-Agent Pipeline to classify, score blast radius, draft fixes, and auto-save a full report."
        );
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 13px; -fx-text-fill: #3F3F46; -fx-line-spacing: 3px;");

        HBox btns = new HBox(10);
        Button b1 = UITheme.serkoBlackPill("Quick Diagnosis", true);
        b1.setPadding(new Insets(8, 16, 8, 16));
        b1.setOnAction(e -> triggerAiDiagnosis());

        Button b2 = UITheme.serkoRainbowPill("6-Agent Pipeline", true);
        b2.setPadding(new Insets(8, 16, 8, 16));
        b2.setOnAction(e -> triggerMultiAgentInvestigation());

        btns.getChildren().addAll(b1, b2);
        heroPrompt.getChildren().addAll(badge, h, desc, btns);
        aiPane.getChildren().add(heroPrompt);
    }

    // ── Data Loading ──────────────────────────────────────────────
    public void refreshIncidents() {
        String prevSelectedId = incidentTable.getSelectionModel().getSelectedItem() != null
            ? incidentTable.getSelectionModel().getSelectedItem().id.get()
            : null;

        setStatus("Loading incidents from Spring Boot API...");
        new Thread(() -> {
            try {
                HttpResponse<String> res = ApiClient.getIncidents();
                if (res.statusCode() == 200) {
                    JsonNode root = mapper.readTree(res.body());
                    JsonNode content = root.path("content");
                    ObservableList<IncidentRow> rows = FXCollections.observableArrayList();
                    long openCount = 0;
                    long critCount = 0;

                    for (JsonNode item : content) {
                        String sev = item.path("severity").asText("LOW");
                        String st  = item.path("status").asText("OPEN");
                        if ("OPEN".equalsIgnoreCase(st)) openCount++;
                        if ("CRITICAL".equalsIgnoreCase(sev)) critCount++;

                        rows.add(new IncidentRow(
                            item.path("id").asText(),
                            item.path("title").asText(),
                            sev,
                            st,
                            item.path("incidentType").asText("—"),
                            item.path("description").asText(""),
                            item.path("errorLogs").asText("")
                        ));
                    }

                    long finalOpen = openCount;
                    long finalCrit = critCount;
                    Platform.runLater(() -> {
                        incidentList.setAll(rows);
                        lblQueueCount.setText(rows.size() + " incident" + (rows.size() == 1 ? "" : "s"));
                        lblOpenChip.setText(finalOpen + " Open");
                        lblCriticalChip.setText(finalCrit + " Critical");
                        setStatus("Synced " + rows.size() + " incident" + (rows.size() == 1 ? "" : "s") +
                                  "  ·  Last updated " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

                        if (prevSelectedId != null) {
                            rows.stream()
                                .filter(r -> prevSelectedId.equals(r.id.get()))
                                .findFirst()
                                .ifPresent(r -> incidentTable.getSelectionModel().select(r));
                        } else if (!rows.isEmpty() && incidentTable.getSelectionModel().getSelectedItem() == null) {
                            incidentTable.getSelectionModel().selectFirst();
                        }
                    });
                } else {
                    Platform.runLater(() -> setStatus("Failed to load incidents (" + res.statusCode() + ")"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("Connection error: " + e.getMessage()));
            }
        }).start();
    }

    private void loadSavedInvestigations(IncidentRow row) {
        historyPane.getChildren().setAll(infoCard("Loading saved investigations...", "Fetching history for Incident #" + row.id.get(), UITheme.BG_SOFT));
        new Thread(() -> {
            try {
                HttpResponse<String> res = ApiClient.getInvestigations(Long.parseLong(row.id.get()));
                if (res.statusCode() == 200) {
                    JsonNode arr = mapper.readTree(res.body());
                    Platform.runLater(() -> renderHistoryCards(row, arr));
                } else {
                    Platform.runLater(() -> historyPane.getChildren().setAll(
                        infoCard("Could not load history", "Server returned status " + res.statusCode(), "#FEE2E2")
                    ));
                }
            } catch (Exception ex) {
                Platform.runLater(() -> historyPane.getChildren().setAll(
                    infoCard("Connection error", ex.getMessage(), "#FEE2E2")
                ));
            }
        }).start();
    }

    private void renderHistoryCards(IncidentRow row, JsonNode arr) {
        historyPane.getChildren().clear();
        if (!arr.isArray() || arr.isEmpty()) {
            historyPane.getChildren().add(infoCard(
                "No saved investigations yet",
                "Trigger the 6-Agent Investigation on Incident #" + row.id.get() + " to generate and persist a permanent root-cause report.",
                UITheme.BG_SOFT
            ));
            return;
        }

        for (JsonNode inv : arr) {
            VBox card = new VBox(8);
            card.setPadding(new Insets(14));
            card.setStyle(UITheme.softCardStyle());

            HBox top = new HBox(8);
            top.setAlignment(Pos.CENTER_LEFT);
            boolean isAi = inv.path("aiGenerated").asBoolean(false);
            Label originBadge = isAi
                ? UITheme.positiveBadge("AI Generated")
                : new Label("Manual");
            if (!isAi) {
                originBadge.setStyle("-fx-background-color: #EAEAEA; -fx-text-fill: #3F3F46; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 9; -fx-background-radius: 999;");
            }
            Label progressBadge = new Label(inv.path("progress").asText("IN_PROGRESS"));
            progressBadge.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #000000; -fx-font-size: 10.5px; -fx-font-weight: bold; -fx-padding: 3 9; -fx-background-radius: 999;");
            top.getChildren().addAll(originBadge, progressBadge);

            Label t = new Label(inv.path("title").asText("Investigation #" + inv.path("id").asText()));
            t.setWrapText(true);
            t.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #000000;");

            String rootCause = inv.path("rootCauseAnalysis").asText("");
            String steps = inv.path("resolutionSteps").asText("");

            Label rcLbl = new Label(rootCause.isBlank() ? inv.path("hypothesis").asText("") : rootCause);
            rcLbl.setWrapText(true);
            rcLbl.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #27272A;");

            card.getChildren().addAll(top, t, rcLbl);

            if (!steps.isBlank()) {
                Label stLbl = new Label("Resolution: " + steps);
                stLbl.setWrapText(true);
                stLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #158142; -fx-font-weight: 600;");
                card.getChildren().add(stLbl);
            }
            historyPane.getChildren().add(card);
        }
    }

    // ── Update Incident Status ────────────────────────────────────
    private void updateSelectedStatus(String newStatus) {
        IncidentRow sel = incidentTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        setStatus("Updating Incident #" + sel.id.get() + " -> " + newStatus + "...");
        new Thread(() -> {
            try {
                HttpResponse<String> res = ApiClient.updateIncidentStatus(Long.parseLong(sel.id.get()), newStatus);
                if (res.statusCode() == 200) {
                    Platform.runLater(() -> {
                        setStatus("Updated Incident #" + sel.id.get() + " status to " + newStatus + ".");
                        refreshIncidents();
                    });
                } else {
                    Platform.runLater(() -> setStatus("Failed to update status (" + res.statusCode() + ")"));
                }
            } catch (Exception ex) {
                Platform.runLater(() -> setStatus("Update error: " + ex.getMessage()));
            }
        }).start();
    }

    // ── AI Trigger 1: Quick Diagnosis ─────────────────────────────
    private void triggerAiDiagnosis() {
        IncidentRow sel = incidentTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            switchSubTab("ai");
            aiPane.getChildren().setAll(infoCard("Select an incident first", "Choose an incident from the table on the left before launching AI Diagnosis.", "#FEE2E2"));
            return;
        }

        switchSubTab("ai");
        lastAiIncidentId = sel.id.get();

        VBox loadingCard = new VBox(12);
        loadingCard.setPadding(new Insets(20));
        loadingCard.setStyle("-fx-background-color: " + UITheme.BG_ICE + "; -fx-background-radius: 18;");
        ProgressBar pb = new ProgressBar();
        pb.setMaxWidth(Double.MAX_VALUE);
        pb.setPrefHeight(8);
        Label l1 = new Label("Running Quick LLM Diagnosis on Incident #" + sel.id.get() + "...");
        l1.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #000000;");
        Label l2 = new Label("Extracting exception type, impacted module, and recommended fixes via Groq / Gemini.");
        l2.setWrapText(true);
        l2.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #4D4D4D;");
        loadingCard.getChildren().addAll(l1, l2, pb);
        aiPane.getChildren().setAll(loadingCard);

        setStatus("Running Quick AI diagnosis on Incident #" + sel.id.get() + "...");
        btnToolbarQuickAi.setDisable(true);

        new Thread(() -> {
            try {
                HttpResponse<String> res = ApiClient.runAiDiagnosis(Long.parseLong(sel.id.get()));
                Platform.runLater(() -> btnToolbarQuickAi.setDisable(false));
                if (res.statusCode() == 200) {
                    JsonNode n = mapper.readTree(res.body());
                    Platform.runLater(() -> {
                        renderQuickDiagnosisResult(sel, n);
                        setStatus("Quick AI diagnosis complete for Incident #" + sel.id.get() + ".");
                    });
                } else {
                    Platform.runLater(() -> {
                        aiPane.getChildren().setAll(infoCard("Diagnosis failed (" + res.statusCode() + ")", res.body(), "#FEE2E2"));
                        setStatus("Quick diagnosis failed.");
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnToolbarQuickAi.setDisable(false);
                    aiPane.getChildren().setAll(infoCard("Connection error", e.getMessage(), "#FEE2E2"));
                    setStatus("Diagnosis error.");
                });
            }
        }).start();
    }

    private void renderQuickDiagnosisResult(IncidentRow sel, JsonNode n) {
        aiPane.getChildren().clear();

        HBox topTags = new HBox(8);
        topTags.setAlignment(Pos.CENTER_LEFT);
        topTags.getChildren().addAll(
            UITheme.positiveBadge("Quick AI Complete"),
            UITheme.severityBadge(n.path("severity").asText(sel.severity.get()))
        );

        VBox summaryCard = new VBox(10);
        summaryCard.setPadding(new Insets(18));
        summaryCard.setStyle("-fx-background-color: " + UITheme.BG_ICE + "; -fx-background-radius: 18;");
        Label lblErrType = new Label(n.path("errorType").asText("Detected Exception"));
        lblErrType.setWrapText(true);
        lblErrType.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #000000;");

        Label lblComp = new Label("Affected Component: " + n.path("affectedComponent").asText("Unknown"));
        lblComp.setWrapText(true);
        lblComp.setStyle("-fx-font-size: 12.5px; -fx-font-weight: bold; -fx-text-fill: #0369A1;");

        Label lblRoot = new Label(n.path("rootCause").asText(""));
        lblRoot.setWrapText(true);
        lblRoot.setStyle("-fx-font-size: 13.5px; -fx-text-fill: #1F2937; -fx-line-spacing: 3px;");
        summaryCard.getChildren().addAll(UITheme.eyebrowLabel("Root Cause Analysis"), lblErrType, lblComp, lblRoot);

        VBox fixesWrap = new VBox(8);
        fixesWrap.getChildren().add(UITheme.eyebrowLabel("Recommended Fixes"));
        int i = 1;
        for (JsonNode fix : n.path("suggestedSolutions")) {
            HBox fixRow = new HBox(10);
            fixRow.setAlignment(Pos.CENTER_LEFT);
            fixRow.setPadding(new Insets(12, 14, 12, 14));
            fixRow.setStyle(UITheme.softCardStyle());

            Label num = new Label(String.valueOf(i++));
            num.setMinSize(24, 24);
            num.setAlignment(Pos.CENTER);
            num.setStyle("-fx-background-color: #000000; -fx-text-fill: #FFFFFF; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 999;");

            Label fTxt = new Label(fix.asText());
            fTxt.setWrapText(true);
            fTxt.setStyle("-fx-font-size: 13px; -fx-text-fill: #111827;");
            fixRow.getChildren().addAll(num, fTxt);
            fixesWrap.getChildren().add(fixRow);
        }

        Button btnEscalate = UITheme.serkoRainbowPill("Escalate to Full 6-Agent Investigation", true);
        btnEscalate.setMaxWidth(Double.MAX_VALUE);
        btnEscalate.setOnAction(e -> triggerMultiAgentInvestigation());

        aiPane.getChildren().addAll(topTags, summaryCard, fixesWrap, btnEscalate);
        workbenchScroll.setVvalue(0);
    }

    // ── AI Trigger 2: 6-Agent Orchestration Pipeline ──────────────
    private void triggerMultiAgentInvestigation() {
        IncidentRow sel = incidentTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            switchSubTab("ai");
            aiPane.getChildren().setAll(infoCard("Select an incident first", "Choose an incident from the queue before launching the 6-agent pipeline.", "#FEE2E2"));
            return;
        }

        switchSubTab("ai");
        lastAiIncidentId = sel.id.get();

        // Build Animated 6-Agent Stepper Card
        String[] stageNames = {
            "1. Log Analysis Agent",
            "2. Error Classification Agent",
            "3. Severity Evaluation Agent",
            "4. Root Cause Analysis Agent",
            "5. Resolution Planning Agent",
            "6. Report Generation Agent"
        };

        VBox stepperCard = new VBox(12);
        stepperCard.setPadding(new Insets(18));
        stepperCard.setStyle("-fx-background-color: #F6F6F6; -fx-background-radius: 18;");

        Label runHead = new Label("Orchestrating 6-Agent Pipeline · Incident #" + sel.id.get());
        runHead.setStyle("-fx-font-size: 14.5px; -fx-font-weight: bold; -fx-text-fill: #000000;");

        ProgressBar pb = new ProgressBar(0.15);
        pb.setMaxWidth(Double.MAX_VALUE);
        pb.setPrefHeight(8);

        VBox stepsList = new VBox(7);
        Label[] badgeRefs = new Label[stageNames.length];
        for (int i = 0; i < stageNames.length; i++) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(9, 12, 9, 12));
            row.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12;");

            Label name = new Label(stageNames[i]);
            name.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 600; -fx-text-fill: #111827;");
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);

            Label st = new Label(i == 0 ? "Running..." : "Queued");
            st.setStyle(i == 0
                ? "-fx-background-color: #E8F3FE; -fx-text-fill: #0369A1; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 9; -fx-background-radius: 999;"
                : "-fx-background-color: #F6F6F6; -fx-text-fill: #9CA3AF; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 9; -fx-background-radius: 999;");
            badgeRefs[i] = st;
            row.getChildren().addAll(name, sp, st);
            stepsList.getChildren().add(row);
        }

        stepperCard.getChildren().addAll(runHead, pb, stepsList);
        aiPane.getChildren().setAll(stepperCard);

        if (pipelineStepTimeline != null) pipelineStepTimeline.stop();
        final int[] activeIdx = {0};
        pipelineStepTimeline = new Timeline(new KeyFrame(Duration.millis(900), ev -> {
            if (activeIdx[0] < stageNames.length - 1) {
                badgeRefs[activeIdx[0]].setText("Completed");
                badgeRefs[activeIdx[0]].setStyle("-fx-background-color: " + UITheme.POS_BG + "; -fx-text-fill: " + UITheme.POS_INK + "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 9; -fx-background-radius: 999;");
                activeIdx[0]++;
                badgeRefs[activeIdx[0]].setText("Running...");
                badgeRefs[activeIdx[0]].setStyle("-fx-background-color: #E8F3FE; -fx-text-fill: #0369A1; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 9; -fx-background-radius: 999;");
                pb.setProgress((activeIdx[0] + 1) / (double) stageNames.length);
            }
        }));
        pipelineStepTimeline.setCycleCount(5);
        pipelineStepTimeline.play();

        setStatus("Orchestrating 6-agent pipeline on Incident #" + sel.id.get() + "...");
        btnToolbarMultiAgent.setDisable(true);

        new Thread(() -> {
            try {
                HttpResponse<String> res = ApiClient.runMultiAgentInvestigation(Long.parseLong(sel.id.get()));
                Platform.runLater(() -> {
                    if (pipelineStepTimeline != null) pipelineStepTimeline.stop();
                    btnToolbarMultiAgent.setDisable(false);
                });
                if (res.statusCode() == 200) {
                    JsonNode n = mapper.readTree(res.body());
                    Platform.runLater(() -> {
                        renderMultiAgentResult(sel, n);
                        setStatus("6-agent investigation complete and persisted for Incident #" + sel.id.get() + ".");
                        refreshIncidents();
                        loadSavedInvestigations(sel);
                    });
                } else {
                    Platform.runLater(() -> {
                        aiPane.getChildren().setAll(infoCard("Pipeline error (" + res.statusCode() + ")", res.body(), "#FEE2E2"));
                        setStatus("6-agent investigation failed.");
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (pipelineStepTimeline != null) pipelineStepTimeline.stop();
                    btnToolbarMultiAgent.setDisable(false);
                    aiPane.getChildren().setAll(infoCard("Connection error", e.getMessage(), "#FEE2E2"));
                    setStatus("Investigation error.");
                });
            }
        }).start();
    }

    private void renderMultiAgentResult(IncidentRow sel, JsonNode n) {
        aiPane.getChildren().clear();

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label doneBadge = UITheme.positiveBadge("6/6 Agents Completed · Saved");
        String sevVal = n.path("severityEvaluation").path("assessedSeverity").asText(sel.severity.get());
        topRow.getChildren().addAll(doneBadge, UITheme.severityBadge(sevVal));

        // Final Executive Summary Banner
        JsonNode fin = n.path("finalReport");
        VBox execCard = new VBox(8);
        execCard.setPadding(new Insets(18));
        execCard.setStyle("-fx-background-color: " + UITheme.BG_ICE + "; -fx-background-radius: 18;");
        Label execLbl = new Label(fin.path("executiveSummary").asText("Investigation complete."));
        execLbl.setWrapText(true);
        execLbl.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 600; -fx-text-fill: #000000; -fx-line-spacing: 3px;");
        execCard.getChildren().addAll(UITheme.eyebrowLabel("Executive Summary"), execLbl);

        // Structured Breakdown Cards
        JsonNode logA = n.path("logAnalysis");
        JsonNode cls  = n.path("errorClassification");
        JsonNode sev  = n.path("severityEvaluation");
        JsonNode rc   = n.path("rootCauseAnalysis");
        JsonNode res  = n.path("resolutionPlan");

        VBox logCard = infoCard(
            "1. Log Analysis · " + logA.path("failingMethod").asText("Trace"),
            logA.path("keyAnomaly").asText(""),
            UITheme.BG_SOFT
        );

        String clsBody = cls.path("primaryCategory").asText("") + " (" + cls.path("subType").asText("") + ")  ·  Confidence: "
            + Math.round(cls.path("confidenceScore").asDouble(0) * 100) + "%";
        VBox clsCard = infoCard("2. Classification & Blast Radius", clsBody + "\nImpact: " + sev.path("userImpact").asText(""), UITheme.BG_SOFT);

        VBox rcCard = infoCard(
            "3-4. Root Cause · " + rc.path("affectedComponent").asText("Core"),
            rc.path("underlyingCause").asText(rc.path("primaryHypothesis").asText("")),
            UITheme.BG_SOFT
        );

        VBox resCard = new VBox(8);
        resCard.setPadding(new Insets(14));
        resCard.setStyle(UITheme.softCardStyle());
        resCard.getChildren().add(UITheme.eyebrowLabel("5. Resolution Plan"));
        Label mitLbl = new Label("Immediate Mitigation: " + res.path("immediateMitigation").asText(""));
        mitLbl.setWrapText(true);
        mitLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #158142;");
        resCard.getChildren().add(mitLbl);

        for (JsonNode step : res.path("remediationSteps")) {
            Label s = new Label("•  " + step.asText());
            s.setWrapText(true);
            s.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #27272A;");
            resCard.getChildren().add(s);
        }

        String codePatch = res.path("suggestedCodePatch").asText("");
        if (!codePatch.isBlank()) {
            TextArea patchArea = new TextArea(codePatch);
            patchArea.setEditable(false);
            patchArea.setPrefRowCount(4);
            patchArea.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11.5px; -fx-background-color: #FFFFFF; -fx-border-color: #E5E7EB; -fx-border-radius: 10;");
            resCard.getChildren().add(patchArea);
        }

        // Full Formatted Report Area
        VBox fullRepCard = new VBox(8);
        fullRepCard.setPadding(new Insets(14));
        fullRepCard.setStyle(UITheme.softCardStyle());
        fullRepCard.getChildren().add(UITheme.eyebrowLabel("6. Full Markdown Report"));
        TextArea reportArea = new TextArea(fin.path("formattedMarkdownReport").asText(""));
        reportArea.setEditable(false);
        reportArea.setWrapText(true);
        reportArea.setPrefRowCount(10);
        reportArea.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11.5px; -fx-background-color: #FFFFFF; -fx-border-color: #E5E7EB; -fx-border-radius: 10;");
        fullRepCard.getChildren().add(reportArea);

        aiPane.getChildren().addAll(topRow, execCard, logCard, clsCard, rcCard, resCard, fullRepCard);
        workbenchScroll.setVvalue(0);
    }

    // ── Helpers ───────────────────────────────────────────────────
    private VBox infoCard(String title, String body, String bg) {
        VBox box = new VBox(6);
        box.setPadding(new Insets(14, 16, 14, 16));
        box.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 14;");
        Label t = new Label(title);
        t.setWrapText(true);
        t.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #000000;");
        Label b = new Label(body);
        b.setWrapText(true);
        b.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #4D4D4D; -fx-line-spacing: 2px;");
        box.getChildren().addAll(t, b);
        return box;
    }

    private void setStatus(String msg) {
        statusBarLbl.setText("●  " + msg);
    }

    private <T> TableColumn<IncidentRow, T> col(String title, double width) {
        TableColumn<IncidentRow, T> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setSortable(true);
        return col;
    }
}
