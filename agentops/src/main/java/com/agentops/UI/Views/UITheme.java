package com.agentops.UI.Views;

import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Line;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

/**
 * IncidentIQ Unified Design System aligned with serko.ai's light editorial + horizon aura aesthetic.
 */
public class UITheme {

    public static final String FONT_FAMILY = "Plus Jakarta Sans";

    // ── Serko Swatch Palette ────────────────────────────────────
    public static final String BG_PRIMARY    = "#FFFFFF";
    public static final String BG_SOFT       = "#F6F6F6";
    public static final String BG_ICE        = "#E8F3FE";
    public static final String BG_ICE_TOP    = "#E5F2FF";

    // Compatibility tokens mapped cleanly to Light Editorial palette
    public static final String BG_DARK       = BG_PRIMARY;
    public static final String BG_CARD       = BG_PRIMARY;
    public static final String BG_TOP        = BG_PRIMARY;
    public static final String BG_INPUT      = BG_SOFT;
    public static final String BG_SURFACE    = BG_SOFT;
    public static final String BG_HOVER      = "#F3F4F6";

    // ── Ink & Text ──────────────────────────────────────────────
    public static final String INK_PRIMARY   = "#000000";
    public static final String INK_SECONDARY = "#4D4D4D";
    public static final String INK_MUTED     = "#71717A";
    public static final String INK_DIM       = "#9CA3AF";

    // Compatibility aliases
    public static final String TEXT_WHITE    = INK_PRIMARY;
    public static final String TEXT_MUTED    = INK_SECONDARY;
    public static final String TEXT_DIM      = INK_MUTED;

    // ── Horizon Multi-Stop Gradient Colors ──────────────────────
    public static final String GRAD_BLUE     = "#1B98F2";
    public static final String GRAD_PURPLE   = "#CF69E0";
    public static final String GRAD_PINK     = "#F56293";
    public static final String GRAD_CORAL    = "#FF635C";
    public static final String GRAD_ORANGE   = "#FF8C51";

    public static final String ACCENT_PRIMARY = INK_PRIMARY;
    public static final String ACCENT_CYAN    = GRAD_BLUE;
    public static final String ACCENT_PURPLE  = GRAD_PURPLE;
    public static final String ACCENT_PINK    = GRAD_PINK;
    public static final String ACCENT_GREEN   = "#158142";

    // ── Status & Semantic Swatches ──────────────────────────────
    public static final String POS_BG        = "#C0F2B5";
    public static final String POS_INK       = "#17510B";
    public static final String POS_DOT       = "#158142";
    public static final String COLOR_DANGER  = "#F13931";
    public static final String COLOR_WARNING = "#EA580C";
    public static final String COLOR_SUCCESS = POS_DOT;
    public static final String COLOR_INFO    = GRAD_BLUE;

    // ── Borders & Radii ─────────────────────────────────────────
    public static final String BORDER_SUBTLE = "#E5E7EB";
    public static final String RADIUS_SM     = "10";
    public static final String RADIUS_MD     = "16";
    public static final String RADIUS_LG     = "22";

    private static boolean fontsLoaded = false;

    public static void loadFonts() {
        if (fontsLoaded) return;
        fontsLoaded = true;
        String[] paths = {
            "/fonts/PlusJakartaSans-Regular.ttf",
            "/fonts/PlusJakartaSans-Medium.ttf",
            "/fonts/PlusJakartaSans-SemiBold.ttf",
            "/fonts/PlusJakartaSans-Bold.ttf"
        };
        for (String p : paths) {
            try (var in = UITheme.class.getResourceAsStream(p)) {
                if (in != null) {
                    Font.loadFont(in, 14);
                }
            } catch (Exception ignored) {}
        }
    }

    // ── Shared Style Strings ────────────────────────────────────

    public static String inputStyle() {
        return "-fx-background-color: " + BG_SOFT + ";" +
               "-fx-text-fill: " + INK_PRIMARY + ";" +
               "-fx-border-color: " + BORDER_SUBTLE + ";" +
               "-fx-border-radius: 12;" +
               "-fx-background-radius: 12;" +
               "-fx-padding: 10 14;" +
               "-fx-font-size: 13.5px;" +
               "-fx-prompt-text-fill: " + INK_DIM + ";";
    }

    public static String labelStyle() {
        return "-fx-text-fill: " + INK_SECONDARY + "; -fx-font-size: 11.5px; -fx-font-weight: bold;";
    }

    public static String headingStyle() {
        return "-fx-text-fill: " + INK_PRIMARY + "; -fx-font-size: 24px; -fx-font-weight: bold;";
    }

    public static String subHeadingStyle() {
        return "-fx-text-fill: " + INK_SECONDARY + "; -fx-font-size: 13.5px;";
    }

