package com.agentops.UI;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.agentops.UI.Models.IncidentRow;
import com.agentops.UI.Views.AnalyticsView;
import com.agentops.UI.Views.IncidentsView;
import com.agentops.UI.Views.LoginView;
import com.agentops.UI.Views.PublicWebsiteView;
import com.agentops.UI.Views.ReportIncidentView;
import com.agentops.UI.Views.UITheme;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class IncidentIqApp extends Application {

    private Stage primaryStage;
    private Scene websiteScene;
    private Scene loginScene;
    private Scene mainScene;

    private PublicWebsiteView  publicWebsiteView;
    private LoginView          loginView;
    private IncidentsView      incidentsView;
    private ReportIncidentView reportView;
    private AnalyticsView      analyticsView;

    private StackPane contentArea;
    private Button    navIncidents, navReport, navAnalytics;
    private Label     userEmailLbl;
    private String    activeTab = "incidents";

    // Workspace overlays (Toasts + Ctrl+K Command Palette)
    private VBox       toastStack;
    private StackPane  commandPaletteOverlay;
    private TextField  txtCmdSearch;
    private VBox       cmdResultsBox;
    private final List<CommandEntry> visibleCmdEntries = new ArrayList<>();
    private final List<HBox>         visibleCmdRows    = new ArrayList<>();
    private int selectedCmdIndex = 0;

    private record CommandEntry(String title, String subtitle, String rightTag, Node icon, Runnable action) {}

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        UITheme.loadFonts();
        primaryStage.setTitle("IncidentIQ");

        initViews();

        websiteScene = new Scene(publicWebsiteView, 1320, 840);
        loginScene   = new Scene(loginView, 1320, 840);
        mainScene    = new Scene(buildWorkspaceSceneRoot(), 1320, 840);

        loadCss(websiteScene);
        loadCss(loginScene);
        loadCss(mainScene);
        installWorkspaceShortcuts(mainScene);

        primaryStage.setScene(websiteScene);
        primaryStage.setMinWidth(1040);
        primaryStage.setMinHeight(680);
        primaryStage.show();
    }

    public void initViews() {
        incidentsView = new IncidentsView();
        reportView    = new ReportIncidentView(
            () -> {
                incidentsView.refreshIncidents();
                analyticsView.refreshDashboard();
            },
            () -> showTab("incidents")
        );
        analyticsView = new AnalyticsView();

        publicWebsiteView = new PublicWebsiteView(this::openAuth, this::openWorkspace);
        loginView         = new LoginView(this::onLoginSuccess, this::openPublicWebsite);

        // Cross-view notifications & interactive telemetry deep-links
        reportView.setOnNotifyToast(this::showToast);
        incidentsView.setOnNotifyToast(this::showToast);
        incidentsView.setOnDataChanged(() -> analyticsView.refreshDashboard());
        analyticsView.setOnNotifyToast(this::showToast);
        analyticsView.setOnNavigateFilter((sev, stat) -> {
            incidentsView.filterBy(sev, stat);
            showTab("incidents", false);
            String label = sev != null ? sev + " severity" : stat + " status";
            showToast("Filtered queue to " + UITheme.capitalizeWord(label));
        });
    }

    public IncidentsView getIncidentsView() { return incidentsView; }
    public ReportIncidentView getReportView() { return reportView; }
    public AnalyticsView getAnalyticsView() { return analyticsView; }

    public void setWorkspaceUserEmail(String email) {
        if (userEmailLbl != null) {
            userEmailLbl.setText(email != null && !email.isBlank() ? email : "Signed in");
            userEmailLbl.setTooltip(new Tooltip(userEmailLbl.getText()));
        }
    }

    private void loadCss(Scene scene) {
        try {
            String css = getClass().getResource("/css/app.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            System.err.println("Could not load CSS: " + e.getMessage());
        }
    }

    private void installWorkspaceShortcuts(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
            if (ev.getCode() == KeyCode.ESCAPE && commandPaletteOverlay != null && commandPaletteOverlay.isVisible()) {
                closeCommandPalette();
                ev.consume();
                return;
            }
            if (ev.isControlDown()) {
                if (ev.getCode() == KeyCode.K) {
                    toggleCommandPalette();
                    ev.consume();
                } else if (ev.getCode() == KeyCode.DIGIT1) {
                    closeCommandPalette();
                    showTab("incidents");
                    ev.consume();
                } else if (ev.getCode() == KeyCode.DIGIT2) {
                    closeCommandPalette();
                    showTab("report");
                    ev.consume();
                } else if (ev.getCode() == KeyCode.DIGIT3) {
                    closeCommandPalette();
                    showTab("analytics");
                    ev.consume();
                } else if (ev.getCode() == KeyCode.R) {
                    if ("analytics".equals(activeTab)) analyticsView.refreshDashboard();
                    else if ("incidents".equals(activeTab)) incidentsView.refreshIncidents();
                    ev.consume();
                }
            }
        });
    }

    private void openAuth(boolean registerMode) {
        publicWebsiteView.setSceneActive(false);
        analyticsView.setSceneActive(false);
        loginView.setRegisterMode(registerMode);
        primaryStage.setScene(loginScene);
    }

    private void openPublicWebsite() {
        analyticsView.setSceneActive(false);
        publicWebsiteView.refreshAuthButtons(ApiClient.isAuthenticated());
        publicWebsiteView.setSceneActive(true);
        primaryStage.setScene(websiteScene);
    }

    private void openWorkspace() {
        if (ApiClient.isAuthenticated()) {
            onLoginSuccess();
        } else {
            openAuth(false);
        }
    }

    private void onLoginSuccess() {
        publicWebsiteView.setSceneActive(false);
        setWorkspaceUserEmail(ApiClient.getCurrentUserEmail());
        publicWebsiteView.refreshAuthButtons(true);
        primaryStage.setScene(mainScene);
        showTab("incidents");
        incidentsView.refreshIncidents();
        analyticsView.refreshDashboard();
    }

    public StackPane buildWorkspaceSceneRoot() {
        BorderPane layout = buildMainLayout();

        toastStack = new VBox(8);
        toastStack.setAlignment(Pos.BOTTOM_RIGHT);
        toastStack.setPadding(new Insets(0, 22, 22, 0));
        toastStack.setPickOnBounds(false);
        toastStack.setMouseTransparent(true);
        toastStack.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(toastStack, Pos.BOTTOM_RIGHT);

        commandPaletteOverlay = buildCommandPaletteOverlay();
        commandPaletteOverlay.setVisible(false);
        commandPaletteOverlay.setManaged(false);

        StackPane workspaceRoot = new StackPane(layout, toastStack, commandPaletteOverlay);
        workspaceRoot.setStyle("-fx-background-color: " + UITheme.BG_CANVAS + ";");
        return workspaceRoot;
    }

    public void showToast(String message) {
        if (toastStack == null || message == null || message.isBlank()) return;
        Runnable add = () -> {
            while (toastStack.getChildren().size() >= 3) {
                toastStack.getChildren().remove(0);
            }
            String low = message.toLowerCase(Locale.ROOT);
            boolean isWarn = low.contains("fail") || low.contains("error") || low.contains("only")
                || low.contains("cannot") || low.contains("denied");
            Circle dot = new Circle(3.5, Color.web(isWarn ? UITheme.COLOR_DANGER : UITheme.POS_DOT));
            Label txt = new Label(message);
            txt.setMnemonicParsing(false);
            txt.setWrapText(true);
            txt.setMaxWidth(290);
            txt.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 500; -fx-text-fill: #09090B;");

            HBox pill = new HBox(9, dot, txt);
            pill.setAlignment(Pos.CENTER_LEFT);
            pill.setPadding(new Insets(10, 14, 10, 14));
            pill.setStyle(
                "-fx-background-color: #FFFFFF; -fx-border-color: #E4E4E7; -fx-border-width: 1;" +
                "-fx-border-radius: 10; -fx-background-radius: 10;"
            );
            DropShadow ds = new DropShadow();
            ds.setColor(Color.rgb(0, 0, 0, 0.08));
            ds.setRadius(14);
            ds.setOffsetY(4);
            pill.setEffect(ds);

            pill.setOpacity(0);
            pill.setTranslateY(10);
            toastStack.getChildren().add(pill);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(160), pill);
            fadeIn.setToValue(1.0);
            TranslateTransition slideIn = new TranslateTransition(Duration.millis(160), pill);
            slideIn.setToY(0);
            ParallelTransition enter = new ParallelTransition(fadeIn, slideIn);

            PauseTransition hold = new PauseTransition(Duration.seconds(2.6));

            FadeTransition fadeOut = new FadeTransition(Duration.millis(220), pill);
            fadeOut.setToValue(0.0);

            SequentialTransition seq = new SequentialTransition(enter, hold, fadeOut);
            seq.setOnFinished(e -> toastStack.getChildren().remove(pill));
            seq.play();
        };
        if (Platform.isFxApplicationThread()) add.run();
        else Platform.runLater(add);
    }

    private StackPane buildCommandPaletteOverlay() {
        StackPane backdrop = new StackPane();
        backdrop.setAlignment(Pos.TOP_CENTER);
        backdrop.setStyle("-fx-background-color: rgba(9, 9, 11, 0.18);");
        backdrop.setOnMouseClicked(e -> {
            if (e.getTarget() == backdrop) closeCommandPalette();
        });

        VBox dialog = new VBox(0);
        dialog.setMaxWidth(560);
        dialog.setMaxHeight(480);
        dialog.setStyle(
            "-fx-background-color: #FFFFFF; -fx-border-color: #E4E4E7; -fx-border-width: 1;" +
            "-fx-border-radius: 12; -fx-background-radius: 12;"
        );
        DropShadow ds = new DropShadow();
        ds.setColor(Color.rgb(0, 0, 0, 0.14));
        ds.setRadius(28);
        ds.setOffsetY(10);
        dialog.setEffect(ds);
        StackPane.setMargin(dialog, new Insets(58, 24, 24, 24));

        HBox searchRow = new HBox(10);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchRow.setPadding(new Insets(11, 16, 11, 16));
        searchRow.setStyle("-fx-border-color: transparent transparent " + UITheme.WS_BORDER + " transparent; -fx-border-width: 0 0 1 0;");

        txtCmdSearch = new TextField();
        txtCmdSearch.setPromptText("Jump to incident #ID, title, or workspace action...");
        txtCmdSearch.setStyle(
            "-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 2 0;" +
            "-fx-font-size: 13.5px; -fx-text-fill: #09090B;"
        );
        HBox.setHgrow(txtCmdSearch, Priority.ALWAYS);

        Label escHint = new Label("ESC");
        escHint.setStyle(
            "-fx-background-color: #F4F4F5; -fx-text-fill: #71717A; -fx-border-color: #E4E4E7;" +
            "-fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;" +
            "-fx-font-family: '" + UITheme.MONO_FAMILY + "', 'Consolas', monospace; -fx-font-size: 10.5px; -fx-padding: 2 6;"
        );
        searchRow.getChildren().addAll(UITheme.searchIcon(15, Color.web("#71717A")), txtCmdSearch, escHint);

        cmdResultsBox = new VBox(2);
        cmdResultsBox.setPadding(new Insets(6, 8, 10, 8));

        ScrollPane resScroll = new ScrollPane(cmdResultsBox);
        resScroll.setFitToWidth(true);
        resScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        resScroll.setMaxHeight(425);
        resScroll.getStyleClass().add("scroll-pane");

        txtCmdSearch.textProperty().addListener((obs, o, n) -> rebuildCommandResults(n));
        txtCmdSearch.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.DOWN) {
                if (!visibleCmdRows.isEmpty()) {
                    selectedCmdIndex = (selectedCmdIndex + 1) % visibleCmdRows.size();
                    highlightCmdRow();
                }
                ev.consume();
            } else if (ev.getCode() == KeyCode.UP) {
                if (!visibleCmdRows.isEmpty()) {
                    selectedCmdIndex = (selectedCmdIndex - 1 + visibleCmdRows.size()) % visibleCmdRows.size();
                    highlightCmdRow();
                }
                ev.consume();
            } else if (ev.getCode() == KeyCode.ENTER) {
                executeSelectedCommand();
                ev.consume();
            }
        });

        dialog.getChildren().addAll(searchRow, resScroll);
        backdrop.getChildren().add(dialog);
        return backdrop;
    }

    public void openCommandPalette() {
        if (commandPaletteOverlay == null) return;
        txtCmdSearch.clear();
        rebuildCommandResults("");
        commandPaletteOverlay.setVisible(true);
        commandPaletteOverlay.setManaged(true);
        Platform.runLater(() -> txtCmdSearch.requestFocus());
    }

    private void closeCommandPalette() {
        if (commandPaletteOverlay == null) return;
        commandPaletteOverlay.setVisible(false);
        commandPaletteOverlay.setManaged(false);
    }

    private void toggleCommandPalette() {
        if (commandPaletteOverlay == null) return;
        if (commandPaletteOverlay.isVisible()) closeCommandPalette();
        else openCommandPalette();
    }

    private void executeSelectedCommand() {
        if (visibleCmdEntries.isEmpty()) return;
        int idx = Math.max(0, Math.min(visibleCmdEntries.size() - 1, selectedCmdIndex));
        CommandEntry entry = visibleCmdEntries.get(idx);
        closeCommandPalette();
        entry.action().run();
    }

    private void rebuildCommandResults(String rawQuery) {
        String q = rawQuery == null ? "" : rawQuery.trim().toLowerCase(Locale.ROOT);
        cmdResultsBox.getChildren().clear();
        visibleCmdEntries.clear();
        visibleCmdRows.clear();
        selectedCmdIndex = 0;

        List<CommandEntry> commands = List.of(
            new CommandEntry("Go to Incidents queue", "View and filter active alerts", "Ctrl+1",
                UITheme.inboxIcon(14, Color.web("#52525B")), () -> showTab("incidents")),
            new CommandEntry("Report new incident", "Log a stack trace or service outage", "Ctrl+2",
                UITheme.plusSquareIcon(14, Color.web("#52525B")), () -> showTab("report")),
            new CommandEntry("Open Operational analytics", "MTTR, AI coverage & daily chart", "Ctrl+3",
                UITheme.chartBarIcon(14, Color.web("#52525B")), () -> showTab("analytics")),
            new CommandEntry("Run Quick diagnosis on selected", "Single-pass Groq / Gemini triage", "AI",
                UITheme.boltIcon(14, Color.web(UITheme.GRAD_BLUE)), () -> { showTab("incidents", false); incidentsView.runQuickDiagnosisOnSelected(); }),
            new CommandEntry("Run 6-agent investigation on selected", "Full pipeline + Markdown report", "6 Agents",
                UITheme.layersIcon(14, Color.web(UITheme.GRAD_BLUE)), () -> { showTab("incidents", false); incidentsView.runMultiAgentOnSelected(); }),
            new CommandEntry("Refresh current workspace view", "Sync latest records from backend", "Ctrl+R",
                UITheme.refreshIcon(14, Color.web("#52525B")), () -> {
                    if ("analytics".equals(activeTab)) analyticsView.refreshDashboard();
                    else incidentsView.refreshIncidents();
                    showToast("Refreshed workspace data");
                }),
            new CommandEntry("Open Public showcase website", "Return to public IncidentIQ landing", "Public",
                UITheme.brandLogoMark(14, false), this::openPublicWebsite)
        );

        List<CommandEntry> actionMatches = new ArrayList<>();
        for (CommandEntry ce : commands) {
            if (q.isEmpty() || ce.title().toLowerCase(Locale.ROOT).contains(q) || ce.subtitle().toLowerCase(Locale.ROOT).contains(q)) {
                actionMatches.add(ce);
                if (q.isEmpty() && actionMatches.size() >= 4) break;
            }
        }

        List<CommandEntry> incidentMatches = new ArrayList<>();
        for (IncidentRow row : incidentsView.getLoadedIncidents()) {
            String id = row.id.get();
            String title = row.title.get();
            String type = row.type.get();
            String sev = row.severity.get();
            if (q.isEmpty() && incidentMatches.size() >= 3) break;
            boolean matches = q.isEmpty()
                || ("#" + id).toLowerCase(Locale.ROOT).contains(q)
                || title.toLowerCase(Locale.ROOT).contains(q)
                || type.toLowerCase(Locale.ROOT).contains(q)
                || sev.toLowerCase(Locale.ROOT).contains(q);
            if (matches) {
                incidentMatches.add(new CommandEntry(
                    "#" + id + "  " + title,
                    UITheme.capitalizeWord(type) + " · " + UITheme.capitalizeWord(row.status.get()),
                    sev,
                    UITheme.terminalIcon(14, Color.web("#18181B")),
                    () -> {
                        showTab("incidents", false);
                        incidentsView.selectIncidentById(id);
                    }
                ));
                if (!q.isEmpty() && incidentMatches.size() >= 8) break;
            }
        }

        if (actionMatches.isEmpty() && incidentMatches.isEmpty()) {
            Label none = new Label("No matching incidents or actions for \"" + rawQuery + "\"");
            none.setMnemonicParsing(false);
            none.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #71717A; -fx-padding: 18 12;");
            cmdResultsBox.getChildren().add(none);
            return;
        }

        if (!q.isEmpty() && !incidentMatches.isEmpty()) {
            addPaletteGroup("Matching incidents", incidentMatches);
            if (!actionMatches.isEmpty()) addPaletteGroup("Workspace actions", actionMatches);
        } else {
            if (!actionMatches.isEmpty()) addPaletteGroup("Workspace actions", actionMatches);
            if (!incidentMatches.isEmpty()) addPaletteGroup("Loaded queue", incidentMatches);
        }
        highlightCmdRow();
    }

    private void addPaletteGroup(String heading, List<CommandEntry> entries) {
        Label h = new Label(heading);
        h.setStyle("-fx-font-size: 11px; -fx-font-weight: 500; -fx-text-fill: #71717A; -fx-padding: 4 10 2 10;");
        cmdResultsBox.getChildren().add(h);

        for (CommandEntry ce : entries) {
            final int idx = visibleCmdEntries.size();
            visibleCmdEntries.add(ce);

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(5, 10, 5, 10));
            row.setCursor(Cursor.HAND);

            Label t = new Label(ce.title());
            t.setMnemonicParsing(false);
            t.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 500; -fx-text-fill: #09090B;");
            Label s = new Label(ce.subtitle());
            s.setMnemonicParsing(false);
            s.setStyle("-fx-font-size: 11px; -fx-text-fill: #71717A;");
            VBox texts = new VBox(1, t, s);
            HBox.setHgrow(texts, Priority.ALWAYS);

            Label rBadge = List.of("CRITICAL", "HIGH", "MEDIUM", "LOW").contains(ce.rightTag().toUpperCase(Locale.ROOT))
                ? UITheme.severityBadge(ce.rightTag())
                : UITheme.neutralTag(ce.rightTag());

            row.getChildren().addAll(ce.icon(), texts, rBadge);
            row.setOnMouseEntered(e -> {
                selectedCmdIndex = idx;
                highlightCmdRow();
            });
            row.setOnMouseClicked(e -> {
                selectedCmdIndex = idx;
                executeSelectedCommand();
            });

            visibleCmdRows.add(row);
            cmdResultsBox.getChildren().add(row);
        }
    }

    private void highlightCmdRow() {
        for (int i = 0; i < visibleCmdRows.size(); i++) {
            boolean active = (i == selectedCmdIndex);
            visibleCmdRows.get(i).setStyle(active
                ? "-fx-background-color: #EFF6FF; -fx-border-color: #DBEAFE; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;"
                : "-fx-background-color: transparent; -fx-border-color: transparent; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;"
            );
        }
    }

    // ── Inside Workspace Layout (Operate 220px Left Rail + #FAFAFA Canvas) ──
    public BorderPane buildMainLayout() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + UITheme.BG_CANVAS + ";");
        root.setLeft(buildSidebarRail());

        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: " + UITheme.BG_CANVAS + ";");
        contentArea.getChildren().addAll(incidentsView, reportView, analyticsView);
        reportView.setVisible(false);
        analyticsView.setVisible(false);

        root.setCenter(contentArea);
        return root;
    }

    private VBox buildSidebarRail() {
        VBox rail = new VBox(0);
        rail.setPrefWidth(220);
        rail.setMinWidth(220);
        rail.setMaxWidth(220);
        rail.setPadding(new Insets(18, 12, 16, 12));
        rail.setStyle(
            "-fx-background-color: " + UITheme.BG_CANVAS + ";" +
            "-fx-border-color: transparent " + UITheme.WS_BORDER + " transparent transparent;" +
            "-fx-border-width: 0 1 0 0;"
        );

        // Top Brand Lockup
        HBox brand = new HBox(9);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.setPadding(new Insets(4, 8, 18, 8));
        brand.setCursor(Cursor.HAND);
        brand.setOnMouseClicked(e -> showTab("incidents"));
        StackPane brandMark = UITheme.brandLogoMark(20, false);
        Label brandName = new Label("IncidentIQ");
        brandName.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #09090B;");
        Region bsp = new Region(); HBox.setHgrow(bsp, Priority.ALWAYS);
        Label opsTag = UITheme.neutralTag("Ops");
        brand.getChildren().addAll(brandMark, brandName, bsp, opsTag);

        // Navigation Items
        VBox navList = new VBox(4);
        navIncidents = makeSidebarBtn("Incidents",       UITheme.inboxIcon(16, Color.web("#18181B")),      "incidents", "Ctrl+1");
        navReport    = makeSidebarBtn("Report incident", UITheme.plusSquareIcon(16, Color.web("#52525B")), "report",    "Ctrl+2");
        navAnalytics = makeSidebarBtn("Analytics",       UITheme.chartBarIcon(16, Color.web("#52525B")),   "analytics", "Ctrl+3");
        navList.getChildren().addAll(navIncidents, navReport, navAnalytics);

        // Quick jump Command Palette button (Ctrl+K)
        HBox cmdGraphic = new HBox(8);
        cmdGraphic.setAlignment(Pos.CENTER_LEFT);
        cmdGraphic.setMaxWidth(Double.MAX_VALUE);
        Label cmdLbl = new Label("Quick jump");
        cmdLbl.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #71717A; -fx-font-weight: 400;");
        Region cmdSp = new Region(); HBox.setHgrow(cmdSp, Priority.ALWAYS);
        Label kbdBadge = new Label("Ctrl+K");
        kbdBadge.setStyle(
            "-fx-background-color: #F4F4F5; -fx-text-fill: #71717A; -fx-border-color: #E4E4E7;" +
            "-fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;" +
            "-fx-font-family: '" + UITheme.MONO_FAMILY + "', 'Consolas', monospace; -fx-font-size: 10px; -fx-padding: 1 5;"
        );
        cmdGraphic.getChildren().addAll(UITheme.searchIcon(13, Color.web("#71717A")), cmdLbl, cmdSp, kbdBadge);

        Button btnCmdPalette = new Button();
        btnCmdPalette.setGraphic(cmdGraphic);
        btnCmdPalette.setMaxWidth(Double.MAX_VALUE);
        btnCmdPalette.setTooltip(new Tooltip("Open command palette (Ctrl+K)"));
        String cmdNormal = "-fx-background-color: #FFFFFF; -fx-border-color: #E4E4E7; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 7 10; -fx-cursor: hand;";
        String cmdHover  = "-fx-background-color: #F4F4F5; -fx-border-color: #D4D4D8; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 7 10; -fx-cursor: hand;";
        btnCmdPalette.setStyle(cmdNormal);
        btnCmdPalette.setOnMouseEntered(e -> btnCmdPalette.setStyle(cmdHover));
        btnCmdPalette.setOnMouseExited(e -> btnCmdPalette.setStyle(cmdNormal));
        btnCmdPalette.setOnAction(e -> openCommandPalette());
        VBox.setMargin(btnCmdPalette, new Insets(14, 0, 0, 0));

        Region railSpacer = new Region();
        VBox.setVgrow(railSpacer, Priority.ALWAYS);

        // Bottom Utility Footer
        VBox footer = new VBox(10);
        footer.setPadding(new Insets(14, 6, 2, 6));
        footer.setStyle(
            "-fx-border-color: " + UITheme.WS_BORDER + " transparent transparent transparent;" +
            "-fx-border-width: 1 0 0 0;"
        );

        HBox apiRow = new HBox(8);
        apiRow.setAlignment(Pos.CENTER_LEFT);
        Circle apiDot = new Circle(3.5, Color.web(UITheme.POS_DOT));
        Label apiTxt = new Label("API connected");
        apiTxt.setStyle("-fx-font-size: 12px; -fx-text-fill: #52525B; -fx-font-weight: 500;");
        apiRow.getChildren().addAll(apiDot, apiTxt);

        userEmailLbl = new Label("engineer@company.com");
        userEmailLbl.setMaxWidth(180);
        userEmailLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #18181B; -fx-font-weight: 500;");

        HBox utilityLinks = new HBox(4);
        utilityLinks.setAlignment(Pos.CENTER_LEFT);

        Button btnWebsite = new Button("Website");
        btnWebsite.getStyleClass().add("ws-btn-ghost");
        btnWebsite.setOnAction(e -> openPublicWebsite());

        Label sepDot = new Label("·");
        sepDot.setStyle("-fx-text-fill: #D4D4D8; -fx-font-size: 12px;");

        Button btnLogout = new Button("Sign out");
        String outNormal = "-fx-background-color: transparent; -fx-text-fill: #52525B; -fx-font-size: 12.5px; -fx-font-weight: 500; -fx-padding: 6 10; -fx-background-radius: 8; -fx-cursor: hand;";
        String outHover  = "-fx-background-color: #FEF2F2; -fx-text-fill: #B91C1C; -fx-font-size: 12.5px; -fx-font-weight: 500; -fx-padding: 6 10; -fx-background-radius: 8; -fx-cursor: hand;";
        btnLogout.setStyle(outNormal);
        btnLogout.setOnMouseEntered(e -> btnLogout.setStyle(outHover));
        btnLogout.setOnMouseExited(e -> btnLogout.setStyle(outNormal));
        btnLogout.setOnAction(e -> {
            analyticsView.setSceneActive(false);
            ApiClient.setAuth(null, null);
            publicWebsiteView.refreshAuthButtons(false);
            openPublicWebsite();
        });

        utilityLinks.getChildren().addAll(btnWebsite, sepDot, btnLogout);
        footer.getChildren().addAll(apiRow, userEmailLbl, utilityLinks);

        rail.getChildren().addAll(brand, navList, btnCmdPalette, railSpacer, footer);
        return rail;
    }

    private Button makeSidebarBtn(String text, Node icon, String tabId, String shortcutHint) {
        Button btn = new Button(text, icon);
        btn.setContentDisplay(ContentDisplay.LEFT);
        btn.setGraphicTextGap(10);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setTooltip(new Tooltip(text + " (" + shortcutHint + ")"));
        applyNavStyle(btn, false);
        btn.setOnAction(e -> showTab(tabId));
        return btn;
    }

    public void showTab(String tabId) {
        showTab(tabId, true);
    }

    public void showTab(String tabId, boolean triggerRefresh) {
        this.activeTab = tabId;
        incidentsView.setVisible("incidents".equals(tabId));
        reportView.setVisible("report".equals(tabId));
        analyticsView.setVisible("analytics".equals(tabId));
        analyticsView.setSceneActive("analytics".equals(tabId));

        if (triggerRefresh) {
            if ("analytics".equals(tabId)) {
                analyticsView.refreshDashboard();
            } else if ("incidents".equals(tabId)) {
                incidentsView.refreshIncidents();
            }
        }

        for (Button b : new Button[]{navIncidents, navReport, navAnalytics}) {
            applyNavStyle(b, false);
        }
        Button active = switch (tabId) {
            case "report"    -> navReport;
            case "analytics" -> navAnalytics;
            default          -> navIncidents;
        };
        applyNavStyle(active, true);
    }

    private void applyNavStyle(Button btn, boolean active) {
        if (active) {
            btn.setStyle(
                "-fx-background-color: #FFFFFF;" +
                "-fx-text-fill: #09090B;" +
                "-fx-border-color: #E4E4E7;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: 500;" +
                "-fx-padding: 9 12;" +
                "-fx-cursor: hand;"
            );
        } else {
            btn.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: #52525B;" +
                "-fx-border-color: transparent;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: 400;" +
                "-fx-padding: 9 12;" +
                "-fx-cursor: hand;"
            );
        }
    }
}
