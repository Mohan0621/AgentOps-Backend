package com.agentops.UI.Views;

import java.net.http.HttpResponse;

import com.agentops.UI.ApiClient;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class ReportIncidentView extends VBox {

    private final Runnable onIncidentCreated;

    public ReportIncidentView(Runnable onIncidentCreated) {
        this.onIncidentCreated = onIncidentCreated;
        setupUI();
    }

    private void setupUI() {
        setSpacing(12);
        setPadding(new Insets(25));
        setMaxWidth(700);

        Label heading = new Label("Report an Anomaly / Error Incident");
        heading.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + UITheme.ACCENT_CYAN + ";");

        TextField txtTitle = new TextField();
        txtTitle.setPromptText("Incident Title (e.g., NullPointerException in Payment Gateway)");

        TextArea txtDesc = new TextArea();
        txtDesc.setPromptText("Detailed description of user impact or observed behavior...");
        txtDesc.setPrefRowCount(3);

        TextArea txtLogs = new TextArea();
        txtLogs.setPromptText("Paste raw error logs, exceptions, or stack traces here...");
        txtLogs.setPrefRowCount(8);
        txtLogs.setStyle("-fx-font-family: 'Consolas', monospace;");

        ComboBox<String> cmbSeverity = new ComboBox<>();
        cmbSeverity.getItems().addAll("LOW", "MEDIUM", "HIGH", "CRITICAL");
        cmbSeverity.setValue("HIGH");

        Button btnSubmit = new Button("Submit Incident");
        btnSubmit.setStyle("-fx-background-color: " + UITheme.ACCENT_CYAN + "; -fx-text-fill: #12121c; -fx-font-weight: bold; -fx-padding: 8 22;");

        Label lblMsg = new Label();

        btnSubmit.setOnAction(e -> {
            if (txtTitle.getText().isBlank()) {
                lblMsg.setText("Title is required!");
                lblMsg.setStyle("-fx-text-fill: " + UITheme.COLOR_DANGER + ";");
                return;
            }
            new Thread(() -> {
                try {
                    HttpResponse<String> res = ApiClient.createIncident(
                            txtTitle.getText().trim(),
                            txtDesc.getText().trim(),
                            txtLogs.getText().trim(),
                            cmbSeverity.getValue()
                    );
                    if (res.statusCode() == 201) {
                        Platform.runLater(() -> {
                            lblMsg.setText("Incident reported successfully!");
                            lblMsg.setStyle("-fx-text-fill: #4ade80;");
                            txtTitle.clear();
                            txtDesc.clear();
                            txtLogs.clear();
                            if (onIncidentCreated != null) {
                                onIncidentCreated.run();
                            }
                        });
                    } else {
                        Platform.runLater(() -> {
                            lblMsg.setText("Submission error: " + res.body());
                            lblMsg.setStyle("-fx-text-fill: " + UITheme.COLOR_DANGER + ";");
                        });
                    }
                } catch (Exception ex) {
                    Platform.runLater(() -> lblMsg.setText("Error: " + ex.getMessage()));
                }
            }).start();
        });

        getChildren().addAll(
                heading,
                new Label("Title:"), txtTitle,
                new Label("Severity:"), cmbSeverity,
                new Label("Description:"), txtDesc,
                new Label("Stack Trace / Error Logs:"), txtLogs,
                btnSubmit, lblMsg
        );
    }
}

