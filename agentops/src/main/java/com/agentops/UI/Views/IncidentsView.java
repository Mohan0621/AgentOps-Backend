package com.agentops.UI.Views;

import java.net.http.HttpResponse;

import com.agentops.UI.ApiClient;
import com.agentops.UI.Models.IncidentRow;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class IncidentsView extends BorderPane {

    private final ObjectMapper mapper = new ObjectMapper();
    private final TableView<IncidentRow> incidentTable = new TableView<>();
    private final ObservableList<IncidentRow> incidentList = FXCollections.observableArrayList();
    private final TextArea txtDetails = new TextArea();

    public IncidentsView() {
        setupUI();
    }

    private void setupUI() {
        setPadding(new Insets(15));
        setStyle("-fx-background-color: " + UITheme.BG_DARK + ";");

        // Columns
        TableColumn<IncidentRow, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(c -> c.getValue().id);
        colId.setPrefWidth(50);

        TableColumn<IncidentRow, String> colTitle = new TableColumn<>("Title");
        colTitle.setCellValueFactory(c -> c.getValue().title);
        colTitle.setPrefWidth(220);

        TableColumn<IncidentRow, String> colSeverity = new TableColumn<>("Severity");
        colSeverity.setCellValueFactory(c -> c.getValue().severity);
        colSeverity.setPrefWidth(90);

        TableColumn<IncidentRow, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(c -> c.getValue().status);
        colStatus.setPrefWidth(110);

        TableColumn<IncidentRow, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(c -> c.getValue().type);
        colType.setPrefWidth(120);

        incidentTable.getColumns().addAll(colId, colTitle, colSeverity, colStatus, colType);
        incidentTable.setItems(incidentList);

        txtDetails.setEditable(false);
        txtDetails.setPromptText("Select an incident to view logs and trigger AI Multi-Agent investigations...");
        txtDetails.setWrapText(true);
        txtDetails.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px;");

        incidentTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                txtDetails.setText("=== Incident #" + newV.id.get() + " ===\n" +
                                   "Title: " + newV.title.get() + "\n" +
                                   "Severity: " + newV.severity.get() + " | Status: " + newV.status.get() + "\n\n" +
                                   "Description:\n" + newV.description.get() + "\n\n" +
                                   "Error Logs:\n" + newV.errorLogs.get());
            }
        });

        // Action Toolbar
        HBox toolbar = new HBox(12);
        toolbar.setPadding(new Insets(10, 0, 10, 0));
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Button btnRefresh = new Button("↻ Refresh Incidents");
        btnRefresh.setStyle("-fx-background-color: #2b2b40; -fx-text-fill: white;");
        btnRefresh.setOnAction(e -> refreshIncidents());

        Button btnQuickAi = new Button("⚡ Quick AI Diagnosis");
        btnQuickAi.setStyle("-fx-background-color: #7209b7; -fx-text-fill: white; -fx-font-weight: bold;");
        btnQuickAi.setOnAction(e -> triggerAiDiagnosis());

        Button btnMultiAgent = new Button("🤖 Run 6-Agent Investigation (LangGraph)");
        btnMultiAgent.setStyle("-fx-background-color: #4361ee; -fx-text-fill: white; -fx-font-weight: bold;");
        btnMultiAgent.setOnAction(e -> triggerMultiAgentInvestigation());

        toolbar.getChildren().addAll(btnRefresh, btnQuickAi, btnMultiAgent);

        VBox centerBox = new VBox(10, toolbar, incidentTable);
        VBox.setVgrow(incidentTable, javafx.scene.layout.Priority.ALWAYS);

        VBox rightBox = new VBox(10, new Label("Live Investigation Console & Agent Output:"), txtDetails);
        rightBox.setPrefWidth(420);
        VBox.setVgrow(txtDetails, javafx.scene.layout.Priority.ALWAYS);

        HBox split = new HBox(15, centerBox, rightBox);
        HBox.setHgrow(centerBox, javafx.scene.layout.Priority.ALWAYS);

        setCenter(split);
    }

    public void refreshIncidents() {
        new Thread(() -> {
            try {
                HttpResponse<String> res = ApiClient.getIncidents();
                if (res.statusCode() == 200) {
                    JsonNode root = mapper.readTree(res.body());
                    JsonNode content = root.path("content");

                    ObservableList<IncidentRow> rows = FXCollections.observableArrayList();
                    for (JsonNode item : content) {
                        rows.add(new IncidentRow(
                                item.path("id").asText(),
                                item.path("title").asText(),
                                item.path("severity").asText(),
                                item.path("status").asText(),
                                item.path("incidentType").asText("N/A"),
                                item.path("description").asText(""),
                                item.path("errorLogs").asText("")
                        ));
                    }
                    Platform.runLater(() -> incidentList.setAll(rows));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void triggerAiDiagnosis() {
        IncidentRow selected = incidentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selection Required", "Please select an incident from the queue first.");
            return;
        }

        txtDetails.appendText("\n\n>>> Triggering AI Diagnosis Engine (Groq / Gemini)...");
        new Thread(() -> {
            try {
                HttpResponse<String> res = ApiClient.runAiDiagnosis(Long.parseLong(selected.id.get()));
                if (res.statusCode() == 200) {
                    JsonNode node = mapper.readTree(res.body());
                    String formatted = "\n\n=== AI DIAGNOSIS COMPLETED ===" +
                                       "\nError Type: " + node.path("errorType").asText() +
                                       "\nSeverity: " + node.path("severity").asText() +
                                       "\nComponent: " + node.path("affectedComponent").asText() +
                                       "\nRoot Cause: " + node.path("rootCause").asText() +
                                       "\n\nSuggested Fixes:\n" + node.path("suggestedSolutions").toPrettyString();
                    Platform.runLater(() -> txtDetails.appendText(formatted));
                }
            } catch (Exception e) {
                Platform.runLater(() -> txtDetails.appendText("\nAI Diagnosis error: " + e.getMessage()));
            }
        }).start();
    }

    private void triggerMultiAgentInvestigation() {
        IncidentRow selected = incidentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selection Required", "Please select an incident from the queue first.");
            return;
        }

        txtDetails.appendText("\n\n>>> Orchestrating 6-Agent LangGraph Pipeline...\n" +
                              "- Log Analysis Agent...\n" +
                              "- Error Classification Agent...\n" +
                              "- Severity Assessment Agent...\n" +
                              "- Root Cause Agent...\n" +
                              "- Resolution Agent...\n" +
                              "- Report Agent...");

        new Thread(() -> {
            try {
                HttpResponse<String> res = ApiClient.runMultiAgentInvestigation(Long.parseLong(selected.id.get()));
                if (res.statusCode() == 200) {
                    JsonNode node = mapper.readTree(res.body());
                    String mdReport = node.path("finalReport").path("formattedMarkdownReport").asText();
                    Platform.runLater(() -> {
                        txtDetails.appendText("\n\n=== MULTI-AGENT INVESTIGATION REPORT ===\n" + mdReport);
                        refreshIncidents();
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> txtDetails.appendText("\nInvestigation failure: " + e.getMessage()));
            }
        }).start();
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}

