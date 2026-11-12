package com.agentops.UI.Views;

import java.net.http.HttpResponse;

import com.agentops.UI.ApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * Calm Light Editorial Authentication Screen.
 * Supports Sign In and Register backed by /api/login and /api/register.
 */
public class LoginView extends StackPane {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Runnable onLoginSuccess;
    private final Runnable onBackToHome;

    private TextField txtFullName;
    private TextField txtEmail;
    private PasswordField txtPass;
    private TextField txtPassVisible;
    private boolean passVisible = false;
    private HBox healthPill;
    private Circle healthDot;
    private Label healthLbl;
    private HBox statusBanner;
    private Label statusLbl;
    private Label cardHeading;
    private Label cardSubtitle;
    private VBox fullNameBox;
    private boolean registerMode = false;
    private Button tabLogin;
    private Button tabReg;
    private Button btnPrimary;
    private Button btnToggle;

    public LoginView(Runnable onLoginSuccess) {
        this(onLoginSuccess, null);
    }

    public LoginView(Runnable onLoginSuccess, Runnable onBackToHome) {
        this.onLoginSuccess = onLoginSuccess;
        this.onBackToHome = onBackToHome;
        buildBackground();
        buildTopBackBar();
        buildCard();
        refreshHealth();
    }

    public void setRegisterMode(boolean register) {
        if (tabLogin != null && tabReg != null) {
            switchMode(register);
        }
        refreshHealth();
    }

    public void refreshHealth() {
        if (healthPill == null) return;
        healthDot.setFill(Color.web(UITheme.INK_DIM));
        healthLbl.setText("Checking API...");
        healthLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 500; -fx-text-fill: #52525B;");
        healthPill.setStyle("-fx-background-color: #F4F4F5; -fx-border-color: #E4E4E7; -fx-border-width: 1; -fx-border-radius: 999; -fx-background-radius: 999; -fx-padding: 3 9; -fx-cursor: hand;");
        new Thread(() -> {
            boolean up = ApiClient.checkBackendHealth();
            Platform.runLater(() -> {
                if (up) {
                    healthDot.setFill(Color.web(UITheme.POS_DOT));
                    healthLbl.setText("API online");
                    healthLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 500; -fx-text-fill: #166534;");
                    healthPill.setStyle("-fx-background-color: #F0FDF4; -fx-border-color: #BBF7D0; -fx-border-width: 1; -fx-border-radius: 999; -fx-background-radius: 999; -fx-padding: 3 9; -fx-cursor: hand;");
                } else {
                    healthDot.setFill(Color.web(UITheme.COLOR_DANGER));
                    healthLbl.setText("API offline");
                    healthLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 500; -fx-text-fill: #991B1B;");
                    healthPill.setStyle("-fx-background-color: #FEF2F2; -fx-border-color: #FECACA; -fx-border-width: 1; -fx-border-radius: 999; -fx-background-radius: 999; -fx-padding: 3 9; -fx-cursor: hand;");
                }
            });
        }).start();
    }

    private void buildBackground() {
        getStyleClass().add("serko-hero-bg");
    }

    private void buildTopBackBar() {
        if (onBackToHome == null) return;

        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(20, 28, 0, 28));
        topBar.setMaxHeight(64);
        StackPane.setAlignment(topBar, Pos.TOP_LEFT);

        Button btnBack = new Button("Back to website", UITheme.arrowLeftIcon(14, Color.web("#18181B")));
        btnBack.setContentDisplay(ContentDisplay.LEFT);
        btnBack.setGraphicTextGap(7);
        btnBack.getStyleClass().add("ws-btn-secondary");
        btnBack.setStyle(
            "-fx-background-color: rgba(255,255,255,0.78); -fx-text-fill: #18181B;" +
            "-fx-border-color: rgba(0,0,0,0.09); -fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-font-size: 12.5px; -fx-font-weight: 500; -fx-padding: 7 13; -fx-cursor: hand;"
        );
        btnBack.setOnAction(e -> onBackToHome.run());

