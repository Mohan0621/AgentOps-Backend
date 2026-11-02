package com.agentops.UI;

import com.agentops.UI.Views.AnalyticsView;
import com.agentops.UI.Views.IncidentsView;
import com.agentops.UI.Views.LoginView;
import com.agentops.UI.Views.ReportIncidentView;
import com.agentops.UI.Views.UITheme;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class IncidentIqApp extends Application {

    private Stage primaryStage;
    private Scene loginScene;
    private Scene mainScene;

    // View Components
    private IncidentsView incidentsView;
    private ReportIncidentView reportIncidentView;
    private AnalyticsView analyticsView;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("IncidentIQ - Autonomous Incident & Multi-Agent Investigation");

        initViews();

        this.loginScene = new Scene(new LoginView(this::onLoginSuccess));
        this.mainScene = createMainDashboardScene();

        primaryStage.setScene(loginScene);
        primaryStage.setWidth(1120);
        primaryStage.setHeight(760);
        primaryStage.show();
    }

    private void initViews() {
        this.incidentsView = new IncidentsView();
        this.reportIncidentView = new ReportIncidentView(() -> {
            incidentsView.refreshIncidents();
            analyticsView.refreshDashboard();
        });
        this.analyticsView = new AnalyticsView();
    }

    private void onLoginSuccess() {
        primaryStage.setScene(mainScene);
        incidentsView.refreshIncidents();
        analyticsView.refreshDashboard();
    }

    private Scene createMainDashboardScene() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + UITheme.BG_DARK + ";");

        // Top bar
        HBox topBar = new HBox(15);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(12, 20, 12, 20));
        topBar.setStyle("-fx-background-color: " + UITheme.BG_TOP + "; -fx-border-color: #2b2b40; -fx-border-width: 0 0 1 0;");

        Label brand = new Label("IncidentIQ Console");
        brand.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + UITheme.ACCENT_CYAN + ";");

        Label userGreeting = new Label("Session: Active (JWT Authenticated)");
        userGreeting.setStyle("-fx-text-fill: " + UITheme.TEXT_MUTED + ";");

        Button btnLogout = new Button("Sign Out");
        btnLogout.setStyle("-fx-background-color: " + UITheme.COLOR_DANGER + "; -fx-text-fill: white;");
        btnLogout.setOnAction(e -> {
            ApiClient.setAuth(null, null);
            primaryStage.setScene(loginScene);
        });

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        topBar.getChildren().addAll(brand, userGreeting, spacer, btnLogout);
        root.setTop(topBar);

        // Center TabPane
        TabPane tabPane = new TabPane();

        Tab tab1 = new Tab("Incident Queue & Multi-Agent Investigation", incidentsView);
        tab1.setClosable(false);

        Tab tab2 = new Tab("Report New Incident", reportIncidentView);
        tab2.setClosable(false);

        Tab tab3 = new Tab("Platform Analytics & MTTR", analyticsView);
        tab3.setClosable(false);

        tabPane.getTabs().addAll(tab1, tab2, tab3);
        root.setCenter(tabPane);

        return new Scene(root);
    }
}