    public static String primaryButtonStyle() {
        return "-fx-background-color: " + INK_PRIMARY + ";" +
               "-fx-text-fill: #FFFFFF;" +
               "-fx-font-weight: bold;" +
               "-fx-font-size: 13.5px;" +
               "-fx-padding: 10 22;" +
               "-fx-background-radius: 999;" +
               "-fx-cursor: hand;";
    }

    public static String ghostButtonStyle() {
        return "-fx-background-color: #FFFFFF;" +
               "-fx-text-fill: " + INK_PRIMARY + ";" +
               "-fx-font-weight: 600;" +
               "-fx-font-size: 13.5px;" +
               "-fx-padding: 10 20;" +
               "-fx-border-color: " + BORDER_SUBTLE + ";" +
               "-fx-border-radius: 999;" +
               "-fx-background-radius: 999;" +
               "-fx-cursor: hand;";
    }

    public static String dangerButtonStyle() {
        return "-fx-background-color: #FEE2E2;" +
               "-fx-text-fill: #B91C1C;" +
               "-fx-font-weight: bold;" +
               "-fx-font-size: 13px;" +
               "-fx-padding: 10 22;" +
               "-fx-background-radius: 999;" +
               "-fx-border-color: #FECACA;" +
               "-fx-border-radius: 999;" +
               "-fx-cursor: hand;";
    }

    public static String aiButtonStyle() {
        return "-fx-background-color: " +
               "linear-gradient(to bottom left, " + GRAD_BLUE + ", " + GRAD_PURPLE + ", " + GRAD_PINK + ", " + GRAD_CORAL + ", " + GRAD_ORANGE + "), " +
               "#FFFFFF;" +
               "-fx-background-insets: 0, 2;" +
               "-fx-background-radius: 999, 999;" +
               "-fx-text-fill: " + INK_PRIMARY + ";" +
               "-fx-font-weight: bold;" +
               "-fx-font-size: 13.5px;" +
               "-fx-padding: 10 22;" +
               "-fx-cursor: hand;";
    }

    public static String cardStyle() {
        return "-fx-background-color: #FFFFFF;" +
               "-fx-background-radius: 18;" +
               "-fx-border-color: #EBEBEB;" +
               "-fx-border-radius: 18;" +
               "-fx-border-width: 1;";
    }

    public static String softCardStyle() {
        return "-fx-background-color: " + BG_SOFT + ";" +
               "-fx-background-radius: 20;" +
               "-fx-border-color: rgba(0,0,0,0.04);" +
               "-fx-border-radius: 20;" +
               "-fx-border-width: 1;";
    }

    public static String tableStyle() {
        return "-fx-background-color: #FFFFFF;" +
               "-fx-border-color: #EBEBEB;" +
               "-fx-border-radius: 16;" +
               "-fx-table-cell-border-color: transparent;";
    }

