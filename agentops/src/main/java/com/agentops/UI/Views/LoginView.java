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

    public LoginView(Runnable onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
        setupUI();
    }

    private void setupUI() {
        setAlignment(Pos.CENTER);
        setSpacing(15);
        setPadding(new Insets(40));
        setStyle("-fx-background-color: #1e1e2f;");

        Label title = new Label("IncidentIQ Workspace");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Label subtitle = new Label("Sign in or register to access the incident & AI investigation console");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #a0a0b0;");

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(12);

        Label lblEmail = new Label("Email:");
        lblEmail.setStyle("-fx-text-fill: #ffffff;");
        TextField txtEmail = new TextField("engineer@incidentiq.com");
        txtEmail.setPrefWidth(260);

        Label lblPass = new Label("Password:");
        lblPass.setStyle("-fx-text-fill: #ffffff;");
        PasswordField txtPass = new PasswordField();
        txtPass.setText("password123");
        txtPass.setPrefWidth(260);

        grid.add(lblEmail, 0, 0);
        grid.add(txtEmail, 1, 0);
        grid.add(lblPass, 0, 1);
        grid.add(txtPass, 1, 1);

        Label statusLbl = new Label("");
        statusLbl.setStyle("-fx-text-fill: #ff6b6b;");

        HBox btnBox = new HBox(12);
        btnBox.setAlignment(Pos.CENTER);

        Button btnLogin = new Button("Login");
        btnLogin.setStyle("-fx-background-color: #4361ee; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20;");

        Button btnRegister = new Button("Register New Account");
        btnRegister.setStyle("-fx-background-color: #3f37c9; -fx-text-fill: white; -fx-padding: 8 16;");

        btnBox.getChildren().addAll(btnLogin, btnRegister);

        btnLogin.setOnAction(e -> authenticate(txtEmail.getText().trim(), txtPass.getText().trim(), false, statusLbl));
        btnRegister.setOnAction(e -> authenticate(txtEmail.getText().trim(), txtPass.getText().trim(), true, statusLbl));

        getChildren().addAll(title, subtitle, grid, btnBox, statusLbl);
    }

    private void authenticate(String email, String pass, boolean isRegister, Label statusLbl) {
        statusLbl.setStyle("-fx-text-fill: #a0a0b0;");
        statusLbl.setText("Authenticating...");

        new Thread(() -> {
            try {
                HttpResponse<String> res = isRegister ? ApiClient.register(email, pass) : ApiClient.login(email, pass);
                if (res.statusCode() == 200) {
                    JsonNode node = mapper.readTree(res.body());
                    String token = node.path("token").asText();
                    ApiClient.setAuth(token, email);
                    Platform.runLater(onLoginSuccess);
                } else {
                    Platform.runLater(() -> {
                        statusLbl.setStyle("-fx-text-fill: #ff6b6b;");
                        statusLbl.setText((isRegister ? "Registration" : "Login") + " failed: " + res.body());
                    });
                }
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    statusLbl.setStyle("-fx-text-fill: #ff6b6b;");
                    statusLbl.setText("Connection error: " + ex.getMessage());
                });
            }
        }).start();
    }
}

