package com.agentops.UI.Views;

import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * Inside Workspace: Incident Queue + Structured AI Investigation Workbench.
 * Operate density (#FAFAFA canvas, #FFFFFF panes, #EAEAEA hairline dividers, tabular mono IDs).
 */
public class IncidentsView extends BorderPane {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ObservableList<IncidentRow> incidentList = FXCollections.observableArrayList();
    private final FilteredList<IncidentRow> filteredList = new FilteredList<>(incidentList, p -> true);
    private final TableView<IncidentRow> incidentTable = new TableView<>();

    private TextField txtSearch;
    private ComboBox<String> cmbSevFilter;
    private ComboBox<String> cmbStatFilter;

    private Label lblQueueMeta;
    private Label lblOpenChip;
    private Label lblCriticalChip;
    private Label lblSelectedId;
    private Label statusBarLbl;
    private Circle statusDot;

    // Right Workbench Panel state
    private Button subTabDetails, subTabAi, subTabHistory;
    private Button btnMarkInvestigating, btnMarkResolved;
    private Button btnToolbarQuickAi, btnToolbarMultiAgent;
    private VBox detailsPane, aiPane, historyPane;
    private StackPane workbenchStack;
    private ScrollPane workbenchScroll;

    private String lastAiIncidentId = null;
    private Timeline pipelineStepTimeline;

    public IncidentsView() {
        setupUI();
    }

    private void setupUI() {
        setPadding(new Insets(20, 24, 14, 24));
        setStyle("-fx-background-color: " + UITheme.BG_CANVAS + ";");

        setTop(buildTopSection());
        setCenter(buildSplitPane());
        setBottom(buildStatusBar());
    }

    // ── Top Header + Compact Filter Strip ─────────────────────────
    private VBox buildTopSection() {
        VBox wrap = new VBox(12);
        wrap.setPadding(new Insets(0, 0, 14, 0));

        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(3);
        Label eyebrow = UITheme.wsSectionLabel("Incident queue");
        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.BASELINE_LEFT);
        Label title = new Label("All incidents");
        title.setStyle(UITheme.headingStyle());
        lblQueueMeta = new Label("0 incidents");
        lblQueueMeta.setStyle(UITheme.monoStyle(12, UITheme.INK_MUTED));
        titleRow.getChildren().addAll(title, lblQueueMeta);
        titleBox.getChildren().addAll(eyebrow, titleRow);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnRefresh = UITheme.wsSecondaryBtn("Refresh", UITheme.refreshIcon(14, Color.web("#52525B")));
        btnRefresh.setOnAction(e -> refreshIncidents());

        btnToolbarQuickAi = UITheme.wsPrimaryBtn("Quick diagnosis", UITheme.boltIcon(14, Color.WHITE));
        btnToolbarQuickAi.setOnAction(e -> triggerAiDiagnosis());

        btnToolbarMultiAgent = UITheme.wsSecondaryBtn("6-agent investigation", UITheme.layersIcon(14, Color.web(UITheme.GRAD_BLUE)));
        btnToolbarMultiAgent.setOnAction(e -> triggerMultiAgentInvestigation());

        bar.getChildren().addAll(titleBox, spacer, btnRefresh, btnToolbarQuickAi, btnToolbarMultiAgent);

        // Single clean white hairline filter strip
        HBox filterBar = new HBox(8);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(8, 12, 8, 12));
        filterBar.setStyle(
            "-fx-background-color: #FFFFFF;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: " + UITheme.WS_BORDER + ";" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1;"
        );

        txtSearch = new TextField();
        txtSearch.setPromptText("Filter by #ID, title, category, or error log...");
        txtSearch.setStyle(
            "-fx-background-color: " + UITheme.BG_CANVAS + "; -fx-border-color: #E4E4E7; -fx-border-radius: 8; " +
            "-fx-background-radius: 8; -fx-padding: 7 12; -fx-font-size: 13px; -fx-text-fill: #09090B;"
        );
        HBox.setHgrow(txtSearch, Priority.ALWAYS);
        txtSearch.textProperty().addListener((obs, o, n) -> applyFilters());

        cmbSevFilter = new ComboBox<>();
        cmbSevFilter.getItems().addAll("All severities", "CRITICAL", "HIGH", "MEDIUM", "LOW");
        cmbSevFilter.setValue("All severities");
        cmbSevFilter.setPrefWidth(148);
        cmbSevFilter.valueProperty().addListener((obs, o, n) -> applyFilters());

        cmbStatFilter = new ComboBox<>();
        cmbStatFilter.getItems().addAll("All statuses", "OPEN", "INVESTIGATING", "RESOLVED", "CLOSED");
        cmbStatFilter.setValue("All statuses");
        cmbStatFilter.setPrefWidth(150);
        cmbStatFilter.valueProperty().addListener((obs, o, n) -> applyFilters());

        Button btnResetFilter = new Button("Reset");
        btnResetFilter.getStyleClass().add("ws-btn-ghost");
        btnResetFilter.setOnAction(e -> {
            txtSearch.clear();
            cmbSevFilter.setValue("All severities");
            cmbStatFilter.setValue("All statuses");
        });

        filterBar.getChildren().addAll(txtSearch, cmbSevFilter, cmbStatFilter, btnResetFilter);
        wrap.getChildren().addAll(bar, filterBar);
        return wrap;
    }

    // ── Split Pane: Table Left, Workbench Right ───────────────────
    private HBox buildSplitPane() {
        HBox split = new HBox(16);
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

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 16, 12, 16));
        header.setStyle(
            "-fx-background-color: #FFFFFF;" +
            "-fx-background-radius: 12 12 0 0;" +
            "-fx-border-color: transparent transparent " + UITheme.WS_BORDER + " transparent;" +
            "-fx-border-width: 0 0 1 0;"
        );

        Label lbl = new Label("Queue");
        lbl.setStyle("-fx-text-fill: #09090B; -fx-font-size: 13px; -fx-font-weight: 500;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        lblOpenChip = new Label("0 open");
        lblOpenChip.setStyle(UITheme.monoStyle(12, UITheme.INK_MUTED));

        Label sep = new Label("·");
        sep.setStyle("-fx-text-fill: #D4D4D8;");

        lblCriticalChip = new Label("0 critical");
        lblCriticalChip.setStyle(UITheme.monoStyle(12, "#991B1B"));

        header.getChildren().addAll(lbl, sp, lblOpenChip, sep, lblCriticalChip);

        buildTableColumns();
        SortedList<IncidentRow> sorted = new SortedList<>(filteredList);
        sorted.comparatorProperty().bind(incidentTable.comparatorProperty());
        incidentTable.setItems(sorted);
        incidentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        incidentTable.setFixedCellSize(46);

        // Actionable Empty State
        VBox emptyBox = new VBox(8);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setPadding(new Insets(36));
        Label emptyTitle = new Label("No incidents in queue");
        emptyTitle.setStyle("-fx-text-fill: #18181B; -fx-font-size: 13.5px; -fx-font-weight: 500;");
        Label emptyHint = new Label("Adjust your filters, refresh the queue, or report a new stack trace.");
        emptyHint.setStyle("-fx-text-fill: #71717A; -fx-font-size: 12.5px;");
        Button btnEmptyRefresh = UITheme.wsSecondaryBtn("Refresh queue", UITheme.refreshIcon(13, Color.web("#52525B")));
        btnEmptyRefresh.setOnAction(e -> refreshIncidents());
        emptyBox.getChildren().addAll(UITheme.inboxIcon(24, Color.web("#A1A1AA")), emptyTitle, emptyHint, btnEmptyRefresh);
        incidentTable.setPlaceholder(emptyBox);

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
        TableColumn<IncidentRow, String> colId = col("ID", 58);
        colId.setMaxWidth(72);
        colId.setCellValueFactory(c -> c.getValue().id);
        colId.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else { setText("#" + item); setStyle(UITheme.monoStyle(12, UITheme.INK_MUTED)); }
            }
        });

        TableColumn<IncidentRow, String> colTitle = col("Title", 240);
        colTitle.setCellValueFactory(c -> c.getValue().title);
        colTitle.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else { setText(item); setStyle("-fx-text-fill: #09090B; -fx-font-weight: 500; -fx-font-size: 13px;"); }
            }
        });

        TableColumn<IncidentRow, String> colType = col("Category", 130);
        colType.setCellValueFactory(c -> c.getValue().type);
        colType.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    setText(null);
                    setGraphic(UITheme.neutralTag(UITheme.capitalizeWord(item)));
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

        TableColumn<IncidentRow, String> colStatus = col("Status", 122);
        colStatus.setMaxWidth(140);
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
        card.setPrefWidth(465);
        card.setMinWidth(400);
        card.setStyle(UITheme.cardStyle());

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 16, 10, 16));
        header.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12 12 0 0;");

        Circle dot = new Circle(3.5, Color.web(UITheme.POS_DOT));
        Label lbl = new Label("Workbench");
        lbl.setStyle("-fx-text-fill: #09090B; -fx-font-size: 13px; -fx-font-weight: 500;");

        lblSelectedId = new Label("");
        lblSelectedId.setStyle(UITheme.monoStyle(12, UITheme.INK_MUTED));

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        btnMarkInvestigating = quickActionBtn("Investigate", "#EFF6FF", "#1D4ED8", "#BFDBFE");
        btnMarkInvestigating.setOnAction(e -> updateSelectedStatus("INVESTIGATING"));

        btnMarkResolved = quickActionBtn("Resolve", "#F0FDF4", "#166534", "#BBF7D0");
        btnMarkResolved.setOnAction(e -> updateSelectedStatus("RESOLVED"));

        btnMarkInvestigating.setDisable(true);
        btnMarkResolved.setDisable(true);

        header.getChildren().addAll(dot, lbl, lblSelectedId, sp, btnMarkInvestigating, btnMarkResolved);

        // Flat 8px Segmented Switcher (zero drop shadow)
        HBox subTabBarWrap = new HBox();
        subTabBarWrap.setPadding(new Insets(0, 16, 10, 16));
        subTabBarWrap.setStyle("-fx-border-color: transparent transparent " + UITheme.WS_BORDER + " transparent; -fx-border-width: 0 0 1 0;");

        HBox subTrack = new HBox(4);
        subTrack.setPadding(new Insets(3));
        subTrack.setStyle("-fx-background-color: #F4F4F5; -fx-background-radius: 8;");
        HBox.setHgrow(subTrack, Priority.ALWAYS);

        subTabDetails = makeSubTabBtn("Details & logs", "details");
        subTabAi      = makeSubTabBtn("AI diagnosis",   "ai");
        subTabHistory = makeSubTabBtn("Saved history",  "history");
        HBox.setHgrow(subTabDetails, Priority.ALWAYS);
        HBox.setHgrow(subTabAi, Priority.ALWAYS);
        HBox.setHgrow(subTabHistory, Priority.ALWAYS);
        subTabDetails.setMaxWidth(Double.MAX_VALUE);
        subTabAi.setMaxWidth(Double.MAX_VALUE);
        subTabHistory.setMaxWidth(Double.MAX_VALUE);

        subTrack.getChildren().addAll(subTabDetails, subTabAi, subTabHistory);
        subTabBarWrap.getChildren().add(subTrack);

        detailsPane = new VBox(14);
        detailsPane.setPadding(new Insets(16));

        aiPane = new VBox(14);
        aiPane.setPadding(new Insets(16));

        historyPane = new VBox(12);
        historyPane.setPadding(new Insets(16));

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

    private Button quickActionBtn(String text, String bg, String fg, String br) {
        Button b = new Button(text);
        b.setCursor(Cursor.HAND);
        b.setStyle(
            "-fx-background-color: " + bg + "; -fx-text-fill: " + fg + ";" +
            "-fx-border-color: " + br + "; -fx-border-width: 1; -fx-border-radius: 6;" +
            "-fx-font-size: 11.5px; -fx-font-weight: 500; -fx-padding: 4 10; -fx-background-radius: 6;"
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
                "-fx-background-color: #FFFFFF; -fx-text-fill: #09090B; -fx-font-size: 12px; " +
                "-fx-border-color: #E4E4E7; -fx-border-width: 1; -fx-border-radius: 6;" +
                "-fx-font-weight: 500; -fx-padding: 5 10; -fx-background-radius: 6;"
            );
        } else {
            b.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #71717A; -fx-font-size: 12px; " +
                "-fx-border-color: transparent; -fx-border-width: 1; " +
                "-fx-font-weight: 400; -fx-padding: 5 10; -fx-background-radius: 6;"
            );
        }
    }

    // ── Status Bar ────────────────────────────────────────────────
    private HBox buildStatusBar() {
        HBox bar = new HBox(8);
        bar.setPadding(new Insets(10, 2, 0, 2));
        bar.setAlignment(Pos.CENTER_LEFT);

        statusDot = new Circle(3.2, Color.web(UITheme.INK_DIM));
        statusBarLbl = new Label("Ready");
        statusBarLbl.setStyle("-fx-text-fill: #71717A; -fx-font-size: 12px; -fx-font-weight: 400;");
        bar.getChildren().addAll(statusDot, statusBarLbl);
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

            boolean matchesSev = sev == null || "All severities".equals(sev)
                || sev.equalsIgnoreCase(row.severity.get());

            boolean matchesStat = stat == null || "All statuses".equals(stat)
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
        lblSelectedId.setText("#" + row.id.get());

        renderDetailsPane(row);
        if (!row.id.get().equals(lastAiIncidentId)) {
            lastAiIncidentId = row.id.get();
            renderAiIdleState(row);
        }
        loadSavedInvestigations(row);
    }

    private void renderEmptySelection() {
        detailsPane.getChildren().setAll(infoCard(
            "No incident selected",
            "Select an incident from the queue on the left to inspect error logs, run Quick diagnosis, or start the 6-agent investigation.",
            UITheme.BG_CANVAS
        ));
        aiPane.getChildren().setAll(infoCard(
            "Select an incident",
            "Choose an incident row to trigger LLM triage or the 6-agent pipeline.",
            UITheme.BG_CANVAS
        ));
        historyPane.getChildren().setAll(infoCard(
            "Saved investigations",
            "Saved AI and manual investigation reports for the selected incident will appear here.",
            UITheme.BG_CANVAS
        ));
    }

    private void renderDetailsPane(IncidentRow row) {
        detailsPane.getChildren().clear();

        HBox metaRow = new HBox(6);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        Label idBadge = new Label("#" + row.id.get());
        idBadge.setStyle(
            "-fx-background-color: #09090B; -fx-text-fill: #FFFFFF; -fx-font-family: '" + UITheme.MONO_FAMILY + "', 'Consolas', monospace;" +
            "-fx-font-size: 11px; -fx-font-weight: 500; -fx-padding: 2 7; -fx-background-radius: 4;"
        );
        Label typeBadge = UITheme.neutralTag(UITheme.capitalizeWord(row.type.get()));
        metaRow.getChildren().addAll(idBadge, UITheme.severityBadge(row.severity.get()), UITheme.statusBadge(row.status.get()), typeBadge);

        Label titleLbl = new Label(row.title.get());
        titleLbl.setWrapText(true);
        titleLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: 600; -fx-text-fill: #09090B;");

        VBox descCard = new VBox(5);
        descCard.setPadding(new Insets(12));
        descCard.setStyle(UITheme.softCardStyle());
        Label dLbl = UITheme.wsSectionLabel("Description");
        Label dTxt = new Label(row.description.get().isBlank() ? "No description provided." : row.description.get());
        dTxt.setWrapText(true);
        dTxt.setStyle("-fx-font-size: 13px; -fx-text-fill: #27272A; -fx-line-spacing: 3px;");
        descCard.getChildren().addAll(dLbl, dTxt);

        VBox logsCard = new VBox(6);
        logsCard.setPadding(new Insets(12));
        logsCard.setStyle(UITheme.softCardStyle());
        Label lLbl = UITheme.wsSectionLabel("Error logs / stack trace");
        TextArea ta = new TextArea(row.errorLogs.get().isBlank() ? "(no error logs attached)" : row.errorLogs.get());
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefRowCount(9);
        ta.setStyle(UITheme.monoStyle(11.5, "#18181B"));
        logsCard.getChildren().addAll(lLbl, ta);

        HBox actionRow = new HBox(10);
        Button btnQ = UITheme.wsPrimaryBtn("Quick diagnosis", UITheme.boltIcon(13, Color.WHITE));
        btnQ.setOnAction(e -> triggerAiDiagnosis());

        Button btnM = UITheme.wsSecondaryBtn("6-agent investigation", UITheme.layersIcon(13, Color.web(UITheme.GRAD_BLUE)));
        btnM.setOnAction(e -> triggerMultiAgentInvestigation());
        actionRow.getChildren().addAll(btnQ, btnM);

        detailsPane.getChildren().addAll(metaRow, titleLbl, descCard, logsCard, actionRow);
    }

    private void renderAiIdleState(IncidentRow row) {
        aiPane.getChildren().clear();

        VBox prompt = new VBox(12);
        prompt.setPadding(new Insets(16));
        prompt.setStyle("-fx-background-color: #EFF6FF; -fx-border-color: #DBEAFE; -fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10;");

        Label badge = UITheme.neutralTag("Incident #" + row.id.get());
        Label h = new Label("Run AI root cause analysis");
        h.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #09090B;");
        Label desc = new Label(
            "Run Quick diagnosis for fast single-pass triage, or start the 6-agent pipeline to classify, evaluate impact, draft remediation fixes, and persist a Markdown report."
        );
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 13px; -fx-text-fill: #3F3F46; -fx-line-spacing: 3px;");

        HBox btns = new HBox(8);
        Button b1 = UITheme.wsPrimaryBtn("Quick diagnosis", UITheme.boltIcon(13, Color.WHITE));
        b1.setOnAction(e -> triggerAiDiagnosis());

        Button b2 = UITheme.wsSecondaryBtn("6-agent pipeline", UITheme.layersIcon(13, Color.web(UITheme.GRAD_BLUE)));
        b2.setOnAction(e -> triggerMultiAgentInvestigation());

        btns.getChildren().addAll(b1, b2);
        prompt.getChildren().addAll(badge, h, desc, btns);
        aiPane.getChildren().add(prompt);
    }

    // ── Data Loading ──────────────────────────────────────────────
    public void refreshIncidents() {
        setStatus("Loading incidents...", UITheme.INK_DIM);
        new Thread(() -> {
            try {
                HttpResponse<String> res = ApiClient.getIncidents();
                if (res.statusCode() == 200) {
                    JsonNode root = mapper.readTree(res.body());
                    Platform.runLater(() -> applyIncidentsData(root));
                } else {
                    Platform.runLater(() -> setStatus("Failed to load incidents (" + res.statusCode() + ")", UITheme.COLOR_DANGER));
                }
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("Connection error: " + e.getMessage(), UITheme.COLOR_DANGER));
            }
        }).start();
    }

    /** Populates the queue table, counters, and active selection from an /api/incidents JSON page response. */
    public void applyIncidentsData(JsonNode root) {
        String prevSelectedId = incidentTable.getSelectionModel().getSelectedItem() != null
            ? incidentTable.getSelectionModel().getSelectedItem().id.get()
            : null;

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
                item.path("incidentType").asText("General"),
                item.path("description").asText(""),
                item.path("errorLogs").asText("")
            ));
        }

        incidentList.setAll(rows);
        lblQueueMeta.setText(rows.size() + " incident" + (rows.size() == 1 ? "" : "s"));
        lblOpenChip.setText(openCount + " open");
        lblCriticalChip.setText(critCount + " critical");
        setStatus("Synced " + rows.size() + " incident" + (rows.size() == 1 ? "" : "s") +
                  " · updated " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), UITheme.POS_DOT);

        if (prevSelectedId != null) {
            rows.stream()
                .filter(r -> prevSelectedId.equals(r.id.get()))
                .findFirst()
                .ifPresent(r -> incidentTable.getSelectionModel().select(r));
        } else if (!rows.isEmpty() && incidentTable.getSelectionModel().getSelectedItem() == null) {
            incidentTable.getSelectionModel().selectFirst();
        }
    }

    private void loadSavedInvestigations(IncidentRow row) {
        historyPane.getChildren().setAll(infoCard("Loading history...", "Fetching investigation records for Incident #" + row.id.get(), UITheme.BG_CANVAS));
        new Thread(() -> {
            try {
                HttpResponse<String> res = ApiClient.getInvestigations(Long.parseLong(row.id.get()));
                if (res.statusCode() == 200) {
                    JsonNode arr = mapper.readTree(res.body());
                    Platform.runLater(() -> renderHistoryCards(row, arr));
                } else {
                    Platform.runLater(() -> historyPane.getChildren().setAll(
                        infoCard("Could not load history", "Server returned status " + res.statusCode(), "#FEF2F2")
                    ));
                }
            } catch (Exception ex) {
                Platform.runLater(() -> historyPane.getChildren().setAll(
                    infoCard("Connection error", ex.getMessage(), "#FEF2F2")
                ));
            }
        }).start();
    }

    private void renderHistoryCards(IncidentRow row, JsonNode arr) {
        historyPane.getChildren().clear();
        if (!arr.isArray() || arr.isEmpty()) {
            historyPane.getChildren().add(infoCard(
                "No saved investigations",
                "Run the 6-agent investigation on Incident #" + row.id.get() + " to generate and persist a permanent root-cause report.",
                UITheme.BG_CANVAS
            ));
            return;
        }

        for (JsonNode inv : arr) {
            VBox card = new VBox(8);
            card.setPadding(new Insets(12, 14, 12, 14));
            card.setStyle(UITheme.softCardStyle());

            HBox top = new HBox(6);
            top.setAlignment(Pos.CENTER_LEFT);
            boolean isAi = inv.path("aiGenerated").asBoolean(false);
            Label originBadge = isAi
                ? UITheme.positiveBadge("AI generated")
                : UITheme.neutralTag("Manual");
            Label progressBadge = UITheme.neutralTag(UITheme.capitalizeWord(inv.path("progress").asText("IN_PROGRESS")));
            top.getChildren().addAll(originBadge, progressBadge);

            Label t = new Label(inv.path("title").asText("Investigation #" + inv.path("id").asText()));
            t.setWrapText(true);
            t.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 600; -fx-text-fill: #09090B;");

            String rootCause = inv.path("rootCauseAnalysis").asText("");
            String steps = inv.path("resolutionSteps").asText("");

            Label rcLbl = new Label(rootCause.isBlank() ? inv.path("hypothesis").asText("") : rootCause);
            rcLbl.setWrapText(true);
            rcLbl.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #3F3F46;");

            card.getChildren().addAll(top, t, rcLbl);

            if (!steps.isBlank()) {
                Label stLbl = new Label("Resolution: " + steps);
                stLbl.setWrapText(true);
                stLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #166534; -fx-font-weight: 500;");
                card.getChildren().add(stLbl);
            }
            historyPane.getChildren().add(card);
        }
    }

    // ── Update Incident Status ────────────────────────────────────
    private void updateSelectedStatus(String newStatus) {
        IncidentRow sel = incidentTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        setStatus("Updating Incident #" + sel.id.get() + " -> " + newStatus + "...", UITheme.GRAD_BLUE);
        new Thread(() -> {
            try {
                HttpResponse<String> res = ApiClient.updateIncidentStatus(Long.parseLong(sel.id.get()), newStatus);
                if (res.statusCode() == 200) {
                    Platform.runLater(() -> {
                        setStatus("Updated Incident #" + sel.id.get() + " to " + UITheme.capitalizeWord(newStatus), UITheme.POS_DOT);
                        refreshIncidents();
                    });
                } else {
                    Platform.runLater(() -> setStatus("Failed to update status (" + res.statusCode() + ")", UITheme.COLOR_DANGER));
                }
            } catch (Exception ex) {
                Platform.runLater(() -> setStatus("Update error: " + ex.getMessage(), UITheme.COLOR_DANGER));
            }
        }).start();
    }

    // ── AI Trigger 1: Quick Diagnosis ─────────────────────────────
    private void triggerAiDiagnosis() {
        IncidentRow sel = incidentTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            switchSubTab("ai");
            aiPane.getChildren().setAll(infoCard("Select an incident", "Choose an incident row from the queue before running diagnosis.", "#FEF2F2"));
            return;
        }

        switchSubTab("ai");
        lastAiIncidentId = sel.id.get();

        VBox loadingCard = new VBox(10);
        loadingCard.setPadding(new Insets(16));
        loadingCard.setStyle("-fx-background-color: #EFF6FF; -fx-border-color: #DBEAFE; -fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10;");
        ProgressBar pb = new ProgressBar();
        pb.setMaxWidth(Double.MAX_VALUE);
        pb.setPrefHeight(6);
        Label l1 = new Label("Running Quick LLM diagnosis on Incident #" + sel.id.get() + "...");
        l1.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 600; -fx-text-fill: #09090B;");
        Label l2 = new Label("Extracting exception signature, impacted module, and recommended fixes via Groq / Gemini.");
        l2.setWrapText(true);
        l2.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #52525B;");
        loadingCard.getChildren().addAll(l1, l2, pb);
        aiPane.getChildren().setAll(loadingCard);

        setStatus("Running Quick AI diagnosis on #" + sel.id.get() + "...", UITheme.GRAD_BLUE);
        btnToolbarQuickAi.setDisable(true);

        new Thread(() -> {
            try {
                HttpResponse<String> res = ApiClient.runAiDiagnosis(Long.parseLong(sel.id.get()));
                Platform.runLater(() -> btnToolbarQuickAi.setDisable(false));
                if (res.statusCode() == 200) {
                    JsonNode n = mapper.readTree(res.body());
                    Platform.runLater(() -> {
                        renderQuickDiagnosisResult(sel, n);
                        setStatus("Quick diagnosis complete for #" + sel.id.get(), UITheme.POS_DOT);
                    });
                } else {
                    Platform.runLater(() -> {
                        aiPane.getChildren().setAll(infoCard("Diagnosis failed (" + res.statusCode() + ")", res.body(), "#FEF2F2"));
                        setStatus("Quick diagnosis failed.", UITheme.COLOR_DANGER);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnToolbarQuickAi.setDisable(false);
                    aiPane.getChildren().setAll(infoCard("Connection error", e.getMessage(), "#FEF2F2"));
                    setStatus("Diagnosis error.", UITheme.COLOR_DANGER);
                });
            }
        }).start();
    }

    private void renderQuickDiagnosisResult(IncidentRow sel, JsonNode n) {
        aiPane.getChildren().clear();

        HBox topTags = new HBox(6);
        topTags.setAlignment(Pos.CENTER_LEFT);
        topTags.getChildren().addAll(
            UITheme.positiveBadge("Quick diagnosis complete"),
            UITheme.severityBadge(n.path("severity").asText(sel.severity.get()))
        );

        VBox summaryCard = new VBox(8);
        summaryCard.setPadding(new Insets(14));
        summaryCard.setStyle("-fx-background-color: #EFF6FF; -fx-border-color: #DBEAFE; -fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10;");
        Label lblErrType = new Label(n.path("errorType").asText("Detected Exception"));
        lblErrType.setWrapText(true);
        lblErrType.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #09090B;");

        Label lblComp = new Label("Component: " + n.path("affectedComponent").asText("Unknown"));
        lblComp.setWrapText(true);
        lblComp.setStyle(UITheme.monoStyle(12, "#1D4ED8"));

        Label lblRoot = new Label(n.path("rootCause").asText(""));
        lblRoot.setWrapText(true);
        lblRoot.setStyle("-fx-font-size: 13px; -fx-text-fill: #18181B; -fx-line-spacing: 3px;");
        summaryCard.getChildren().addAll(UITheme.wsSectionLabel("Root cause analysis"), lblErrType, lblComp, lblRoot);

        VBox fixesWrap = new VBox(8);
        fixesWrap.getChildren().add(UITheme.wsSectionLabel("Recommended fixes"));
        int i = 1;
        for (JsonNode fix : n.path("suggestedSolutions")) {
            HBox fixRow = new HBox(10);
            fixRow.setAlignment(Pos.CENTER_LEFT);
            fixRow.setPadding(new Insets(10, 12, 10, 12));
            fixRow.setStyle(UITheme.softCardStyle());

            Label num = new Label(String.valueOf(i++));
            num.setMinSize(22, 22);
            num.setAlignment(Pos.CENTER);
            num.setStyle("-fx-background-color: #09090B; -fx-text-fill: #FFFFFF; -fx-font-size: 11px; -fx-font-weight: 500; -fx-background-radius: 4;");

            Label fTxt = new Label(fix.asText());
            fTxt.setWrapText(true);
            fTxt.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #18181B;");
            fixRow.getChildren().addAll(num, fTxt);
            fixesWrap.getChildren().add(fixRow);
        }

        Button btnEscalate = UITheme.wsPrimaryBtn("Run full 6-agent investigation", UITheme.layersIcon(14, Color.WHITE));
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
            aiPane.getChildren().setAll(infoCard("Select an incident", "Choose an incident from the queue before starting the 6-agent pipeline.", "#FEF2F2"));
            return;
        }

        switchSubTab("ai");
        lastAiIncidentId = sel.id.get();

        String[] stageNames = {
            "1. Log analysis agent",
            "2. Error classification agent",
            "3. Severity evaluation agent",
            "4. Root cause agent",
            "5. Resolution planning agent",
            "6. Report generation agent"
        };

        VBox stepperCard = new VBox(10);
        stepperCard.setPadding(new Insets(14));
        stepperCard.setStyle(UITheme.softCardStyle());

        Label runHead = new Label("Running 6-agent pipeline · Incident #" + sel.id.get());
        runHead.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 600; -fx-text-fill: #09090B;");

        ProgressBar pb = new ProgressBar(0.15);
        pb.setMaxWidth(Double.MAX_VALUE);
        pb.setPrefHeight(6);

        VBox stepsList = new VBox(6);
        Label[] badgeRefs = new Label[stageNames.length];
        for (int i = 0; i < stageNames.length; i++) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8, 10, 8, 10));
            row.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: " + UITheme.WS_BORDER + "; -fx-border-radius: 6; -fx-background-radius: 6;");

            Label name = new Label(stageNames[i]);
            name.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 500; -fx-text-fill: #18181B;");
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);

            Label st = new Label(i == 0 ? "Running" : "Queued");
            st.setStyle(i == 0
                ? "-fx-background-color: #EFF6FF; -fx-text-fill: #1D4ED8; -fx-font-size: 11px; -fx-font-weight: 500; -fx-padding: 2 7; -fx-background-radius: 4;"
                : "-fx-background-color: #F4F4F5; -fx-text-fill: #71717A; -fx-font-size: 11px; -fx-font-weight: 500; -fx-padding: 2 7; -fx-background-radius: 4;");
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
                badgeRefs[activeIdx[0]].setStyle("-fx-background-color: " + UITheme.POS_BG + "; -fx-text-fill: " + UITheme.POS_INK + "; -fx-font-size: 11px; -fx-font-weight: 500; -fx-padding: 2 7; -fx-background-radius: 4;");
                activeIdx[0]++;
                badgeRefs[activeIdx[0]].setText("Running");
                badgeRefs[activeIdx[0]].setStyle("-fx-background-color: #EFF6FF; -fx-text-fill: #1D4ED8; -fx-font-size: 11px; -fx-font-weight: 500; -fx-padding: 2 7; -fx-background-radius: 4;");
                pb.setProgress((activeIdx[0] + 1) / (double) stageNames.length);
            }
        }));
        pipelineStepTimeline.setCycleCount(5);
        pipelineStepTimeline.play();

        setStatus("Running 6-agent pipeline on #" + sel.id.get() + "...", UITheme.GRAD_BLUE);
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
                        setStatus("6-agent investigation saved for #" + sel.id.get(), UITheme.POS_DOT);
                        refreshIncidents();
                        loadSavedInvestigations(sel);
                    });
                } else {
                    Platform.runLater(() -> {
                        aiPane.getChildren().setAll(infoCard("Pipeline error (" + res.statusCode() + ")", res.body(), "#FEF2F2"));
                        setStatus("6-agent investigation failed.", UITheme.COLOR_DANGER);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (pipelineStepTimeline != null) pipelineStepTimeline.stop();
                    btnToolbarMultiAgent.setDisable(false);
                    aiPane.getChildren().setAll(infoCard("Connection error", e.getMessage(), "#FEF2F2"));
                    setStatus("Investigation error.", UITheme.COLOR_DANGER);
                });
            }
        }).start();
    }

    private void renderMultiAgentResult(IncidentRow sel, JsonNode n) {
        aiPane.getChildren().clear();

        HBox topRow = new HBox(6);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label doneBadge = UITheme.positiveBadge("6/6 Agents completed");
        String sevVal = n.path("severityEvaluation").path("assessedSeverity").asText(sel.severity.get());
        topRow.getChildren().addAll(doneBadge, UITheme.severityBadge(sevVal));

        JsonNode fin = n.path("finalReport");
        VBox execCard = new VBox(6);
        execCard.setPadding(new Insets(14));
        execCard.setStyle("-fx-background-color: #EFF6FF; -fx-border-color: #DBEAFE; -fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10;");
        Label execLbl = new Label(fin.path("executiveSummary").asText("Investigation complete."));
        execLbl.setWrapText(true);
        execLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: #09090B; -fx-line-spacing: 3px;");
        execCard.getChildren().addAll(UITheme.wsSectionLabel("Executive summary"), execLbl);

        JsonNode logA = n.path("logAnalysis");
        JsonNode cls  = n.path("errorClassification");
        JsonNode sev  = n.path("severityEvaluation");
        JsonNode rc   = n.path("rootCauseAnalysis");
        JsonNode res  = n.path("resolutionPlan");

        VBox logCard = infoCard(
            "1. Log analysis · " + logA.path("failingMethod").asText("Trace"),
            logA.path("keyAnomaly").asText(""),
            UITheme.BG_CANVAS
        );

        String clsBody = cls.path("primaryCategory").asText("") + " (" + cls.path("subType").asText("") + ")  ·  Confidence: "
            + Math.round(cls.path("confidenceScore").asDouble(0) * 100) + "%";
        VBox clsCard = infoCard("2. Classification & blast radius", clsBody + "\nImpact: " + sev.path("userImpact").asText(""), UITheme.BG_CANVAS);

        VBox rcCard = infoCard(
            "3-4. Root cause · " + rc.path("affectedComponent").asText("Core"),
            rc.path("underlyingCause").asText(rc.path("primaryHypothesis").asText("")),
            UITheme.BG_CANVAS
        );

        VBox resCard = new VBox(8);
        resCard.setPadding(new Insets(12));
        resCard.setStyle(UITheme.softCardStyle());
        resCard.getChildren().add(UITheme.wsSectionLabel("5. Resolution plan"));
        Label mitLbl = new Label("Immediate mitigation: " + res.path("immediateMitigation").asText(""));
        mitLbl.setWrapText(true);
        mitLbl.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 500; -fx-text-fill: #166534;");
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
            patchArea.setStyle(UITheme.monoStyle(11.5, "#18181B"));
            resCard.getChildren().add(patchArea);
        }

        VBox fullRepCard = new VBox(8);
        fullRepCard.setPadding(new Insets(12));
        fullRepCard.setStyle(UITheme.softCardStyle());
        fullRepCard.getChildren().add(UITheme.wsSectionLabel("6. Full markdown report"));
        TextArea reportArea = new TextArea(fin.path("formattedMarkdownReport").asText(""));
        reportArea.setEditable(false);
        reportArea.setWrapText(true);
        reportArea.setPrefRowCount(10);
        reportArea.setStyle(UITheme.monoStyle(11.5, "#18181B"));
        fullRepCard.getChildren().add(reportArea);

        aiPane.getChildren().addAll(topRow, execCard, logCard, clsCard, rcCard, resCard, fullRepCard);
        workbenchScroll.setVvalue(0);
    }

    // ── Helpers ───────────────────────────────────────────────────
    private VBox infoCard(String title, String body, String bg) {
        VBox box = new VBox(5);
        box.setPadding(new Insets(12, 14, 12, 14));
        box.setStyle(
            "-fx-background-color: " + bg + "; -fx-border-color: " + UITheme.WS_BORDER + ";" +
            "-fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;"
        );
        Label t = new Label(title);
        t.setWrapText(true);
        t.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: #09090B;");
        Label b = new Label(body);
        b.setWrapText(true);
        b.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #52525B; -fx-line-spacing: 2px;");
        box.getChildren().addAll(t, b);
        return box;
    }

    private void setStatus(String msg, String dotHex) {
        statusBarLbl.setText(msg);
        if (statusDot != null) {
            statusDot.setFill(Color.web(dotHex));
        }
    }

    private <T> TableColumn<IncidentRow, T> col(String title, double width) {
        TableColumn<IncidentRow, T> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setSortable(true);
        return col;
    }
}
