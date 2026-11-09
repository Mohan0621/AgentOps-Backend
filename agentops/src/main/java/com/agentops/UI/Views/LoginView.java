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
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Serko-aligned Light Editorial + Horizon Glow Authentication Screen.
 * Supports Sign In and Register backed by /api/login and /api/register.
 */
public class LoginView extends StackPane {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Runnable onLoginSuccess;
    private final Runnable onBackToHome;

    private TextField txtFullName;
    private TextField txtEmail;
    private PasswordField txtPass;
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
    }

    public void setRegisterMode(boolean register) {
        if (tabLogin != null && tabReg != null) {
            switchMode(register);
        }
    }

    // ── Serko Horizon Aura Backdrop ───────────────────────────────
    private void buildBackground() {
        getStyleClass().add("serko-hero-bg");
    }

    private void buildTopBackBar() {
        if (onBackToHome == null) return;

        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(22, 32, 0, 32));
        topBar.setMaxHeight(68);
        StackPane.setAlignment(topBar, Pos.TOP_CENTER);

        Button btnBack = new Button("←  Back to website");
        btnBack.getStyleClass().add("serko-btn-white");
        btnBack.setPadding(new Insets(8, 18, 8, 18));
        btnBack.setOnAction(e -> onBackToHome.run());

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        HBox chip = new HBox(8);
        chip.setAlignment(Pos.CENTER);
        chip.setPadding(new Insets(6, 14, 6, 14));
        chip.setStyle("-fx-background-color: rgba(255,255,255,0.75); -fx-background-radius: 999;");
        Label dot = new Label("●");
        dot.setStyle("-fx-text-fill: " + UITheme.POS_DOT + "; -fx-font-size: 9px;");
        Label apiLbl = new Label("Spring Boot Auth  ·  JWT");
        apiLbl.setStyle("-fx-text-fill: #3F3F46; -fx-font-size: 12px; -fx-font-weight: 600;");
        chip.getChildren().addAll(dot, apiLbl);

        topBar.getChildren().addAll(btnBack, sp, chip);
        getChildren().add(topBar);
    }

    // ── Main Centered Card ────────────────────────────────────────
    private void buildCard() {
        VBox card = new VBox(22);
        card.setMaxWidth(450);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setPadding(new Insets(34, 36, 32, 36));
        card.setStyle(
            "-fx-background-color: #FFFFFF;" +
            "-fx-background-radius: 24;" +
            "-fx-border-color: rgba(0, 0, 0, 0.06);" +
            "-fx-border-radius: 24;" +
            "-fx-border-width: 1;"
        );

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.08));
        shadow.setRadius(42);
        shadow.setOffsetY(14);
        card.setEffect(shadow);

        card.getChildren().addAll(buildCardHeader(), buildCardBody());

        card.setOpacity(0);
        card.setTranslateY(18);
        FadeTransition ft = new FadeTransition(Duration.millis(420), card);
        ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(420), card);
        tt.setToY(0);
        ft.play();
        tt.play();

        getChildren().add(card);
    }

    private VBox buildCardHeader() {
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        HBox brandRow = new HBox(10);
        brandRow.setAlignment(Pos.CENTER_LEFT);
        StackPane logo = UITheme.brandLogoMark(24, false);
        Label brand = new Label("IncidentIQ");
        brand.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #000000;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label pill = UITheme.positiveBadge("AI Workspace");
        brandRow.getChildren().addAll(logo, brand, sp, pill);

        cardHeading = new Label("Welcome back");
        cardHeading.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #000000;");

        cardSubtitle = new Label("Sign in to investigate incidents and launch your 6-agent pipeline.");
        cardSubtitle.setWrapText(true);
        cardSubtitle.setStyle("-fx-font-size: 13.5px; -fx-text-fill: rgba(0,0,0,0.65);");

        header.getChildren().addAll(brandRow, cardHeading, cardSubtitle);
        return header;
    }

    private VBox buildCardBody() {
        VBox body = new VBox(15);

        // Segmented Pill Switcher (#F6F6F6 track)
        HBox tabs = new HBox(4);
        tabs.setStyle(
            "-fx-background-color: #F6F6F6;" +
            "-fx-background-radius: 999;" +
            "-fx-padding: 4;"
        );
        tabLogin = styledTabBtn("Sign In", true);
        tabReg   = styledTabBtn("Register", false);
        HBox.setHgrow(tabLogin, Priority.ALWAYS);
        HBox.setHgrow(tabReg, Priority.ALWAYS);
        tabLogin.setMaxWidth(Double.MAX_VALUE);
        tabReg.setMaxWidth(Double.MAX_VALUE);
        tabs.getChildren().addAll(tabLogin, tabReg);

        // Full Name (Register mode)
        txtFullName = new TextField();
        txtFullName.setPromptText("Alex Morgan");
        fullNameBox = UITheme.formField("Full Name", txtFullName);
        fullNameBox.setVisible(false);
        fullNameBox.setManaged(false);

        // Email
        txtEmail = new TextField();
        txtEmail.setPromptText("engineer@company.com");
        VBox emailBox = UITheme.formField("Work Email", txtEmail);

        // Password
        txtPass = new PasswordField();
        txtPass.setPromptText("••••••••");
        VBox passBox = UITheme.formFieldHint("Password", "(min 6 chars)", txtPass);

        // Inline Error / Feedback Banner
        statusLbl = new Label("");
        statusLbl.setWrapText(true);
        statusLbl.setManaged(false);
        statusLbl.setVisible(false);

        // Black Serko Primary Pill
        btnPrimary = UITheme.serkoBlackPill("Sign In", true);
        btnPrimary.setMaxWidth(Double.MAX_VALUE);
        btnPrimary.setPrefHeight(46);

        // Secondary mode switch link
        btnToggle = new Button("Don't have an account? Register");
        btnToggle.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: #4D4D4D;" +
            "-fx-font-size: 12.5px;" +
            "-fx-font-weight: 600;" +
            "-fx-cursor: hand;"
        );
        btnToggle.setMaxWidth(Double.MAX_VALUE);
        btnToggle.setOnMouseEntered(e -> btnToggle.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #000000; -fx-font-size: 12.5px; -fx-font-weight: 600; -fx-underline: true; -fx-cursor: hand;"
        ));
        btnToggle.setOnMouseExited(e -> btnToggle.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #4D4D4D; -fx-font-size: 12.5px; -fx-font-weight: 600; -fx-cursor: hand;"
        ));

        body.getChildren().addAll(
            tabs, fullNameBox, emailBox, passBox, statusLbl, btnPrimary, btnToggle
        );

        tabLogin.setOnAction(e -> switchMode(false));
        tabReg.setOnAction(e -> switchMode(true));
        btnToggle.setOnAction(e -> switchMode(!registerMode));

        btnPrimary.setOnAction(e -> {
            if (registerMode) doRegister();
            else doLogin();
        });
        txtPass.setOnAction(e -> btnPrimary.fire());
        txtEmail.setOnAction(e -> txtPass.requestFocus());

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
                "-fx-text-fill: #000000;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 13px;" +
                "-fx-background-radius: 999;" +
                "-fx-padding: 8 0;" +
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
                "-fx-text-fill: #71717A;" +
                "-fx-font-weight: 600;" +
                "-fx-font-size: 13px;" +
                "-fx-background-radius: 999;" +
                "-fx-padding: 8 0;" +
                "-fx-cursor: hand;"
            );
            btn.setEffect(null);
        }
    }

    private void switchMode(boolean toRegister) {
        registerMode = toRegister;
        applyTabStyle(tabLogin, !toRegister);
        applyTabStyle(tabReg, toRegister);
        fullNameBox.setVisible(toRegister);
        fullNameBox.setManaged(toRegister);
        cardHeading.setText(toRegister ? "Create workspace" : "Welcome back");
        cardSubtitle.setText(toRegister
            ? "Create an engineering account to report incidents and run AI triage."
            : "Sign in to investigate incidents and launch your 6-agent pipeline.");
        btnPrimary.setText(toRegister ? "Create Account" : "Sign In");
        btnToggle.setText(toRegister
            ? "Already have an account? Sign In"
            : "Don't have an account? Register");
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

        setLoading(true, "Sign In");
        new Thread(() -> {
            try {
                HttpResponse<String> res = ApiClient.login(email, pass);
                if (res.statusCode() == 200 || res.statusCode() == 201) {
                    JsonNode node = mapper.readTree(res.body());
                    ApiClient.setAuth(node.path("token").asText(), email);
                    Platform.runLater(() -> {
                        setLoading(false, "Sign In");
                        onLoginSuccess.run();
                    });
                } else {
                    String msg = extractMessage(res.body(), "Invalid credentials (" + res.statusCode() + ")");
                    Platform.runLater(() -> { showError(msg); setLoading(false, "Sign In"); });
                }
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showError("Cannot reach Spring Boot backend at http://localhost:8080.");
                    setLoading(false, "Sign In");
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

        setLoading(true, "Create Account");
        new Thread(() -> {
            try {
                HttpResponse<String> res = ApiClient.register(fullName, email, pass);
                if (res.statusCode() == 200 || res.statusCode() == 201) {
                    JsonNode node = mapper.readTree(res.body());
                    ApiClient.setAuth(node.path("token").asText(), email);
                    Platform.runLater(() -> {
                        setLoading(false, "Create Account");
                        onLoginSuccess.run();
                    });
                } else {
                    String msg = extractMessage(res.body(), "Registration failed (" + res.statusCode() + ")");
                    Platform.runLater(() -> { showError(msg); setLoading(false, "Create Account"); });
                }
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showError("Cannot reach Spring Boot backend at http://localhost:8080.");
                    setLoading(false, "Create Account");
                });
            }
        }).start();
    }

    private void setLoading(boolean loading, String idleLabel) {
        btnPrimary.setDisable(loading);
        btnPrimary.setText(loading ? "Please wait..." : idleLabel);
    }

    private void showError(String msg) {
        statusLbl.setText("⚠  " + msg);
        statusLbl.setStyle(
            "-fx-text-fill: #B91C1C;" +
            "-fx-font-size: 12.5px;" +
            "-fx-font-weight: 600;" +
            "-fx-background-color: #FEE2E2;" +
            "-fx-border-color: #FECACA;" +
            "-fx-border-radius: 12;" +
            "-fx-padding: 10 14;" +
            "-fx-background-radius: 12;"
        );
        statusLbl.setVisible(true);
        statusLbl.setManaged(true);
    }

    private void hideStatus() {
        statusLbl.setVisible(false);
        statusLbl.setManaged(false);
    }

    private String extractMessage(String body, String fallback) {
        try { return mapper.readTree(body).path("message").asText(fallback); }
        catch (Exception e) { return fallback; }
    }
}
