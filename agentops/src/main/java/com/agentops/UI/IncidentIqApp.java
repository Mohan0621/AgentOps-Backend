package com.agentops.UI;

import com.agentops.UI.Views.AnalyticsView;
import com.agentops.UI.Views.IncidentsView;
import com.agentops.UI.Views.LoginView;
import com.agentops.UI.Views.PublicWebsiteView;
import com.agentops.UI.Views.ReportIncidentView;
import com.agentops.UI.Views.UITheme;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

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

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        UITheme.loadFonts();
        primaryStage.setTitle("IncidentIQ");

        initViews();

        websiteScene = new Scene(publicWebsiteView, 1320, 840);
        loginScene   = new Scene(loginView, 1320, 840);
        mainScene    = new Scene(buildMainLayout(), 1320, 840);

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
        scene.setOnKeyPressed(ev -> {
            if (ev.isControlDown()) {
                if (ev.getCode() == KeyCode.DIGIT1) { showTab("incidents"); ev.consume(); }
                else if (ev.getCode() == KeyCode.DIGIT2) { showTab("report"); ev.consume(); }
                else if (ev.getCode() == KeyCode.DIGIT3) { showTab("analytics"); ev.consume(); }
                else if (ev.getCode() == KeyCode.R) {
                    if ("analytics".equals(activeTab)) analyticsView.refreshDashboard();
                    else if ("incidents".equals(activeTab)) incidentsView.refreshIncidents();
                    ev.consume();
                }
            }
        });
    }

    private void openAuth(boolean registerMode) {
        publicWebsiteView.setSceneActive(false);
        loginView.setRegisterMode(registerMode);
        primaryStage.setScene(loginScene);
    }

    private void openPublicWebsite() {
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
            ApiClient.setAuth(null, null);
            publicWebsiteView.refreshAuthButtons(false);
            openPublicWebsite();
        });

        utilityLinks.getChildren().addAll(btnWebsite, sepDot, btnLogout);
        footer.getChildren().addAll(apiRow, userEmailLbl, utilityLinks);

        rail.getChildren().addAll(brand, navList, railSpacer, footer);
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