        topBar.getChildren().add(btnBack);
        getChildren().add(topBar);
    }

    // ── Centered Crisp Card ──────────────────────────────────────
    private void buildCard() {
        VBox card = new VBox(20);
        card.setMaxWidth(424);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setPadding(new Insets(30, 32, 28, 32));
        card.setStyle(
            "-fx-background-color: #FFFFFF;" +
            "-fx-background-radius: 14;" +
            "-fx-border-color: " + UITheme.WS_BORDER + ";" +
            "-fx-border-radius: 14;" +
            "-fx-border-width: 1;"
        );

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.06));
        shadow.setRadius(24);
        shadow.setOffsetY(8);
        card.setEffect(shadow);

        card.getChildren().addAll(buildCardHeader(), buildCardBody());

        card.setOpacity(0);
        card.setTranslateY(12);
        FadeTransition ft = new FadeTransition(Duration.millis(300), card);
        ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), card);
        tt.setToY(0);
        ft.play();
        tt.play();

        getChildren().add(card);
    }

    private VBox buildCardHeader() {
        VBox header = new VBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        HBox brandRow = new HBox(8);
        brandRow.setAlignment(Pos.CENTER_LEFT);
        StackPane logo = UITheme.brandLogoMark(20, false);
        Label brand = new Label("IncidentIQ");
        brand.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #09090B;");

        Region bsp = new Region();
        HBox.setHgrow(bsp, Priority.ALWAYS);

        healthDot = new Circle(3.5, Color.web(UITheme.INK_DIM));
        healthLbl = new Label("Checking API...");
        healthLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 500; -fx-text-fill: #52525B;");
        healthPill = new HBox(5, healthDot, healthLbl);
        healthPill.setAlignment(Pos.CENTER);
        healthPill.setStyle("-fx-background-color: #F4F4F5; -fx-border-color: #E4E4E7; -fx-border-width: 1; -fx-border-radius: 999; -fx-background-radius: 999; -fx-padding: 3 9; -fx-cursor: hand;");
        healthPill.setOnMouseClicked(e -> refreshHealth());
        Tooltip.install(healthPill, new Tooltip("Spring Boot API status at http://localhost:8080 (click to re-check)"));

        brandRow.getChildren().addAll(logo, brand, bsp, healthPill);

        cardHeading = new Label("Sign in to IncidentIQ");
        cardHeading.setStyle("-fx-font-size: 22px; -fx-font-weight: 600; -fx-text-fill: #09090B;");

        cardSubtitle = new Label("Manage incidents and run multi-agent root cause analysis.");
        cardSubtitle.setWrapText(true);
        cardSubtitle.setStyle("-fx-font-size: 13.5px; -fx-text-fill: #52525B;");

        header.getChildren().addAll(brandRow, cardHeading, cardSubtitle);
        return header;
    }

    private VBox buildCardBody() {
        VBox body = new VBox(14);

        // Flat 8px Segmented Mode Switcher
        HBox tabs = new HBox(4);
        tabs.setStyle(
            "-fx-background-color: #F4F4F5;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 3;"
        );
        tabLogin = styledTabBtn("Sign in", true);
        tabReg   = styledTabBtn("Register", false);
        HBox.setHgrow(tabLogin, Priority.ALWAYS);
        HBox.setHgrow(tabReg, Priority.ALWAYS);
        tabLogin.setMaxWidth(Double.MAX_VALUE);
        tabReg.setMaxWidth(Double.MAX_VALUE);
        tabs.getChildren().addAll(tabLogin, tabReg);

        // Full Name (Register mode)
        txtFullName = new TextField();
        txtFullName.setPromptText("Alex Morgan");
        fullNameBox = UITheme.formField("Full name", txtFullName);
        fullNameBox.setVisible(false);
        fullNameBox.setManaged(false);

        // Email
        txtEmail = new TextField();
        txtEmail.setPromptText("engineer@company.com");
        VBox emailBox = UITheme.formField("Work email", txtEmail);

        // Password + Reveal Toggle
        txtPass = new PasswordField();
        txtPass.setPromptText("••••••••");
        txtPass.setStyle("-fx-padding: 9 38 9 12;");

        txtPassVisible = new TextField();
        txtPassVisible.setPromptText("••••••••");
        txtPassVisible.setStyle("-fx-padding: 9 38 9 12;");
        txtPassVisible.setVisible(false);
        txtPassVisible.setManaged(false);
        txtPassVisible.textProperty().bindBidirectional(txtPass.textProperty());

        Button btnEye = new Button();
        btnEye.setGraphic(UITheme.eyeIcon(15, Color.web("#71717A")));
        btnEye.setStyle("-fx-background-color: transparent; -fx-padding: 5 7; -fx-background-radius: 6; -fx-cursor: hand;");
        btnEye.setFocusTraversable(false);
        btnEye.setTooltip(new Tooltip("Show password"));
        btnEye.setOnMouseEntered(e -> btnEye.setStyle("-fx-background-color: #F4F4F5; -fx-padding: 5 7; -fx-background-radius: 6; -fx-cursor: hand;"));
        btnEye.setOnMouseExited(e -> btnEye.setStyle("-fx-background-color: transparent; -fx-padding: 5 7; -fx-background-radius: 6; -fx-cursor: hand;"));
        btnEye.setOnAction(e -> {
            passVisible = !passVisible;
            txtPassVisible.setVisible(passVisible);
            txtPassVisible.setManaged(passVisible);
            txtPass.setVisible(!passVisible);
            txtPass.setManaged(!passVisible);
            btnEye.setGraphic(passVisible
                ? UITheme.eyeOffIcon(15, Color.web("#09090B"))
                : UITheme.eyeIcon(15, Color.web("#71717A")));
            btnEye.setTooltip(new Tooltip(passVisible ? "Hide password" : "Show password"));
            if (passVisible) {
                txtPassVisible.requestFocus();
                txtPassVisible.positionCaret(txtPassVisible.getText().length());
            } else {
                txtPass.requestFocus();
                txtPass.positionCaret(txtPass.getText().length());
            }
        });

        StackPane passWrap = new StackPane(txtPass, txtPassVisible, btnEye);
        StackPane.setAlignment(btnEye, Pos.CENTER_RIGHT);
        StackPane.setMargin(btnEye, new Insets(0, 6, 0, 0));
        VBox passBox = UITheme.formFieldHint("Password", "(min 6 chars)", passWrap);

        // Inline Validation Banner with SVG Alert Icon
        statusBanner = new HBox(8);
        statusBanner.setAlignment(Pos.CENTER_LEFT);
        statusBanner.setPadding(new Insets(9, 12, 9, 12));
        statusBanner.setStyle(
            "-fx-background-color: #FEF2F2; -fx-border-color: #FECACA; -fx-border-radius: 8; -fx-background-radius: 8;"
        );
        statusLbl = new Label("");
        statusLbl.setWrapText(true);
        statusLbl.setStyle("-fx-text-fill: #991B1B; -fx-font-size: 12.5px; -fx-font-weight: 500;");
        statusBanner.getChildren().addAll(UITheme.alertIcon(15, Color.web("#DC2626")), statusLbl);
        statusBanner.setVisible(false);
        statusBanner.setManaged(false);

        // Primary 8px Action Button
        btnPrimary = UITheme.wsPrimaryBtn("Sign in", null);
        btnPrimary.setMaxWidth(Double.MAX_VALUE);
        btnPrimary.setPrefHeight(40);

        btnToggle = new Button("Need an account? Register");
        btnToggle.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: #52525B;" +
            "-fx-font-size: 12.5px;" +
            "-fx-font-weight: 500;" +
            "-fx-cursor: hand;"
        );
        btnToggle.setMaxWidth(Double.MAX_VALUE);
        btnToggle.setOnMouseEntered(e -> btnToggle.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #09090B; -fx-font-size: 12.5px; -fx-font-weight: 500; -fx-underline: true; -fx-cursor: hand;"
        ));
        btnToggle.setOnMouseExited(e -> btnToggle.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #52525B; -fx-font-size: 12.5px; -fx-font-weight: 500; -fx-cursor: hand;"
        ));

        body.getChildren().addAll(
            tabs, fullNameBox, emailBox, passBox, statusBanner, btnPrimary, btnToggle
        );

        tabLogin.setOnAction(e -> switchMode(false));
        tabReg.setOnAction(e -> switchMode(true));
        btnToggle.setOnAction(e -> switchMode(!registerMode));

        btnPrimary.setOnAction(e -> {
            if (registerMode) doRegister();
            else doLogin();
        });
        txtPass.setOnAction(e -> btnPrimary.fire());
        txtPassVisible.setOnAction(e -> btnPrimary.fire());
        txtEmail.setOnAction(e -> {
            if (passVisible) txtPassVisible.requestFocus();
            else txtPass.requestFocus();
        });

        return body;
    }

    private Button styledTabBtn(String text, boolean active) {
        Button btn = new Button(text);
        btn.setCursor(Cursor.HAND);
        applyTabStyle(btn, active);
        return btn;
    }

    private void applyTabStyle(Button btn, boolean active) {
        if (active) {
            btn.setStyle(
                "-fx-background-color: #FFFFFF;" +
                "-fx-text-fill: #09090B;" +
                "-fx-border-color: #E4E4E7;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 6;" +
                "-fx-font-weight: 500;" +
                "-fx-font-size: 12.5px;" +
                "-fx-background-radius: 6;" +
                "-fx-padding: 6 0;" +
                "-fx-cursor: hand;"
            );
        } else {
            btn.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: #71717A;" +
                "-fx-border-color: transparent;" +
                "-fx-border-width: 1;" +
                "-fx-font-weight: 400;" +
                "-fx-font-size: 12.5px;" +
                "-fx-background-radius: 6;" +
                "-fx-padding: 6 0;" +
                "-fx-cursor: hand;"
            );
        }
    }

    private void switchMode(boolean toRegister) {
        registerMode = toRegister;
        applyTabStyle(tabLogin, !toRegister);
        applyTabStyle(tabReg, toRegister);
        fullNameBox.setVisible(toRegister);
        fullNameBox.setManaged(toRegister);
        cardHeading.setText(toRegister ? "Create workspace account" : "Sign in to IncidentIQ");
        cardSubtitle.setText(toRegister
            ? "Create an engineering account to report incidents and run AI triage."
            : "Manage incidents and run multi-agent root cause analysis.");
        btnPrimary.setText(toRegister ? "Create account" : "Sign in");
        btnToggle.setText(toRegister
            ? "Already have an account? Sign in"
            : "Need an account? Register");
        hideStatus();
    }

    // ── Auth Calls ────────────────────────────────────────────────
    private void doLogin() {
        String email = txtEmail.getText().trim();
        String pass  = txtPass.getText().trim();
        if (email.isEmpty() || pass.isEmpty()) {
            showError("Email and password are required.");
            return;
        }

        setLoading(true, "Sign in");
        new Thread(() -> {
            try {
                HttpResponse<String> res = ApiClient.login(email, pass);
                if (res.statusCode() == 200 || res.statusCode() == 201) {
                    JsonNode node = mapper.readTree(res.body());
                    ApiClient.setAuth(node.path("token").asText(), email);
                    Platform.runLater(() -> {
                        setLoading(false, "Sign in");
                        onLoginSuccess.run();
                    });
                } else {
                    String msg = extractMessage(res.body(), "Invalid credentials (" + res.statusCode() + ")");
                    Platform.runLater(() -> { showError(msg); setLoading(false, "Sign in"); });
                }
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showError("Cannot reach Spring Boot backend at http://localhost:8080.");
                    setLoading(false, "Sign in");
                });
            }
        }).start();
    }

    private void doRegister() {
        String fullName = txtFullName.getText().trim();
        String email    = txtEmail.getText().trim();
        String pass     = txtPass.getText().trim();
        if (email.isEmpty() || pass.isEmpty()) {
            showError("Email and password are required.");
            return;
        }
        if (pass.length() < 6) {
            showError("Password must be at least 6 characters.");
            return;
        }

        setLoading(true, "Create account");
        new Thread(() -> {
            try {
                HttpResponse<String> res = ApiClient.register(fullName, email, pass);
                if (res.statusCode() == 200 || res.statusCode() == 201) {
                    JsonNode node = mapper.readTree(res.body());
                    ApiClient.setAuth(node.path("token").asText(), email);
                    Platform.runLater(() -> {
                        setLoading(false, "Create account");
                        onLoginSuccess.run();
                    });
                } else {
                    String msg = extractMessage(res.body(), "Registration failed (" + res.statusCode() + ")");
                    Platform.runLater(() -> { showError(msg); setLoading(false, "Create account"); });
                }
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showError("Cannot reach Spring Boot backend at http://localhost:8080.");
                    setLoading(false, "Create account");
                });
            }
        }).start();
    }

    private void setLoading(boolean loading, String idleLabel) {
        btnPrimary.setDisable(loading);
        btnPrimary.setText(loading ? "Please wait..." : idleLabel);
    }

    private void showError(String msg) {
        statusLbl.setText(msg);
        statusBanner.setVisible(true);
        statusBanner.setManaged(true);
    }

    private void hideStatus() {
        statusBanner.setVisible(false);
        statusBanner.setManaged(false);
    }

    private String extractMessage(String body, String fallback) {
        try { return mapper.readTree(body).path("message").asText(fallback); }
        catch (Exception e) { return fallback; }
    }
}
