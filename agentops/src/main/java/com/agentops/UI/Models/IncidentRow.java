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
    public final SimpleStringProperty createdByEmail;
    public final SimpleStringProperty assignedToEmail;
    public final SimpleStringProperty createdAt;
    public final SimpleStringProperty updatedAt;

    public IncidentRow(String id, String title, String severity, String status, String type, String desc, String logs) {
        this(id, title, severity, status, type, desc, logs, "", "", "", "");
    }

    public IncidentRow(String id, String title, String severity, String status, String type,
                       String desc, String logs, String createdByEmail, String assignedToEmail,
                       String createdAt, String updatedAt) {
        this.id = new SimpleStringProperty(safe(id));
        this.title = new SimpleStringProperty(safe(title));
        this.severity = new SimpleStringProperty(safe(severity));
        this.status = new SimpleStringProperty(safe(status));
        this.type = new SimpleStringProperty(safe(type));
        this.description = new SimpleStringProperty(safe(desc));
        this.errorLogs = new SimpleStringProperty(safe(logs));
        this.createdByEmail = new SimpleStringProperty(safe(createdByEmail));
        this.assignedToEmail = new SimpleStringProperty(safe(assignedToEmail));
        this.createdAt = new SimpleStringProperty(safe(createdAt));
        this.updatedAt = new SimpleStringProperty(safe(updatedAt));
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
