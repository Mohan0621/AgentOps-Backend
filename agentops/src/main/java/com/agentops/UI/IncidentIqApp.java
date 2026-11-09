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
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
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

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        UITheme.loadFonts();
        primaryStage.setTitle("IncidentIQ — AI-Powered Incident Management");

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

        websiteScene = new Scene(publicWebsiteView, 1320, 840);
        loginScene   = new Scene(loginView, 1320, 840);
        mainScene    = new Scene(buildMainLayout(), 1320, 840);

        loadCss(websiteScene);
        loadCss(loginScene);
        loadCss(mainScene);

        primaryStage.setScene(websiteScene);
        primaryStage.setMinWidth(1020);
        primaryStage.setMinHeight(680);
        primaryStage.show();
    }

    private void loadCss(Scene scene) {
        try {
            String css = getClass().getResource("/css/app.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            System.err.println("Could not load CSS: " + e.getMessage());
        }
    }

    private void openAuth(boolean registerMode) {
        loginView.setRegisterMode(registerMode);
        primaryStage.setScene(loginScene);
    }

    private void openPublicWebsite() {
        publicWebsiteView.refreshAuthButtons(ApiClient.isAuthenticated());
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
        String email = ApiClient.getCurrentUserEmail();
        if (userEmailLbl != null) {
            userEmailLbl.setText(email != null && !email.isBlank() ? email : "Signed in");
        }
        publicWebsiteView.refreshAuthButtons(true);
        primaryStage.setScene(mainScene);
        showTab("incidents");
        incidentsView.refreshIncidents();
        analyticsView.refreshDashboard();
    }

    // ── Inside Workspace Layout (Light Editorial Shell) ───────────
    private BorderPane buildMainLayout() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #FFFFFF;");
        root.setTop(buildTopNav());

        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: #FFFFFF;");
        contentArea.getChildren().addAll(incidentsView, reportView, analyticsView);
        reportView.setVisible(false);
        analyticsView.setVisible(false);

        root.setCenter(contentArea);
        return root;
    }

    // ── Workspace Top Bar (Matches serko.ai Light Navigation) ─────
    private VBox buildTopNav() {
        VBox wrapper = new VBox(0);

        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER);
        bar.setPrefHeight(66);
        bar.setPadding(new Insets(0, 28, 0, 28));
        bar.setStyle("-fx-background-color: #FFFFFF;");

        // Left: Brand + Return to Public Website pill
        HBox leftGroup = new HBox(12);
        leftGroup.setAlignment(Pos.CENTER_LEFT);

        HBox brand = new HBox(9);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.setCursor(Cursor.HAND);
        brand.setOnMouseClicked(e -> showTab("incidents"));
        StackPane brandMark = UITheme.brandLogoMark(22, false);
        Label brandName = new Label("IncidentIQ");
        brandName.setStyle("-fx-font-size: 16.5px; -fx-font-weight: bold; -fx-text-fill: #000000;");
        brand.getChildren().addAll(brandMark, brandName);

        Button btnWebsite = new Button("Website");
        btnWebsite.getStyleClass().add("serko-nav-link");
        btnWebsite.setStyle("-fx-background-color: #F6F6F6; -fx-text-fill: #4D4D4D; -fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 5 12; -fx-background-radius: 999;");
        btnWebsite.setOnMouseEntered(e -> btnWebsite.setStyle("-fx-background-color: #EAEAEA; -fx-text-fill: #000000; -fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 5 12; -fx-background-radius: 999;"));
        btnWebsite.setOnMouseExited(e -> btnWebsite.setStyle("-fx-background-color: #F6F6F6; -fx-text-fill: #4D4D4D; -fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 5 12; -fx-background-radius: 999;"));
        btnWebsite.setOnAction(e -> openPublicWebsite());

        leftGroup.getChildren().addAll(brand, btnWebsite);

        // Center: Segmented Navigation Pills
        HBox navTrack = new HBox(4);
        navTrack.setAlignment(Pos.CENTER);
        navTrack.setPadding(new Insets(4));
        navTrack.setStyle("-fx-background-color: #F6F6F6; -fx-background-radius: 999;");

        navIncidents = makeNavBtn("Incidents",       "incidents");
        navReport    = makeNavBtn("Report Incident", "report");
        navAnalytics = makeNavBtn("Analytics",       "analytics");
        navTrack.getChildren().addAll(navIncidents, navReport, navAnalytics);

        Region s1 = new Region(); HBox.setHgrow(s1, Priority.ALWAYS);
        Region s2 = new Region(); HBox.setHgrow(s2, Priority.ALWAYS);

        // Right: Live status badge + user pill + Sign Out
        HBox rightGroup = new HBox(10);
        rightGroup.setAlignment(Pos.CENTER_RIGHT);

        Label liveBadge = UITheme.positiveBadge("●  Live API");

        userEmailLbl = new Label("Engineer");
        userEmailLbl.setStyle(
            "-fx-background-color: #F6F6F6;" +
            "-fx-text-fill: #3F3F46;" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: 600;" +
            "-fx-padding: 5 12;" +
            "-fx-background-radius: 999;"
        );

        Button btnLogout = new Button("Sign Out");
        String logoutNormal = "-fx-background-color: rgba(255,255,255,0.85); -fx-text-fill: #000000; -fx-border-color: #E5E7EB; " +
            "-fx-border-radius: 999; -fx-background-radius: 999; -fx-font-size: 12.5px; -fx-font-weight: 600; -fx-padding: 6 16; -fx-cursor: hand;";
        String logoutHover = "-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-border-color: #FECACA; " +
            "-fx-border-radius: 999; -fx-background-radius: 999; -fx-font-size: 12.5px; -fx-font-weight: 600; -fx-padding: 6 16; -fx-cursor: hand;";
        btnLogout.setStyle(logoutNormal);
        btnLogout.setOnMouseEntered(e -> btnLogout.setStyle(logoutHover));
        btnLogout.setOnMouseExited(e -> btnLogout.setStyle(logoutNormal));
        btnLogout.setOnAction(e -> {
            ApiClient.setAuth(null, null);
            publicWebsiteView.refreshAuthButtons(false);
            openPublicWebsite();
        });

        rightGroup.getChildren().addAll(liveBadge, userEmailLbl, btnLogout);

        bar.getChildren().addAll(leftGroup, s1, navTrack, s2, rightGroup);

        HBox borderLine = new HBox();
        borderLine.setPrefHeight(1);
        borderLine.setStyle("-fx-background-color: #EBEBEB;");

        wrapper.getChildren().addAll(bar, borderLine);
        return wrapper;
    }

    private Button makeNavBtn(String text, String tabId) {
        Button btn = new Button(text);
        applyNavStyle(btn, false);
        btn.setOnAction(e -> showTab(tabId));
        return btn;
    }

    private void showTab(String tabId) {
        incidentsView.setVisible("incidents".equals(tabId));
        reportView.setVisible("report".equals(tabId));
        analyticsView.setVisible("analytics".equals(tabId));

        if ("analytics".equals(tabId)) {
            analyticsView.refreshDashboard();
        } else if ("incidents".equals(tabId)) {
            incidentsView.refreshIncidents();
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
                "-fx-text-fill: #000000;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 7 20;" +
                "-fx-background-radius: 999;" +
                "-fx-cursor: hand;"
            );
            DropShadow s = new DropShadow();
            s.setColor(Color.rgb(0, 0, 0, 0.08));
            s.setRadius(8);
            s.setOffsetY(2);
            btn.setEffect(s);
        } else {
            btn.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: #52525B;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: 600;" +
                "-fx-padding: 7 20;" +
                "-fx-background-radius: 999;" +
                "-fx-cursor: hand;"
            );
            btn.setEffect(null);
        }
    }
}
