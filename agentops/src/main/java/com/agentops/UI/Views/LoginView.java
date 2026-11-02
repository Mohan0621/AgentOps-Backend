package com.agentops.UI.Views;

import java.net.http.HttpResponse;

import com.agentops.UI.ApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class LoginView extends VBox {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Runnable onLoginSuccess;

    // Fields
    private TextField txtFullName;
    private TextField txtEmail;
    private PasswordField txtPass;
    private Label statusLbl;
    private Label lblFullName;

    // Track current mode
    private boolean registerMode = false;

    public LoginView(Runnable onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
        setupUI();
    }

    private void setupUI() {
        setAlignment(Pos.CENTER);
        setSpacing(15);
        setPadding(new Insets(40));
        setStyle("-fx-background-color: " + UITheme.BG_DARK + ";");

        // Brand
        Label title = new Label("⚡ IncidentIQ");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + UITheme.ACCENT_CYAN + ";");

        Label subtitle = new Label("AI-Powered Incident Management & Multi-Agent Investigation");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: " + UITheme.TEXT_MUTED + ";");

        // Form grid
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(12);
        grid.setVgap(14);
        grid.setPadding(new Insets(20, 30, 20, 30));
        grid.setStyle("-fx-background-color: #2b2b40; -fx-background-radius: 10;");

        // Full name (only for register)
        lblFullName = new Label("Full Name:");
        lblFullName.setStyle("-fx-text-fill: #ffffff;");
        txtFullName = new TextField();
        txtFullName.setPromptText("John Doe");
        txtFullName.setPrefWidth(270);
        lblFullName.setVisible(false);
        lblFullName.setManaged(false);
        txtFullName.setVisible(false);
        txtFullName.setManaged(false);

        // Email
        Label lblEmail = new Label("Email:");
        lblEmail.setStyle("-fx-text-fill: #ffffff;");
        txtEmail = new TextField();
        txtEmail.setPromptText("you@example.com");
        txtEmail.setPrefWidth(270);

        // Password
        Label lblPass = new Label("Password:");
        lblPass.setStyle("-fx-text-fill: #ffffff;");
        txtPass = new PasswordField();
        txtPass.setPromptText("••••••••");
        txtPass.setPrefWidth(270);

        grid.add(lblFullName, 0, 0);
        grid.add(txtFullName, 1, 0);
        grid.add(lblEmail, 0, 1);
        grid.add(txtEmail, 1, 1);
        grid.add(lblPass, 0, 2);
        grid.add(txtPass, 1, 2);

        // Status label
        statusLbl = new Label("");
        statusLbl.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 12px;");
        statusLbl.setWrapText(true);
        statusLbl.setMaxWidth(350);

        // Buttons
        HBox btnBox = new HBox(12);
        btnBox.setAlignment(Pos.CENTER);

        Button btnPrimary = new Button("Sign In");
        btnPrimary.setPrefWidth(130);
        btnPrimary.setStyle("-fx-background-color: " + UITheme.ACCENT_PRIMARY + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 9 20; -fx-background-radius: 6;");

        Button btnToggle = new Button("Create Account");
        btnToggle.setPrefWidth(150);
        btnToggle.setStyle("-fx-background-color: #2b2b40; -fx-text-fill: " + UITheme.TEXT_MUTED + "; -fx-font-size: 13px; -fx-padding: 9 16; -fx-border-color: #3a3a5c; -fx-border-radius: 6; -fx-background-radius: 6;");

        btnBox.getChildren().addAll(btnPrimary, btnToggle);

        // Toggle between login / register mode
        btnToggle.setOnAction(e -> {
            registerMode = !registerMode;
            if (registerMode) {
                btnPrimary.setText("Register");
                btnToggle.setText("Back to Sign In");
                lblFullName.setVisible(true);
                lblFullName.setManaged(true);
                txtFullName.setVisible(true);
                txtFullName.setManaged(true);
            } else {
                btnPrimary.setText("Sign In");
                btnToggle.setText("Create Account");
                lblFullName.setVisible(false);
                lblFullName.setManaged(false);
                txtFullName.setVisible(false);
                txtFullName.setManaged(false);
            }
            statusLbl.setText("");
        });

        // Primary action
        btnPrimary.setOnAction(e -> {
            if (registerMode) {
                doRegister(btnPrimary);
            } else {
                doLogin(btnPrimary);
            }
        });

        // Allow Enter key to submit
        txtPass.setOnAction(e -> btnPrimary.fire());

        getChildren().addAll(title, subtitle, grid, btnBox, statusLbl);
    }

    private void doLogin(Button btn) {
        String email = txtEmail.getText().trim();
        String pass = txtPass.getText().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            showError("Email and password are required.");
            return;
        }

        btn.setDisable(true);
        btn.setText("Signing in...");
        statusLbl.setStyle("-fx-text-fill: " + UITheme.TEXT_MUTED + ";");
        statusLbl.setText("Authenticating...");

        new Thread(() -> {
            try {
                HttpResponse<String> res = ApiClient.login(email, pass);
                if (res.statusCode() == 200 || res.statusCode() == 201) {
                    JsonNode node = mapper.readTree(res.body());
                    String token = node.path("token").asText();
                    ApiClient.setAuth(token, email);
                    Platform.runLater(onLoginSuccess);
                } else {
                    JsonNode node = mapper.readTree(res.body());
                    String msg = node.has("message") ? node.path("message").asText() : "Login failed (" + res.statusCode() + ")";
                    Platform.runLater(() -> {
                        showError(msg);
                        btn.setDisable(false);
                        btn.setText("Sign In");
                    });
                }
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showError("Connection error: " + ex.getMessage());
                    btn.setDisable(false);
                    btn.setText("Sign In");
                });
            }
        }).start();
    }

    private void doRegister(Button btn) {
        String fullName = txtFullName.getText().trim();
        String email = txtEmail.getText().trim();
        String pass = txtPass.getText().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            showError("Email and password are required.");
            return;
        }
        if (pass.length() < 6) {
            showError("Password must be at least 6 characters.");
            return;
        }

        btn.setDisable(true);
        btn.setText("Creating account...");
        statusLbl.setStyle("-fx-text-fill: " + UITheme.TEXT_MUTED + ";");
        statusLbl.setText("Registering...");

        new Thread(() -> {
            try {
                HttpResponse<String> res = ApiClient.register(fullName, email, pass);
                if (res.statusCode() == 200 || res.statusCode() == 201) {
                    JsonNode node = mapper.readTree(res.body());
                    String token = node.path("token").asText();
                    ApiClient.setAuth(token, email);
                    Platform.runLater(onLoginSuccess);
                } else {
                    JsonNode node = mapper.readTree(res.body());
                    String msg = node.has("message") ? node.path("message").asText() : "Registration failed (" + res.statusCode() + ")";
                    Platform.runLater(() -> {
                        showError(msg);
                        btn.setDisable(false);
                        btn.setText("Register");
                    });
                }
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showError("Connection error: " + ex.getMessage());
                    btn.setDisable(false);
                    btn.setText("Register");
                });
            }
        }).start();
    }

    private void showError(String msg) {
        statusLbl.setStyle("-fx-text-fill: #ff6b6b;");
        statusLbl.setText(msg);
    }
}
