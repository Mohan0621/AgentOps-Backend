package com.agentops.UI.Views;

import java.io.File;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

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
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.util.Duration;

/**
 * Inside Workspace: Incident Queue + Structured AI & Collaborative Investigation Workbench.
 * Operate density (#FAFAFA canvas, #FFFFFF panes, #EAEAEA hairline dividers, tabular mono IDs).
 */
public class IncidentsView extends BorderPane {

    private static final int PAGE_SIZE = 25;

    private final ObjectMapper mapper = new ObjectMapper();
    private final ObservableList<IncidentRow> incidentList = FXCollections.observableArrayList();
    private final FilteredList<IncidentRow> filteredList = new FilteredList<>(incidentList, p -> true);
    private final TableView<IncidentRow> incidentTable = new TableView<>();

    private Consumer<String> onNotifyToast;
    private Runnable onDataChanged;

    // Filter & scope controls
    private Button btnScopeAll, btnScopeMine;
    private boolean scopeOnlyMine = false;
    private int currentPage = 0;
    private int totalPages = 1;
    private TextField txtSearch;
    private ComboBox<String> cmbSevFilter;
    private ComboBox<String> cmbStatFilter;

    // Header & pagination labels
    private Label lblQueueMeta;
    private Label lblOpenChip;
    private Label lblCriticalChip;
    private Label lblPageInfo;
    private Button btnPrevPage, btnNextPage;
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

    public void setOnNotifyToast(Consumer<String> onNotifyToast) {
        this.onNotifyToast = onNotifyToast;
    }

    public void setOnDataChanged(Runnable onDataChanged) {
        this.onDataChanged = onDataChanged;
    }

    public ObservableList<IncidentRow> getLoadedIncidents() {
        return incidentList;
    }

    public void filterBy(String severity, String status) {
        cmbSevFilter.setValue(severity != null && !severity.isBlank() ? severity : "All severities");
        cmbStatFilter.setValue(status != null && !status.isBlank() ? status : "All statuses");
        txtSearch.clear();
        applyFilters();
    }

    public void selectIncidentById(String incidentId) {
        if (incidentId == null || incidentId.isBlank()) return;
        txtSearch.clear();
        cmbSevFilter.setValue("All severities");
        cmbStatFilter.setValue("All statuses");
        applyFilters();
        for (IncidentRow r : incidentList) {
            if (incidentId.equals(r.id.get())) {
                incidentTable.getSelectionModel().select(r);
                incidentTable.scrollTo(r);
                break;
            }
        }
    }

    public void runQuickDiagnosisOnSelected() {
        triggerAiDiagnosis();
    }

    public void runMultiAgentOnSelected() {
        triggerMultiAgentInvestigation();
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

        // Clean white hairline filter strip with All | Mine scope toggle
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

        HBox scopeTrack = new HBox(3);
        scopeTrack.setPadding(new Insets(3));
        scopeTrack.setStyle("-fx-background-color: #F4F4F5; -fx-background-radius: 7;");
        btnScopeAll  = new Button("All");
        btnScopeMine = new Button("Mine");
        btnScopeAll.setCursor(Cursor.HAND);
        btnScopeMine.setCursor(Cursor.HAND);
        btnScopeAll.setOnAction(e -> setScopeOnlyMine(false));
        btnScopeMine.setOnAction(e -> setScopeOnlyMine(true));
        styleScopeTrack();
        scopeTrack.getChildren().addAll(btnScopeAll, btnScopeMine);

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
        cmbSevFilter.setPrefWidth(136);
        cmbSevFilter.valueProperty().addListener((obs, o, n) -> applyFilters());

        cmbStatFilter = new ComboBox<>();
        cmbStatFilter.getItems().addAll("All statuses", "OPEN", "INVESTIGATING", "RESOLVED", "CLOSED");
        cmbStatFilter.setValue("All statuses");
        cmbStatFilter.setPrefWidth(138);
        cmbStatFilter.valueProperty().addListener((obs, o, n) -> applyFilters());

        Button btnResetFilter = new Button("Reset");
        btnResetFilter.getStyleClass().add("ws-btn-ghost");
        btnResetFilter.setOnAction(e -> {
            txtSearch.clear();
            cmbSevFilter.setValue("All severities");
            cmbStatFilter.setValue("All statuses");
        });

        filterBar.getChildren().addAll(scopeTrack, txtSearch, cmbSevFilter, cmbStatFilter, btnResetFilter);
        wrap.getChildren().addAll(bar, filterBar);
        return wrap;
    }

    private void setScopeOnlyMine(boolean onlyMine) {
        if (this.scopeOnlyMine == onlyMine) return;
        this.scopeOnlyMine = onlyMine;
        this.currentPage = 0;
        styleScopeTrack();
        refreshIncidents();
    }

    private void styleScopeTrack() {
        String active = "-fx-background-color: #FFFFFF; -fx-text-fill: #09090B; -fx-border-color: #E4E4E7; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5; -fx-font-size: 11.5px; -fx-font-weight: 500; -fx-padding: 4 10;";
        String idle   = "-fx-background-color: transparent; -fx-text-fill: #71717A; -fx-border-color: transparent; -fx-border-width: 1; -fx-background-radius: 5; -fx-font-size: 11.5px; -fx-font-weight: 400; -fx-padding: 4 10;";
        btnScopeAll.setStyle(!scopeOnlyMine ? active : idle);
        btnScopeMine.setStyle(scopeOnlyMine ? active : idle);
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
        Label emptyHint = new Label("Adjust your filters, switch All / Mine, or report a new stack trace.");
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

        // Pagination footer
        HBox pagerBar = new HBox(8);
        pagerBar.setAlignment(Pos.CENTER_LEFT);
        pagerBar.setPadding(new Insets(8, 14, 8, 14));
        pagerBar.setStyle(
            "-fx-background-color: #FFFFFF; -fx-background-radius: 0 0 12 12;" +
            "-fx-border-color: " + UITheme.WS_BORDER + " transparent transparent transparent; -fx-border-width: 1 0 0 0;"
        );
        lblPageInfo = new Label("Page 1 of 1");
        lblPageInfo.setStyle(UITheme.monoStyle(11.5, UITheme.INK_MUTED));
        Region pSp = new Region();
        HBox.setHgrow(pSp, Priority.ALWAYS);
        btnPrevPage = new Button("< Prev");
        btnPrevPage.getStyleClass().add("ws-btn-ghost");
        btnPrevPage.setDisable(true);
        btnPrevPage.setOnAction(e -> {
            if (currentPage > 0) {
                currentPage--;
                refreshIncidents();
            }
        });
        btnNextPage = new Button("Next >");
        btnNextPage.getStyleClass().add("ws-btn-ghost");
        btnNextPage.setDisable(true);
        btnNextPage.setOnAction(e -> {
            if (currentPage < totalPages - 1) {
                currentPage++;
                refreshIncidents();
            }
        });
        pagerBar.getChildren().addAll(lblPageInfo, pSp, btnPrevPage, btnNextPage);

        card.getChildren().addAll(header, incidentTable, pagerBar);
        return card;
    }

