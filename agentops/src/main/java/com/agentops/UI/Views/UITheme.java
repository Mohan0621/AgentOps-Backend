package com.agentops.UI.Views;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class UITheme {

    public static final String BG_DARK = "#12121c";
    public static final String BG_CARD = "#1a1a2e";
    public static final String BG_TOP = "#1a1a2e";
    public static final String ACCENT_PRIMARY = "#4361ee";
    public static final String ACCENT_CYAN = "#4cc9f0";
    public static final String ACCENT_PURPLE = "#7209b7";
    public static final String COLOR_DANGER = "#e63946";
    public static final String TEXT_MUTED = "#a0a0b0";
    public static final String TEXT_WHITE = "#ffffff";

    public static Label createStatCard(String title, String initialVal, String borderColor, HBox parent) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(16));
        card.setPrefWidth(180);
        card.setStyle("-fx-background-color: " + BG_CARD + "; -fx-background-radius: 8; -fx-border-color: " + borderColor + "; -fx-border-width: 0 0 3 0;");

        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: " + TEXT_MUTED + ";");

        Label lblVal = new Label(initialVal);
        lblVal.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_WHITE + ";");

        card.getChildren().addAll(lblTitle, lblVal);
        parent.getChildren().add(card);
        return lblVal;
    }
}

