package com.agentops.UI.Models;

import javafx.beans.property.SimpleStringProperty;

public class IncidentRow {
    public final SimpleStringProperty id;
    public final SimpleStringProperty title;
    public final SimpleStringProperty severity;
    public final SimpleStringProperty status;
    public final SimpleStringProperty type;
    public final SimpleStringProperty description;
    public final SimpleStringProperty errorLogs;

    public IncidentRow(String id, String title, String severity, String status, String type, String desc, String logs) {
        this.id = new SimpleStringProperty(id);
        this.title = new SimpleStringProperty(title);
        this.severity = new SimpleStringProperty(severity);
        this.status = new SimpleStringProperty(status);
        this.type = new SimpleStringProperty(type);
        this.description = new SimpleStringProperty(desc);
        this.errorLogs = new SimpleStringProperty(logs);
    }
}