    public static DropShadow softCardShadow() {
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.06));
        shadow.setRadius(22);
        shadow.setOffsetY(6);
        return shadow;
    }

    // ── Serko Signature Components ──────────────────────────────

    /**
     * Exact Serko geometric emblem mark scaled dynamically.
     */
    public static StackPane brandLogoMark(double size, boolean lightOnDark) {
        SVGPath mark = new SVGPath();
        mark.setContent(
            "M27.42 21.21V15.79C27.42 9.40 22.22 4.21 15.81 4.21C9.41 4.21 4.21 9.40 4.21 15.79V21.21C4.21 27.60 9.41 32.79 15.81 32.79V36.99C7.22 36.99 0.22 30.14 0.01 21.61L0 21.21V15.79C0 7.08 7.08 0.01 15.81 0.01C24.55 0.01 31.63 7.08 31.63 15.79V21.21L31.62 21.61C31.41 30.14 24.41 36.99 15.81 36.99V32.79C22.22 32.79 27.42 27.60 27.42 21.21Z " +
            "M19.28 25.39C20.17 24.97 21.24 25.35 21.66 26.24C22.08 27.13 21.70 28.20 20.81 28.62C19.35 29.31 17.71 29.69 15.98 29.69C14.25 29.69 12.60 29.31 11.14 28.62C10.25 28.20 9.87 27.13 10.29 26.24C10.72 25.35 11.78 24.97 12.68 25.39C13.66 25.86 14.78 26.12 15.98 26.12C17.18 26.12 18.30 25.86 19.28 25.39Z"
        );
        mark.setFill(lightOnDark ? Color.WHITE : Color.BLACK);

        double scale = size / 37.0;
        mark.setScaleX(scale);
        mark.setScaleY(scale);

        javafx.scene.Group scaledGroup = new javafx.scene.Group(mark);
        StackPane pane = new StackPane(scaledGroup);
        pane.setPrefSize(size, size);
        pane.setMinSize(size, size);
        pane.setMaxSize(size, size);
        return pane;
    }

    public static SVGPath chevronRightIcon(Color color, double scale) {
        SVGPath p = new SVGPath();
        p.setContent("M7.64 15.63C7.98 16.01 8.57 16.04 8.96 15.70C10.33 14.46 11.42 13.48 12.16 12.65C12.88 11.84 13.44 10.99 13.44 10.00C13.44 9.01 12.88 8.16 12.16 7.35C11.42 6.52 10.33 5.54 8.96 4.30C8.58 3.96 7.98 3.99 7.64 4.37C7.29 4.76 7.32 5.35 7.71 5.70C9.11 6.96 10.11 7.86 10.76 8.59C11.42 9.34 11.56 9.74 11.56 10.00C11.56 10.26 11.42 10.66 10.76 11.41C10.11 12.14 9.11 13.04 7.70 14.30C7.32 14.65 7.29 15.24 7.64 15.63Z");
        p.setFill(color);
        p.setScaleX(scale);
        p.setScaleY(scale);
        return p;
    }

    public static SVGPath chevronDownIcon(Color color, double scale) {
        SVGPath svg = chevronRightIcon(color, scale);
        svg.setRotate(90);
        return svg;
    }

    public static SVGPath checkMarkIcon(Color color, double scale) {
        SVGPath p = new SVGPath();
        p.setContent("M2.68 6.68C3.18 7.18 5.68 10.18 7.68 8.18C9.94 5.91 11.68 2.68 11.68 2.68");
        p.setFill(Color.TRANSPARENT);
        p.setStroke(color);
        p.setStrokeWidth(2.4);
        p.setStrokeLineCap(StrokeLineCap.ROUND);
        p.setScaleX(scale);
        p.setScaleY(scale);
        return p;
    }

    public static Label eyebrowLabel(String text) {
        Label lbl = new Label(text.toUpperCase());
        lbl.setStyle(
            "-fx-text-fill: rgba(0, 0, 0, 0.50);" +
            "-fx-font-size: 11.5px;" +
            "-fx-font-weight: bold;" +
            "-fx-letter-spacing: 0.9px;"
        );
        return lbl;
    }

    /**
     * Builds Serko-style multi-line headline with an inline horizon-gradient accent phrase.
     */
    public static TextFlow gradientAccentFlow(String before, String accent, String after,
                                              double fontSize, TextAlignment align, boolean lightTheme) {
        TextFlow flow = new TextFlow();
        flow.setTextAlignment(align);
        flow.setLineSpacing(2);

        Color baseInk = lightTheme ? Color.BLACK : Color.WHITE;
        Font font = Font.font(FONT_FAMILY, FontWeight.SEMI_BOLD, fontSize);

        if (before != null && !before.isEmpty()) {
            Text t1 = new Text(before);
            t1.setFont(font);
            t1.setFill(baseInk);
            flow.getChildren().add(t1);
        }
        if (accent != null && !accent.isEmpty()) {
            Text tAccent = new Text(accent);
            tAccent.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, fontSize));
            tAccent.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web(GRAD_ORANGE)),
                new Stop(0.5, Color.web(GRAD_PINK)),
                new Stop(1.0, Color.web(GRAD_BLUE))
            ));
            flow.getChildren().add(tAccent);
        }
        if (after != null && !after.isEmpty()) {
            Text t2 = new Text(after);
            t2.setFont(font);
            t2.setFill(baseInk);
            flow.getChildren().add(t2);
        }
        return flow;
    }

    public static Button serkoBlackPill(String text, boolean withArrow) {
        Button btn = new Button(text);
        btn.getStyleClass().add("serko-btn-black");
        if (withArrow) {
            btn.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
            btn.setGraphicTextGap(8);
            btn.setGraphic(chevronRightIcon(Color.WHITE, 0.72));
        }
        addPillHover(btn);
        return btn;
    }

    public static Button serkoWhitePill(String text, boolean withDownArrow) {
        Button btn = new Button(text);
        btn.getStyleClass().add("serko-btn-white");
        if (withDownArrow) {
            btn.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
            btn.setGraphicTextGap(8);
            btn.setGraphic(chevronDownIcon(Color.BLACK, 0.72));
        }
        addPillHover(btn);
        return btn;
    }

    public static Button serkoRainbowPill(String text, boolean withArrow) {
        Button btn = new Button(text);
        btn.getStyleClass().add("serko-btn-rainbow");
        if (withArrow) {
            btn.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
            btn.setGraphicTextGap(8);
            btn.setGraphic(chevronRightIcon(Color.BLACK, 0.72));
        }
        addPillHover(btn);
        return btn;
    }

    public static void addPillHover(Button btn) {
        btn.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), btn);
            st.setToX(1.025);
            st.setToY(1.025);
            st.play();
        });
        btn.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), btn);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
    }

    /**
     * Serko dotted trajectory divider (.u-line-divider-1).
     */
    public static Line dottedDivider(double width) {
        Line line = new Line(0, 0, width, 0);
        line.setStroke(Color.rgb(0, 0, 0, 0.25));
        line.setStrokeWidth(1.6);
        line.setStrokeLineCap(StrokeLineCap.ROUND);
        line.getStrokeDashArray().addAll(1.0, 10.0);
        return line;
    }

    // ── Severity / Status / Positive Badges ─────────────────────

    public static Label positiveBadge(String text) {
        Label badge = new Label(text);
        badge.setStyle(
            "-fx-background-color: " + POS_BG + ";" +
            "-fx-text-fill: " + POS_INK + ";" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 4 11;" +
            "-fx-background-radius: 999;"
        );
        return badge;
    }

    public static Label severityBadge(String severity) {
        String label = severity != null ? severity : "UNKNOWN";
        Label badge = new Label(label);
        String bg, fg;
        switch (label.toUpperCase()) {
            case "CRITICAL" -> { bg = "#FEE2E2"; fg = "#B91C1C"; }
            case "HIGH"     -> { bg = "#FFEDD5"; fg = "#C2410C"; }
            case "MEDIUM"   -> { bg = BG_ICE;    fg = "#0369A1"; }
            case "LOW"      -> { bg = POS_BG;    fg = POS_INK;   }
            default         -> { bg = BG_SOFT;   fg = INK_SECONDARY; }
        }
        badge.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-text-fill: " + fg + ";" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 4 11;" +
            "-fx-background-radius: 999;"
        );
        return badge;
    }

    public static Label statusBadge(String status) {
        String label = status != null ? status : "UNKNOWN";
        Label badge = new Label(label);
        String bg, fg;
        switch (label.toUpperCase()) {
            case "OPEN"          -> { bg = "#FEE2E2"; fg = "#B91C1C"; }
            case "INVESTIGATING" -> { bg = BG_ICE;    fg = "#0369A1"; }
            case "RESOLVED"      -> { bg = POS_BG;    fg = POS_INK;   }
            case "CLOSED"        -> { bg = "#F3F4F6"; fg = "#4B5563"; }
            default              -> { bg = BG_SOFT;   fg = INK_SECONDARY; }
        }
        badge.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-text-fill: " + fg + ";" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 4 11;" +
            "-fx-background-radius: 999;"
        );
        return badge;
    }

    // ── Light Editorial KPI Stat Card ───────────────────────────

    public static Label createStatCard(String title, String initialVal, String accentColor, HBox parent) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(20, 22, 20, 22));
        card.setPrefWidth(200);
        card.setStyle(cardStyle());
        card.setEffect(softCardShadow());

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label dot = new Label("●");
        dot.setStyle("-fx-text-fill: " + accentColor + "; -fx-font-size: 9px;");
        Label lblTitle = new Label(title.toUpperCase());
        lblTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + INK_MUTED + "; -fx-letter-spacing: 0.6px;");
        topRow.getChildren().addAll(dot, lblTitle);

        Label lblVal = new Label(initialVal);
        lblVal.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: " + INK_PRIMARY + ";");

        card.getChildren().addAll(topRow, lblVal);
        parent.getChildren().add(card);
        return lblVal;
    }

    // ── Toast & Form Helpers ────────────────────────────────────

    public static Label toast(String message, boolean success) {
        Label lbl = new Label((success ? "✓  " : "✕  ") + message);
        String bg = success ? POS_BG : "#FEE2E2";
        String fg = success ? POS_INK : "#B91C1C";
        lbl.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-text-fill: " + fg + ";" +
            "-fx-font-size: 12.5px;" +
            "-fx-font-weight: 600;" +
            "-fx-padding: 10 16;" +
            "-fx-background-radius: 12;"
        );
        return lbl;
    }

    public static VBox formField(String labelText, Node input) {
        Label lbl = new Label(labelText.toUpperCase());
        lbl.setStyle(labelStyle());
        return new VBox(6, lbl, input);
    }

    public static VBox formFieldHint(String labelText, String hint, Node input) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.BASELINE_LEFT);
        Label lbl = new Label(labelText.toUpperCase());
        lbl.setStyle(labelStyle());
        Label hintLbl = new Label(hint);
        hintLbl.setStyle("-fx-text-fill: " + INK_DIM + "; -fx-font-size: 11px;");
        row.getChildren().addAll(lbl, hintLbl);
        return new VBox(6, row, input);
    }

    public static Label sectionDivider(String text) {
        Label lbl = new Label(text.toUpperCase());
        lbl.setStyle("-fx-text-fill: " + INK_MUTED + "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 8 0 0 0;");
        return lbl;
    }

    public static void makeHandCursor(Node n) {
        n.setCursor(Cursor.HAND);
    }
}