    @SuppressWarnings("unchecked")
    private void buildTableColumns() {
        TableColumn<IncidentRow, String> colId = col("ID", 58);
        colId.setMinWidth(56);
        colId.setMaxWidth(70);
        colId.setCellValueFactory(c -> c.getValue().id);
        colId.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else { setText("#" + item); setStyle(UITheme.monoStyle(12, UITheme.INK_MUTED)); }
            }
        });

        TableColumn<IncidentRow, String> colTitle = col("Title", 172);
        colTitle.setMinWidth(106);
        colTitle.setCellValueFactory(c -> c.getValue().title);
        colTitle.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else { setText(item); setStyle("-fx-text-fill: #09090B; -fx-font-weight: 500; -fx-font-size: 13px;"); }
            }
        });

        TableColumn<IncidentRow, String> colType = col("Category", 122);
        colType.setMinWidth(112);
        colType.setMaxWidth(145);
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

        TableColumn<IncidentRow, String> colSev = col("Severity", 88);
        colSev.setMinWidth(86);
        colSev.setMaxWidth(100);
        colSev.setCellValueFactory(c -> c.getValue().severity);
        colSev.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else { setText(null); setGraphic(UITheme.severityBadge(item)); }
            }
        });

        TableColumn<IncidentRow, String> colStatus = col("Status", 114);
        colStatus.setMinWidth(112);
        colStatus.setMaxWidth(128);
        colStatus.setCellValueFactory(c -> c.getValue().status);
        colStatus.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else { setText(null); setGraphic(UITheme.statusBadge(item)); }
            }
        });

        incidentTable.getColumns().addAll(colId, colTitle, colType, colSev, colStatus);
        incidentTable.widthProperty().addListener((obs, oldW, newW) -> {
            double w = newW.doubleValue();
            if (w > 10) {
                boolean showCat = w >= 480;
                if (colType.isVisible() != showCat) {
                    colType.setVisible(showCat);
                }
            }
        });
    }

    // ── Right Investigation Workbench ─────────────────────────────
    private VBox buildWorkbenchCard() {
        VBox card = new VBox(0);
        card.setPrefWidth(442);
        card.setMinWidth(356);
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

        detailsPane = new VBox(12);
        detailsPane.setPadding(new Insets(16));

        aiPane = new VBox(12);
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
                || safeLower(row.errorLogs.get()).contains(q)
                || safeLower(row.createdByEmail.get()).contains(q);

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
        btnMarkInvestigating.setDisable(true);
        btnMarkResolved.setDisable(true);
        lblSelectedId.setText("");
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

    // ── Details & Logs Tab (Metadata + Inline Edit + Delete + Copy Logs) ──
    private void renderDetailsPane(IncidentRow row) {
        detailsPane.getChildren().clear();

        HBox metaRow = new HBox(8);
        metaRow.setAlignment(Pos.TOP_LEFT);

        Label idBadge = new Label("#" + row.id.get());
        idBadge.setMinWidth(Region.USE_PREF_SIZE);
        idBadge.setStyle(
            "-fx-background-color: #09090B; -fx-text-fill: #FFFFFF; -fx-font-family: '" + UITheme.MONO_FAMILY + "', 'Consolas', monospace;" +
            "-fx-font-size: 11px; -fx-font-weight: 500; -fx-padding: 2 7; -fx-background-radius: 4;"
        );
        Label sevBadge = UITheme.severityBadge(row.severity.get());
        sevBadge.setMinWidth(Region.USE_PREF_SIZE);
        Label statBadge = UITheme.statusBadge(row.status.get());
        statBadge.setMinWidth(Region.USE_PREF_SIZE);
        Label typeBadge = UITheme.neutralTag(UITheme.capitalizeWord(row.type.get()));
        typeBadge.setMinWidth(Region.USE_PREF_SIZE);

        FlowPane tagsFlow = new FlowPane(6, 5);
        tagsFlow.setAlignment(Pos.CENTER_LEFT);
        tagsFlow.getChildren().addAll(idBadge, sevBadge, statBadge, typeBadge);
        HBox.setHgrow(tagsFlow, Priority.ALWAYS);

        Button btnEdit = miniIconBtn("Edit", UITheme.editIcon(12, Color.web("#52525B")));
        btnEdit.setMinWidth(Region.USE_PREF_SIZE);
        Button btnDelete = miniIconBtn("Delete", UITheme.trashIcon(12, Color.web("#DC2626")));
        btnDelete.setMinWidth(Region.USE_PREF_SIZE);
        String currentUser = ApiClient.getCurrentUserEmail();
        boolean canEdit = row.createdByEmail.get().isBlank() || currentUser == null
            || currentUser.equalsIgnoreCase(row.createdByEmail.get())
            || currentUser.equalsIgnoreCase(row.assignedToEmail.get());
        boolean canDelete = row.createdByEmail.get().isBlank() || currentUser == null
            || currentUser.equalsIgnoreCase(row.createdByEmail.get());
        if (!canEdit) {
            btnEdit.setDisable(true);
            btnEdit.setTooltip(new Tooltip("Only creator or assignee can edit this incident"));
        }
        if (!canDelete) {
            btnDelete.setDisable(true);
            btnDelete.setTooltip(new Tooltip("Only the incident creator can delete this incident"));
        }

        HBox actionBtns = new HBox(6, btnEdit, btnDelete);
        actionBtns.setAlignment(Pos.CENTER_RIGHT);
        actionBtns.setMinWidth(Region.USE_PREF_SIZE);

        metaRow.getChildren().addAll(tagsFlow, actionBtns);

        // Inline delete confirm strip (hidden until Delete clicked)
        HBox deleteConfirmStrip = new HBox(8);
        deleteConfirmStrip.setAlignment(Pos.CENTER_LEFT);
        deleteConfirmStrip.setPadding(new Insets(10, 12, 10, 12));
        deleteConfirmStrip.setStyle("-fx-background-color: #FEF2F2; -fx-border-color: #FECACA; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");
        deleteConfirmStrip.setVisible(false);
        deleteConfirmStrip.setManaged(false);
        Label delPrompt = new Label("Delete #" + row.id.get() + " permanently?");
        delPrompt.setStyle("-fx-font-size: 12px; -fx-font-weight: 500; -fx-text-fill: #991B1B;");
        Region dSp = new Region(); HBox.setHgrow(dSp, Priority.ALWAYS);
        Button btnConfirmDel = quickActionBtn("Confirm delete", "#DC2626", "#FFFFFF", "#DC2626");
        Button btnCancelDel  = UITheme.wsSecondaryBtn("Cancel", null);
        btnCancelDel.setPadding(new Insets(4, 10, 4, 10));
        deleteConfirmStrip.getChildren().addAll(delPrompt, dSp, btnCancelDel, btnConfirmDel);

        btnDelete.setOnAction(e -> {
            boolean show = !deleteConfirmStrip.isVisible();
            deleteConfirmStrip.setVisible(show);
            deleteConfirmStrip.setManaged(show);
        });
        btnCancelDel.setOnAction(e -> {
            deleteConfirmStrip.setVisible(false);
            deleteConfirmStrip.setManaged(false);
        });
        btnConfirmDel.setOnAction(e -> deleteSelectedIncident(row, btnConfirmDel));

        Label titleLbl = new Label(row.title.get());
        titleLbl.setMnemonicParsing(false);
        titleLbl.setWrapText(true);
        titleLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: 600; -fx-text-fill: #09090B;");

        // Metadata ownership / timeline bar
        FlowPane ownershipBar = new FlowPane(12, 4);
        ownershipBar.setPadding(new Insets(8, 12, 8, 12));
        ownershipBar.setStyle(UITheme.softCardStyle());
        ownershipBar.getChildren().addAll(
            metaChip("Reporter", row.createdByEmail.get().isBlank() ? "—" : row.createdByEmail.get()),
            metaChip("Assignee", row.assignedToEmail.get().isBlank() ? "Unassigned" : row.assignedToEmail.get()),
            metaChip("Created", formatIso(row.createdAt.get())),
            metaChip("Updated", formatIso(row.updatedAt.get()))
        );

        VBox descCard = new VBox(5);
        descCard.setPadding(new Insets(12));
        descCard.setStyle(UITheme.softCardStyle());
        Label dLbl = UITheme.wsSectionLabel("Description");
        Label dTxt = new Label(row.description.get().isBlank() ? "No description provided." : row.description.get());
        dTxt.setMnemonicParsing(false);
        dTxt.setWrapText(true);
        dTxt.setStyle("-fx-font-size: 13px; -fx-text-fill: #27272A; -fx-line-spacing: 3px;");
        descCard.getChildren().addAll(dLbl, dTxt);

        VBox logsCard = new VBox(6);
        logsCard.setPadding(new Insets(12));
        logsCard.setStyle(UITheme.softCardStyle());
        HBox logsHead = new HBox(8, UITheme.wsSectionLabel("Error logs / stack trace"), new Region());
        logsHead.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(logsHead.getChildren().get(1), Priority.ALWAYS);
        Button btnCopyLogs = miniIconBtn("Copy logs", UITheme.copyIcon(12, Color.web("#52525B")));
        btnCopyLogs.setOnAction(e -> copyToClipboard(row.errorLogs.get(), "Copied error logs to clipboard"));
        logsHead.getChildren().add(btnCopyLogs);

        TextArea ta = new TextArea(row.errorLogs.get().isBlank() ? "(no error logs attached)" : row.errorLogs.get());
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefRowCount(8);
        ta.setStyle(UITheme.monoStyle(11.5, "#18181B"));
        logsCard.getChildren().addAll(logsHead, ta);

        // Inline Edit Form (hidden until Edit clicked)
        VBox editBox = buildInlineEditCard(row);
        editBox.setVisible(false);
        editBox.setManaged(false);
        btnEdit.setOnAction(e -> {
            boolean editing = !editBox.isVisible();
            editBox.setVisible(editing);
            editBox.setManaged(editing);
            descCard.setVisible(!editing);
            descCard.setManaged(!editing);
            logsCard.setVisible(!editing);
            logsCard.setManaged(!editing);
        });

        HBox actionRow = new HBox(10);
        Button btnQ = UITheme.wsPrimaryBtn("Quick diagnosis", UITheme.boltIcon(13, Color.WHITE));
        btnQ.setOnAction(e -> triggerAiDiagnosis());

        Button btnM = UITheme.wsSecondaryBtn("6-agent investigation", UITheme.layersIcon(13, Color.web(UITheme.GRAD_BLUE)));
        btnM.setOnAction(e -> triggerMultiAgentInvestigation());
        actionRow.getChildren().addAll(btnQ, btnM);

        detailsPane.getChildren().addAll(metaRow, deleteConfirmStrip, titleLbl, ownershipBar, editBox, descCard, logsCard, actionRow);
    }

    private VBox buildInlineEditCard(IncidentRow row) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #DBEAFE; -fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10;");

        TextField tfTitle = new TextField(row.title.get());
        TextField tfType  = new TextField(row.type.get());
        ComboBox<String> cbSev = new ComboBox<>();
        cbSev.getItems().addAll("LOW", "MEDIUM", "HIGH", "CRITICAL");
        cbSev.setValue(row.severity.get().isBlank() ? "MEDIUM" : row.severity.get());
        cbSev.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> cbStat = new ComboBox<>();
        cbStat.getItems().addAll("OPEN", "INVESTIGATING", "RESOLVED", "CLOSED");
        cbStat.setValue(row.status.get().isBlank() ? "OPEN" : row.status.get());
        cbStat.setMaxWidth(Double.MAX_VALUE);

        HBox rowFields = new HBox(8,
            UITheme.formField("Severity", cbSev),
            UITheme.formField("Status", cbStat),
            UITheme.formField("Category", tfType)
        );
        for (var n : rowFields.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);

        TextArea taDesc = new TextArea(row.description.get());
        taDesc.setPrefRowCount(3);
        taDesc.setWrapText(true);

        TextArea taLogs = new TextArea(row.errorLogs.get());
        taLogs.setPrefRowCount(5);
        taLogs.setWrapText(true);
        taLogs.setStyle(UITheme.monoStyle(11.5, "#18181B"));

        Button btnSave = UITheme.wsPrimaryBtn("Save changes", null);
        Button btnCancel = UITheme.wsSecondaryBtn("Cancel", null);
        btnCancel.setOnAction(e -> renderDetailsPane(row));

        btnSave.setOnAction(e -> {
            btnSave.setDisable(true);
            btnSave.setText("Saving...");
            String newTitle = tfTitle.getText().trim();
            String newDesc  = taDesc.getText().trim();
            String newLogs  = taLogs.getText().trim();
            String newSev   = cbSev.getValue();
            String newStat  = cbStat.getValue();
            String newType  = tfType.getText().trim();
            setStatus("Saving edits to #" + row.id.get() + "...", UITheme.GRAD_BLUE);

            new Thread(() -> {
                try {
                    HttpResponse<String> res = ApiClient.updateIncident(
                        Long.parseLong(row.id.get()), newTitle, newDesc, newLogs, newSev, newStat, newType
                    );
                    if (res.statusCode() == 200) {
                        Platform.runLater(() -> {
                            setStatus("Saved changes to Incident #" + row.id.get(), UITheme.POS_DOT);
                            notifyToast("Saved Incident #" + row.id.get());
                            refreshIncidents();
                            if (onDataChanged != null) onDataChanged.run();
                        });
                    } else {
                        String err = extractServerError(res.body(), "Update failed (" + res.statusCode() + ")");
                        Platform.runLater(() -> {
                            btnSave.setDisable(false);
                            btnSave.setText("Save changes");
                            setStatus(err, UITheme.COLOR_DANGER);
                            notifyToast(err);
                        });
                    }
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        btnSave.setDisable(false);
                        btnSave.setText("Save changes");
                        setStatus("Save error: " + ex.getMessage(), UITheme.COLOR_DANGER);
                    });
                }
            }).start();
        });

        HBox btns = new HBox(8, btnSave, btnCancel);
        box.getChildren().addAll(
            UITheme.wsSectionLabel("Edit incident"),
            UITheme.formField("Title", tfTitle),
            rowFields,
            UITheme.formField("Description", taDesc),
            UITheme.formField("Error logs", taLogs),
            btns
        );
        return box;
    }

    private void deleteSelectedIncident(IncidentRow row, Button triggerBtn) {
        triggerBtn.setDisable(true);
        setStatus("Deleting Incident #" + row.id.get() + "...", UITheme.COLOR_WARNING);
        new Thread(() -> {
            try {
                HttpResponse<String> res = ApiClient.deleteIncident(Long.parseLong(row.id.get()));
                if (res.statusCode() == 200 || res.statusCode() == 204) {
                    Platform.runLater(() -> {
                        setStatus("Deleted Incident #" + row.id.get(), UITheme.POS_DOT);
                        notifyToast("Deleted Incident #" + row.id.get());
                        incidentTable.getSelectionModel().clearSelection();
                        renderEmptySelection();
                        refreshIncidents();
                        if (onDataChanged != null) onDataChanged.run();
                    });
                } else {
                    String err = extractServerError(res.body(), "Delete failed (" + res.statusCode() + ")");
                    Platform.runLater(() -> {
                        triggerBtn.setDisable(false);
                        setStatus(err, UITheme.COLOR_DANGER);
                        notifyToast(err);
                    });
                }
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    triggerBtn.setDisable(false);
                    setStatus("Delete error: " + ex.getMessage(), UITheme.COLOR_DANGER);
                });
            }
        }).start();
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
        final boolean onlyMine = scopeOnlyMine;
        final int page = currentPage;
        new Thread(() -> {
            try {
                HttpResponse<String> res = ApiClient.getIncidents(onlyMine, page, PAGE_SIZE);
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
                item.path("errorLogs").asText(""),
                item.path("createdByEmail").asText(""),
                item.path("assignedToEmail").asText(""),
                item.path("createdAt").asText(""),
                item.path("updatedAt").asText("")
            ));
        }

        incidentList.setAll(rows);

        int pageNum = root.path("number").asInt(0);
        int totPages = Math.max(1, root.path("totalPages").asInt(1));
        long totElems = root.path("totalElements").asLong(rows.size());
        this.currentPage = pageNum;
        this.totalPages = totPages;

        lblQueueMeta.setText(totElems + " incident" + (totElems == 1 ? "" : "s"));
        lblOpenChip.setText(openCount + " open");
        lblCriticalChip.setText(critCount + " critical");
        lblPageInfo.setText("Page " + (pageNum + 1) + " of " + totPages + " · " + totElems + " total");
        btnPrevPage.setDisable(root.path("first").asBoolean(pageNum <= 0));
        btnNextPage.setDisable(root.path("last").asBoolean(pageNum >= totPages - 1));

        setStatus("Synced " + rows.size() + " incident" + (rows.size() == 1 ? "" : "s") +
                  " · updated " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), UITheme.POS_DOT);

        if (prevSelectedId != null) {
            rows.stream()
                .filter(r -> prevSelectedId.equals(r.id.get()))
                .findFirst()
                .ifPresentOrElse(
                    r -> incidentTable.getSelectionModel().select(r),
                    () -> { if (!rows.isEmpty()) incidentTable.getSelectionModel().selectFirst(); }
                );
        } else if (!rows.isEmpty() && incidentTable.getSelectionModel().getSelectedItem() == null) {
            incidentTable.getSelectionModel().selectFirst();
        }
    }

    // ── Saved History + Manual Investigation + Comments ─────────────
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

        // Top action bar + inline manual investigation composer
        HBox histHead = new HBox(8);
        histHead.setAlignment(Pos.CENTER_LEFT);
        Label hTitle = new Label("Investigation timeline (" + (arr.isArray() ? arr.size() : 0) + ")");
        hTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #09090B;");
        Region hSp = new Region(); HBox.setHgrow(hSp, Priority.ALWAYS);
        Button btnNewInv = UITheme.wsSecondaryBtn("+ Manual investigation", null);
        btnNewInv.setPadding(new Insets(5, 11, 5, 11));
        histHead.getChildren().addAll(hTitle, hSp, btnNewInv);

        VBox composerBox = buildManualInvestigationForm(row);
        composerBox.setVisible(false);
        composerBox.setManaged(false);
        btnNewInv.setOnAction(e -> {
            boolean show = !composerBox.isVisible();
            composerBox.setVisible(show);
            composerBox.setManaged(show);
        });

        historyPane.getChildren().addAll(histHead, composerBox);

        if (!arr.isArray() || arr.isEmpty()) {
            historyPane.getChildren().add(infoCard(
                "No saved investigations",
                "Run the 6-agent investigation or log a manual investigation above for Incident #" + row.id.get() + ".",
                UITheme.BG_CANVAS
            ));
            return;
        }

        for (JsonNode inv : arr) {
            long invId = inv.path("id").asLong(-1);
            VBox card = new VBox(8);
            card.setPadding(new Insets(12, 14, 12, 14));
            card.setStyle(UITheme.softCardStyle());

            HBox top = new HBox(6);
            top.setAlignment(Pos.CENTER_LEFT);
            boolean isAi = inv.path("aiGenerated").asBoolean(false);
            Label originBadge = isAi
                ? UITheme.positiveBadge("AI generated")
                : UITheme.neutralTag("Manual");

            String currentProgress = inv.path("progress").asText("STARTED");
            ComboBox<String> cmbProg = new ComboBox<>();
            cmbProg.getItems().addAll("STARTED", "IN_PROGRESS", "HYPOTHESIS_TESTING", "ROOT_CAUSE_FOUND", "RESOLVED");
            if (!cmbProg.getItems().contains(currentProgress)) {
                cmbProg.getItems().add(currentProgress);
            }
            cmbProg.setValue(currentProgress);
            cmbProg.setStyle("-fx-font-size: 11px; -fx-padding: 1 6;");
            final String[] committedProg = {currentProgress};
            cmbProg.setOnAction(ev -> {
                String nextProg = cmbProg.getValue();
                if (invId < 0 || nextProg == null || nextProg.equals(committedProg[0])) return;
                cmbProg.setDisable(true);
                new Thread(() -> {
                    try {
                        HttpResponse<String> r = ApiClient.updateInvestigation(invId, nextProg, null, null, null, null);
                        Platform.runLater(() -> {
                            cmbProg.setDisable(false);
                            if (r.statusCode() == 200) {
                                committedProg[0] = nextProg;
                                setStatus("Investigation #" + invId + " -> " + nextProg, UITheme.POS_DOT);
                                notifyToast("Updated investigation progress");
                            } else {
                                cmbProg.setValue(committedProg[0]);
                                setStatus("Progress update failed (" + r.statusCode() + ")", UITheme.COLOR_DANGER);
                            }
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> {
                            cmbProg.setDisable(false);
                            cmbProg.setValue(committedProg[0]);
                            setStatus("Progress update error: " + ex.getMessage(), UITheme.COLOR_DANGER);
                        });
                    }
                }).start();
            });

            Region topSp = new Region(); HBox.setHgrow(topSp, Priority.ALWAYS);
            Label byLbl = new Label(inv.path("investigatedByEmail").asText("") + " · " + formatIso(inv.path("createdAt").asText("")));
            byLbl.setMnemonicParsing(false);
            byLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #71717A;");

            top.getChildren().addAll(originBadge, cmbProg, topSp, byLbl);

            Label t = new Label(inv.path("title").asText("Investigation #" + inv.path("id").asText()));
            t.setMnemonicParsing(false);
            t.setWrapText(true);
            t.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 600; -fx-text-fill: #09090B;");

            card.getChildren().addAll(top, t);

            String hypothesis = inv.path("hypothesis").asText("");
            String notes = inv.path("notes").asText("");
            String rootCause = inv.path("rootCauseAnalysis").asText("");
            String steps = inv.path("resolutionSteps").asText("");
            String attachedLogs = inv.path("attachedLogs").asText("");

            if (!hypothesis.isBlank()) {
                Label hLbl = new Label("Hypothesis: " + hypothesis);
                hLbl.setMnemonicParsing(false);
                hLbl.setWrapText(true);
                hLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #1D4ED8;");
                card.getChildren().add(hLbl);
            }
            if (!notes.isBlank()) {
                Label nLbl = new Label(notes);
                nLbl.setMnemonicParsing(false);
                nLbl.setWrapText(true);
                nLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #52525B;");
                card.getChildren().add(nLbl);
            }
            if (!rootCause.isBlank()) {
                Label rcLbl = new Label("Root cause: " + rootCause);
                rcLbl.setMnemonicParsing(false);
                rcLbl.setWrapText(true);
                rcLbl.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #18181B;");
                card.getChildren().add(rcLbl);
            }
            if (!steps.isBlank()) {
                Label stLbl = new Label("Resolution: " + steps);
                stLbl.setMnemonicParsing(false);
                stLbl.setWrapText(true);
                stLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #166534; -fx-font-weight: 500;");
                card.getChildren().add(stLbl);
            }
            if (!attachedLogs.isBlank()) {
                Button toggleLogs = miniIconBtn("Show attached logs", UITheme.terminalIcon(12, Color.web("#52525B")));
                TextArea logPreview = new TextArea(attachedLogs);
                logPreview.setEditable(false);
                logPreview.setWrapText(true);
                logPreview.setPrefRowCount(4);
                logPreview.setStyle(UITheme.monoStyle(11, "#18181B"));
                logPreview.setVisible(false);
                logPreview.setManaged(false);
                toggleLogs.setOnAction(e -> {
                    boolean show = !logPreview.isVisible();
                    logPreview.setVisible(show);
                    logPreview.setManaged(show);
                    toggleLogs.setText(show ? "Hide attached logs" : "Show attached logs");
                });
                card.getChildren().addAll(toggleLogs, logPreview);
            }

            // Collaboration Comments Thread + Composer
            if (invId >= 0) {
                card.getChildren().add(buildCommentsBlock(invId, inv.path("comments")));
            }

            historyPane.getChildren().add(card);
        }
    }

    private VBox buildManualInvestigationForm(IncidentRow row) {
        VBox form = new VBox(8);
        form.setPadding(new Insets(12));
        form.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #DBEAFE; -fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10;");

        TextField tfTitle = new TextField("Manual Investigation · #" + row.id.get());
        ComboBox<String> cbProg = new ComboBox<>();
        cbProg.getItems().addAll("STARTED", "IN_PROGRESS", "HYPOTHESIS_TESTING", "ROOT_CAUSE_FOUND", "RESOLVED");
        cbProg.setValue("IN_PROGRESS");
        cbProg.setMaxWidth(Double.MAX_VALUE);

        TextField tfHyp = new TextField();
        tfHyp.setPromptText("Working hypothesis...");

        TextArea taNotes = new TextArea();
        taNotes.setPromptText("Observations, checked pods, DB metrics...");
        taNotes.setPrefRowCount(2);
        taNotes.setWrapText(true);

        TextField tfRc = new TextField();
        tfRc.setPromptText("Confirmed root cause (optional)");

        Button btnSave = UITheme.wsPrimaryBtn("Create investigation", null);
        btnSave.setOnAction(e -> {
            if (tfTitle.getText().isBlank()) return;
            btnSave.setDisable(true);
            btnSave.setText("Saving...");
            new Thread(() -> {
                try {
                    HttpResponse<String> r = ApiClient.createInvestigation(
                        Long.parseLong(row.id.get()),
                        tfTitle.getText().trim(),
                        tfHyp.getText().trim(),
                        taNotes.getText().trim(),
                        "",
                        tfRc.getText().trim(),
                        "",
                        cbProg.getValue()
                    );
                    Platform.runLater(() -> {
                        btnSave.setDisable(false);
                        btnSave.setText("Create investigation");
                        if (r.statusCode() == 201 || r.statusCode() == 200) {
                            notifyToast("Manual investigation added");
                            refreshIncidents();
                            loadSavedInvestigations(row);
                            if (onDataChanged != null) onDataChanged.run();
                        } else {
                            setStatus("Failed to save investigation (" + r.statusCode() + ")", UITheme.COLOR_DANGER);
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        btnSave.setDisable(false);
                        btnSave.setText("Create investigation");
                        setStatus("Error: " + ex.getMessage(), UITheme.COLOR_DANGER);
                    });
                }
            }).start();
        });

        form.getChildren().addAll(
            UITheme.wsSectionLabel("Log manual investigation"),
            UITheme.formField("Title", tfTitle),
            UITheme.formField("Progress stage", cbProg),
            UITheme.formField("Hypothesis", tfHyp),
            UITheme.formField("Investigation notes", taNotes),
            UITheme.formField("Root cause summary", tfRc),
            btnSave
        );
        return form;
    }

    private VBox buildCommentsBlock(long invId, JsonNode commentsNode) {
        VBox wrap = new VBox(6);
        wrap.setPadding(new Insets(8, 0, 0, 0));
        wrap.setStyle("-fx-border-color: " + UITheme.WS_BORDER + " transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        int count = commentsNode.isArray() ? commentsNode.size() : 0;
        HBox cHead = new HBox(6, UITheme.messageSquareIcon(13, Color.web("#71717A")), new Label("Team comments (" + count + ")"));
        cHead.setAlignment(Pos.CENTER_LEFT);
        ((Label) cHead.getChildren().get(1)).setStyle("-fx-font-size: 11.5px; -fx-font-weight: 500; -fx-text-fill: #52525B;");

        VBox listBox = new VBox(5);
        if (commentsNode.isArray()) {
            for (JsonNode c : commentsNode) {
                listBox.getChildren().add(commentBubble(
                    c.path("authorEmail").asText("engineer"),
                    c.path("createdAt").asText(""),
                    c.path("content").asText("")
                ));
            }
        }

        TextField tfComment = new TextField();
        tfComment.setPromptText("Post team finding or follow-up comment...");
        tfComment.setStyle("-fx-padding: 6 10; -fx-font-size: 12px;");
        HBox.setHgrow(tfComment, Priority.ALWAYS);

        Button btnPost = UITheme.wsSecondaryBtn("Post", null);
        btnPost.setPadding(new Insets(5, 11, 5, 11));

        Runnable doPost = () -> {
            String msg = tfComment.getText().trim();
            if (msg.isEmpty()) return;
            btnPost.setDisable(true);
            new Thread(() -> {
                try {
                    HttpResponse<String> r = ApiClient.addComment(invId, msg);
                    if (r.statusCode() == 201 || r.statusCode() == 200) {
                        JsonNode added = mapper.readTree(r.body());
                        Platform.runLater(() -> {
                            tfComment.clear();
                            btnPost.setDisable(false);
                            listBox.getChildren().add(commentBubble(
                                added.path("authorEmail").asText(ApiClient.getCurrentUserEmail() != null ? ApiClient.getCurrentUserEmail() : "you"),
                                added.path("createdAt").asText(""),
                                added.path("content").asText(msg)
                            ));
                            ((Label) cHead.getChildren().get(1)).setText("Team comments (" + listBox.getChildren().size() + ")");
                            notifyToast("Comment posted");
                        });
                    } else {
                        Platform.runLater(() -> {
                            btnPost.setDisable(false);
                            setStatus("Comment post failed (" + r.statusCode() + ")", UITheme.COLOR_DANGER);
                        });
                    }
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        btnPost.setDisable(false);
                        setStatus("Comment error: " + ex.getMessage(), UITheme.COLOR_DANGER);
                    });
                }
            }).start();
        };
        btnPost.setOnAction(e -> doPost.run());
        tfComment.setOnAction(e -> doPost.run());

        HBox composer = new HBox(6, tfComment, btnPost);
        composer.setAlignment(Pos.CENTER_LEFT);
        wrap.getChildren().addAll(cHead, listBox, composer);
        return wrap;
    }

    private VBox commentBubble(String author, String ts, String text) {
        VBox b = new VBox(2);
        b.setPadding(new Insets(6, 9, 6, 9));
        b.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: " + UITheme.WS_BORDER + "; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;");
        Label meta = new Label(author + " · " + formatIso(ts));
        meta.setMnemonicParsing(false);
        meta.setStyle("-fx-font-size: 10.5px; -fx-text-fill: #71717A;");
        Label body = new Label(text);
        body.setMnemonicParsing(false);
        body.setWrapText(true);
        body.setStyle("-fx-font-size: 12px; -fx-text-fill: #18181B;");
        b.getChildren().addAll(meta, body);
        return b;
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
                        notifyToast("Incident #" + sel.id.get() + " marked " + UITheme.capitalizeWord(newStatus));
                        refreshIncidents();
                        if (onDataChanged != null) onDataChanged.run();
                    });
                } else {
                    String err = extractServerError(res.body(), "Failed to update status (" + res.statusCode() + ")");
                    Platform.runLater(() -> {
                        setStatus(err, UITheme.COLOR_DANGER);
                        notifyToast(err);
                    });
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
        lblErrType.setMnemonicParsing(false);
        lblErrType.setWrapText(true);
        lblErrType.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #09090B;");

        Label lblComp = new Label("Component: " + n.path("affectedComponent").asText("Unknown"));
        lblComp.setMnemonicParsing(false);
        lblComp.setWrapText(true);
        lblComp.setStyle(UITheme.monoStyle(12, "#1D4ED8"));

        Label lblRoot = new Label(n.path("rootCause").asText(""));
        lblRoot.setMnemonicParsing(false);
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
            fTxt.setMnemonicParsing(false);
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
                        notifyToast("6-agent investigation saved for #" + sel.id.get());
                        refreshIncidents();
                        loadSavedInvestigations(sel);
                        if (onDataChanged != null) onDataChanged.run();
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

        JsonNode logA = n.path("logAnalysis");
        JsonNode cls  = n.path("errorClassification");
        JsonNode sev  = n.path("severityEvaluation");
        JsonNode rc   = n.path("rootCauseAnalysis");
        JsonNode res  = n.path("resolutionPlan");
        JsonNode fin  = n.path("finalReport");

        HBox topRow = new HBox(6);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label doneBadge = UITheme.positiveBadge("6/6 Agents completed");
        String sevVal = sev.path("assessedSeverity").asText(sel.severity.get());
        Label transientBadge = UITheme.neutralTag(cls.path("isTransient").asBoolean(false) ? "Transient" : "Persistent");
        Label catBadge = UITheme.neutralTag(UITheme.capitalizeWord(cls.path("primaryCategory").asText("General")));
        topRow.getChildren().addAll(doneBadge, UITheme.severityBadge(sevVal), transientBadge, catBadge);

        VBox execCard = new VBox(6);
        execCard.setPadding(new Insets(14));
        execCard.setStyle("-fx-background-color: #EFF6FF; -fx-border-color: #DBEAFE; -fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10;");
        Label execLbl = new Label(fin.path("executiveSummary").asText("Investigation complete."));
        execLbl.setMnemonicParsing(false);
        execLbl.setWrapText(true);
        execLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: #09090B; -fx-line-spacing: 3px;");
        execCard.getChildren().addAll(UITheme.wsSectionLabel("Executive summary"), execLbl);

        // 1. Log Analysis Card (with failingMethod, extractedStackTraces, logMetadata)
        VBox logCard = new VBox(6);
        logCard.setPadding(new Insets(12, 14, 12, 14));
        logCard.setStyle(UITheme.softCardStyle());
        Label logHead = new Label("1. Log analysis · " + logA.path("failingMethod").asText("Trace"));
        logHead.setMnemonicParsing(false);
        logHead.setWrapText(true);
        logHead.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #09090B;");
        Label logAnom = new Label(logA.path("keyAnomaly").asText(""));
        logAnom.setMnemonicParsing(false);
        logAnom.setWrapText(true);
        logAnom.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #3F3F46;");
        logCard.getChildren().addAll(logHead, logAnom);

        JsonNode frames = logA.path("extractedStackTraces");
        if (frames.isArray() && !frames.isEmpty()) {
            StringBuilder fb = new StringBuilder();
            for (JsonNode fr : frames) fb.append("• ").append(fr.asText()).append('\n');
            Label frLbl = new Label(fb.toString().trim());
            frLbl.setMnemonicParsing(false);
            frLbl.setWrapText(true);
            frLbl.setStyle(UITheme.monoStyle(11, "#18181B"));
            logCard.getChildren().add(frLbl);
        }
        JsonNode metaMap = logA.path("logMetadata");
        if (metaMap.isObject() && !metaMap.isEmpty()) {
            FlowPane metaFlow = new FlowPane(6, 5);
            Iterator<Map.Entry<String, JsonNode>> fields = metaMap.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> en = fields.next();
                metaFlow.getChildren().add(UITheme.neutralTag(en.getKey() + ": " + en.getValue().asText()));
            }
            logCard.getChildren().add(metaFlow);
        }

        // 2-3. Classification & Blast Radius Card
        VBox clsCard = new VBox(6);
        clsCard.setPadding(new Insets(12, 14, 12, 14));
        clsCard.setStyle(UITheme.softCardStyle());
        Label clsHead = new Label("2–3. Classification & blast radius");
        clsHead.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #09090B;");
        String subType = cls.path("subType").asText("");
        String conf = Math.round(cls.path("confidenceScore").asDouble(0) * 100) + "%";
        String blast = sev.path("blastRadius").asText("");
        String impact = sev.path("userImpact").asText("");
        String just = sev.path("justification").asText("");
        Label clsLine = new Label(cls.path("primaryCategory").asText("") + (subType.isBlank() ? "" : " (" + subType + ")") + " · Confidence: " + conf);
        clsLine.setMnemonicParsing(false);
        clsLine.setWrapText(true);
        clsLine.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 500; -fx-text-fill: #1D4ED8;");
        clsCard.getChildren().addAll(clsHead, clsLine);
        if (!blast.isBlank()) clsCard.getChildren().add(wrappedBody("Blast radius: " + blast));
        if (!impact.isBlank()) clsCard.getChildren().add(wrappedBody("User impact: " + impact));
        if (!just.isBlank()) clsCard.getChildren().add(wrappedBody("Severity justification: " + just));

        // 4. Root Cause Card (with contributingFactors)
        VBox rcCard = new VBox(6);
        rcCard.setPadding(new Insets(12, 14, 12, 14));
        rcCard.setStyle(UITheme.softCardStyle());
        Label rcHead = new Label("4. Root cause · " + rc.path("affectedComponent").asText("Core"));
        rcHead.setMnemonicParsing(false);
        rcHead.setWrapText(true);
        rcHead.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #09090B;");
        rcCard.getChildren().add(rcHead);
        String hyp = rc.path("primaryHypothesis").asText("");
        String underlying = rc.path("underlyingCause").asText("");
        if (!hyp.isBlank()) rcCard.getChildren().add(wrappedBody("Hypothesis: " + hyp));
        if (!underlying.isBlank()) rcCard.getChildren().add(wrappedBody(underlying));
        for (JsonNode factor : rc.path("contributingFactors")) {
            rcCard.getChildren().add(wrappedBody("•  " + factor.asText()));
        }

        // 5. Resolution Plan Card (with preventativeAction & Copy Patch button)
        VBox resCard = new VBox(8);
        resCard.setPadding(new Insets(12, 14, 12, 14));
        resCard.setStyle(UITheme.softCardStyle());
        resCard.getChildren().add(UITheme.wsSectionLabel("5. Resolution plan"));
        Label mitLbl = new Label("Immediate mitigation: " + res.path("immediateMitigation").asText(""));
        mitLbl.setMnemonicParsing(false);
        mitLbl.setWrapText(true);
        mitLbl.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 500; -fx-text-fill: #166534;");
        resCard.getChildren().add(mitLbl);

        for (JsonNode step : res.path("remediationSteps")) {
            resCard.getChildren().add(wrappedBody("•  " + step.asText()));
        }

        String prevAct = res.path("preventativeAction").asText("");
        if (!prevAct.isBlank()) {
            Label prevLbl = new Label("Preventative action: " + prevAct);
            prevLbl.setMnemonicParsing(false);
            prevLbl.setWrapText(true);
            prevLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #1D4ED8; -fx-font-weight: 500;");
            resCard.getChildren().add(prevLbl);
        }

        String codePatch = res.path("suggestedCodePatch").asText("");
        if (!codePatch.isBlank()) {
            HBox patchHead = new HBox(8, UITheme.wsSectionLabel("Suggested code patch"), new Region());
            patchHead.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(patchHead.getChildren().get(1), Priority.ALWAYS);
            Button btnCopyPatch = miniIconBtn("Copy patch", UITheme.copyIcon(12, Color.web("#52525B")));
            btnCopyPatch.setOnAction(e -> copyToClipboard(codePatch, "Copied suggested patch to clipboard"));
            patchHead.getChildren().add(btnCopyPatch);

            TextArea patchArea = new TextArea(codePatch);
            patchArea.setEditable(false);
            patchArea.setPrefRowCount(4);
            patchArea.setStyle(UITheme.monoStyle(11.5, "#18181B"));
            resCard.getChildren().addAll(patchHead, patchArea);
        }

        // 6. Full Markdown Report Card (with Timeline, Next Steps, Copy & Export .md)
        VBox fullRepCard = new VBox(8);
        fullRepCard.setPadding(new Insets(12, 14, 12, 14));
        fullRepCard.setStyle(UITheme.softCardStyle());

        String mdReport = fin.path("formattedMarkdownReport").asText("");
        HBox repHead = new HBox(6, UITheme.wsSectionLabel("6. Full markdown report"), new Region());
        repHead.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(repHead.getChildren().get(1), Priority.ALWAYS);
        Button btnCopyMd = miniIconBtn("Copy .md", UITheme.copyIcon(12, Color.web("#52525B")));
        btnCopyMd.setOnAction(e -> copyToClipboard(mdReport, "Copied Markdown report"));
        Button btnExportMd = miniIconBtn("Export .md", UITheme.downloadIcon(12, Color.web("#09090B")));
        btnExportMd.setOnAction(e -> exportMarkdownReport(sel.id.get(), mdReport));
        repHead.getChildren().addAll(btnCopyMd, btnExportMd);

        fullRepCard.getChildren().add(repHead);
        String timeline = fin.path("incidentTimeline").asText("");
        String nextSteps = fin.path("nextSteps").asText("");
        if (!timeline.isBlank()) fullRepCard.getChildren().add(wrappedBody("Timeline: " + timeline));
        if (!nextSteps.isBlank()) fullRepCard.getChildren().add(wrappedBody("Next steps: " + nextSteps));

        TextArea reportArea = new TextArea(mdReport);
        reportArea.setEditable(false);
        reportArea.setWrapText(true);
        reportArea.setPrefRowCount(10);
        reportArea.setStyle(UITheme.monoStyle(11.5, "#18181B"));
        fullRepCard.getChildren().add(reportArea);

        // Quick Stage Filter Pills
        FlowPane stageBar = new FlowPane(5, 5);
        String[] stageLabels = {"All (6)", "1 Log", "2–3 Impact", "4 Root cause", "5 Fix", "6 Report"};
        VBox[] stageCards = {null, logCard, clsCard, rcCard, resCard, fullRepCard};
        List<Button> stageBtns = new ArrayList<>();
        String pActive = "-fx-background-color: #09090B; -fx-text-fill: #FFFFFF; -fx-font-size: 11px; -fx-font-weight: 500; -fx-padding: 3 9; -fx-background-radius: 6; -fx-cursor: hand;";
        String pIdle   = "-fx-background-color: #F4F4F5; -fx-text-fill: #52525B; -fx-font-size: 11px; -fx-font-weight: 500; -fx-padding: 3 9; -fx-background-radius: 6; -fx-cursor: hand;";
        for (int s = 0; s < stageLabels.length; s++) {
            final int sIdx = s;
            Button sb = new Button(stageLabels[s]);
            sb.setMnemonicParsing(false);
            sb.setStyle(s == 0 ? pActive : pIdle);
            sb.setOnAction(e -> {
                for (int k = 0; k < stageBtns.size(); k++) {
                    stageBtns.get(k).setStyle(k == sIdx ? pActive : pIdle);
                }
                for (int k = 1; k < stageCards.length; k++) {
                    boolean show = (sIdx == 0 || sIdx == k);
                    stageCards[k].setVisible(show);
                    stageCards[k].setManaged(show);
                }
            });
            stageBtns.add(sb);
            stageBar.getChildren().add(sb);
        }

        aiPane.getChildren().addAll(topRow, stageBar, execCard, logCard, clsCard, rcCard, resCard, fullRepCard);
        workbenchScroll.setVvalue(0);
    }

    private void exportMarkdownReport(String incidentId, String mdContent) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Investigation Markdown Report");
        fc.setInitialFileName("incident-" + incidentId + "-report.md");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Markdown files (*.md)", "*.md"));
        File out = fc.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (out != null) {
            try {
                Files.writeString(out.toPath(), mdContent != null ? mdContent : "", StandardCharsets.UTF_8);
                setStatus("Exported report to " + out.getName(), UITheme.POS_DOT);
                notifyToast("Saved " + out.getName());
            } catch (Exception ex) {
                setStatus("Export failed: " + ex.getMessage(), UITheme.COLOR_DANGER);
            }
        }
    }

    private void copyToClipboard(String text, String toastMsg) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text != null ? text : "");
        Clipboard.getSystemClipboard().setContent(content);
        notifyToast(toastMsg);
    }

    private void notifyToast(String msg) {
        if (onNotifyToast != null && msg != null && !msg.isBlank()) {
            onNotifyToast.accept(msg);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────
    private Label wrappedBody(String txt) {
        Label l = new Label(txt);
        l.setMnemonicParsing(false);
        l.setWrapText(true);
        l.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #3F3F46; -fx-line-spacing: 2px;");
        return l;
    }

    private HBox metaChip(String key, String val) {
        Label k = new Label(key + ":");
        k.setStyle("-fx-font-size: 11px; -fx-text-fill: #71717A; -fx-font-weight: 500;");
        Label v = new Label(val);
        v.setMnemonicParsing(false);
        v.setStyle("-fx-font-size: 11px; -fx-text-fill: #09090B; -fx-font-weight: 500;");
        HBox box = new HBox(4, k, v);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private Button miniIconBtn(String label, StackPane icon) {
        Button b = new Button(label, icon);
        b.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        b.setGraphicTextGap(5);
        b.setCursor(Cursor.HAND);
        String normal = "-fx-background-color: #FFFFFF; -fx-text-fill: #27272A; -fx-border-color: #E4E4E7; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 11px; -fx-font-weight: 500; -fx-padding: 3 8;";
        String hover  = "-fx-background-color: #F4F4F5; -fx-text-fill: #09090B; -fx-border-color: #D4D4D8; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 11px; -fx-font-weight: 500; -fx-padding: 3 8;";
        b.setStyle(normal);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e -> b.setStyle(normal));
        return b;
    }

    private String formatIso(String iso) {
        if (iso == null || iso.isBlank()) return "—";
        try {
            return LocalDateTime.parse(iso).format(DateTimeFormatter.ofPattern("dd MMM, HH:mm"));
        } catch (Exception e) {
            return iso.length() > 16 ? iso.substring(0, 16).replace('T', ' ') : iso;
        }
    }

    private String extractServerError(String body, String fallback) {
        try {
            JsonNode n = mapper.readTree(body);
            if (n.hasNonNull("message")) return n.path("message").asText(fallback);
            if (n.hasNonNull("error")) return n.path("error").asText(fallback);
            return fallback;
        } catch (Exception e) {
            return (body != null && !body.isBlank() && body.length() < 120) ? body : fallback;
        }
    }

    private VBox infoCard(String title, String body, String bg) {
        VBox box = new VBox(5);
        box.setPadding(new Insets(12, 14, 12, 14));
        box.setStyle(
            "-fx-background-color: " + bg + "; -fx-border-color: " + UITheme.WS_BORDER + ";" +
            "-fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;"
        );
        Label t = new Label(title);
        t.setMnemonicParsing(false);
        t.setWrapText(true);
        t.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: #09090B;");
        Label b = new Label(body);
        b.setMnemonicParsing(false);
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
