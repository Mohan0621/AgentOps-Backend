package com.agentops.UI.Views;

import java.util.Locale;

import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Line;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

/**
 * IncidentIQ Unified Design System.
 * - Outer website surfaces follow serko.ai's light horizon aura & medium typography.
 * - Authenticated workspace follows Operate density (#FAFAFA ground, #FFFFFF cards, #EAEAEA hairlines).
 */
public class UITheme {

    public static final String FONT_FAMILY = "Plus Jakarta Sans";
    public static final String MONO_FAMILY = "Cascadia Mono";

    // ── Serko & Operate Canvas Palette ──────────────────────────
    public static final String BG_PRIMARY    = "#FFFFFF";
    public static final String BG_CANVAS     = "#FAFAFA";
    public static final String BG_SOFT       = "#F6F6F6";
    public static final String BG_MUTED      = "#F4F4F5";
    public static final String BG_ICE        = "#E8F3FE";
    public static final String BG_ICE_TOP    = "#E5F2FF";

    // Compatibility tokens
    public static final String BG_DARK       = BG_PRIMARY;
    public static final String BG_CARD       = BG_PRIMARY;
    public static final String BG_TOP        = BG_CANVAS;
    public static final String BG_INPUT      = BG_PRIMARY;
    public static final String BG_SURFACE    = BG_SOFT;
    public static final String BG_HOVER      = "#F4F4F5";

    // ── Ink & Text ──────────────────────────────────────────────
    public static final String INK_PRIMARY   = "#09090B";
    public static final String INK_SECONDARY = "#52525B";
    public static final String INK_MUTED     = "#71717A";
    public static final String INK_DIM       = "#A1A1AA";

    // Compatibility aliases
    public static final String TEXT_WHITE    = INK_PRIMARY;
    public static final String TEXT_MUTED    = INK_SECONDARY;
    public static final String TEXT_DIM      = INK_MUTED;

    // ── Serko Horizon Gradient Colors ───────────────────────────
    public static final String GRAD_BLUE     = "#1B98F2";
    public static final String GRAD_PURPLE   = "#CF69E0";
    public static final String GRAD_PINK     = "#F56293";
    public static final String GRAD_CORAL    = "#FF635C";
    public static final String GRAD_ORANGE   = "#FF8C51";

    public static final String ACCENT_PRIMARY = INK_PRIMARY;
    public static final String ACCENT_CYAN    = GRAD_BLUE;
    public static final String ACCENT_PURPLE  = GRAD_PURPLE;
    public static final String ACCENT_PINK    = GRAD_PINK;
    public static final String ACCENT_GREEN   = "#16A34A";

    // ── Semantic Status Palette ─────────────────────────────────
    public static final String POS_BG        = "#F0FDF4";
    public static final String POS_BORDER    = "#BBF7D0";
    public static final String POS_INK       = "#166534";
    public static final String POS_DOT       = "#16A34A";

    public static final String SERKO_POS_BG  = "#C0F2B5";
    public static final String SERKO_POS_INK = "#17510B";

    public static final String COLOR_DANGER  = "#DC2626";
    public static final String COLOR_WARNING = "#EA580C";
    public static final String COLOR_SUCCESS = POS_DOT;
    public static final String COLOR_INFO    = GRAD_BLUE;

    // ── Hairline Borders ────────────────────────────────────────
    public static final String WS_BORDER        = "#EAEAEA";
    public static final String WS_BORDER_STRONG = "#D4D4D8";
    public static final String BORDER_SUBTLE    = WS_BORDER;
    public static final String RADIUS_SM        = "8";
    public static final String RADIUS_MD        = "12";
    public static final String RADIUS_LG        = "16";

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
        return "-fx-background-color: #FFFFFF;" +
               "-fx-text-fill: " + INK_PRIMARY + ";" +
               "-fx-border-color: #E4E4E7;" +
               "-fx-border-radius: 8;" +
               "-fx-background-radius: 8;" +
               "-fx-border-width: 1;" +
               "-fx-padding: 9 12;" +
               "-fx-font-size: 13.5px;" +
               "-fx-prompt-text-fill: " + INK_DIM + ";";
    }

    public static String labelStyle() {
        return "-fx-text-fill: #3F3F46; -fx-font-size: 12.5px; -fx-font-weight: 500;";
    }

    public static String headingStyle() {
        return "-fx-text-fill: " + INK_PRIMARY + "; -fx-font-size: 22px; -fx-font-weight: 600;";
    }

    public static String subHeadingStyle() {
        return "-fx-text-fill: " + INK_SECONDARY + "; -fx-font-size: 13.5px; -fx-font-weight: 400;";
    }

    public static String primaryButtonStyle() {
        return "-fx-background-color: " + INK_PRIMARY + ";" +
               "-fx-text-fill: #FFFFFF;" +
               "-fx-font-weight: 500;" +
               "-fx-font-size: 13px;" +
               "-fx-padding: 8 16;" +
               "-fx-background-radius: 8;" +
               "-fx-cursor: hand;";
    }

    public static String ghostButtonStyle() {
        return "-fx-background-color: #FFFFFF;" +
               "-fx-text-fill: " + INK_PRIMARY + ";" +
               "-fx-font-weight: 500;" +
               "-fx-font-size: 13px;" +
               "-fx-padding: 7.5 14;" +
               "-fx-border-color: " + WS_BORDER_STRONG + ";" +
               "-fx-border-radius: 8;" +
               "-fx-background-radius: 8;" +
               "-fx-cursor: hand;";
    }

    public static String dangerButtonStyle() {
        return "-fx-background-color: #FEF2F2;" +
               "-fx-text-fill: #991B1B;" +
               "-fx-font-weight: 500;" +
               "-fx-font-size: 13px;" +
               "-fx-padding: 8 16;" +
               "-fx-background-radius: 8;" +
               "-fx-border-color: #FECACA;" +
               "-fx-border-radius: 8;" +
               "-fx-cursor: hand;";
    }

    public static String cardStyle() {
        return "-fx-background-color: #FFFFFF;" +
               "-fx-background-radius: 12;" +
               "-fx-border-color: " + WS_BORDER + ";" +
               "-fx-border-radius: 12;" +
               "-fx-border-width: 1;";
    }

    public static String softCardStyle() {
        return "-fx-background-color: " + BG_CANVAS + ";" +
               "-fx-background-radius: 8;" +
               "-fx-border-color: " + WS_BORDER + ";" +
               "-fx-border-radius: 8;" +
               "-fx-border-width: 1;";
    }

    public static String monoStyle(double size, String hexColor) {
        return "-fx-font-family: '" + MONO_FAMILY + "', 'Consolas', monospace; " +
               "-fx-font-size: " + size + "px; -fx-text-fill: " + hexColor + ";";
    }

    /** Soft elevation reserved strictly for public hero/floating cards, NOT static workspace panels. */
    public static DropShadow softCardShadow() {
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.06));
        shadow.setRadius(20);
        shadow.setOffsetY(6);
        return shadow;
    }

    /** Deeper hover elevation for interactive floating cards on public showcase surfaces. */
    public static DropShadow elevatedCardShadow() {
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.12));
        shadow.setRadius(28);
        shadow.setOffsetY(10);
        return shadow;
    }

    // ── Workspace Buttons (8px Operate geometry) ─────────────────

    public static Button wsPrimaryBtn(String text, Node icon) {
        Button btn = new Button(text);
        btn.getStyleClass().add("ws-btn-primary");
        if (icon != null) {
            btn.setGraphic(icon);
            btn.setContentDisplay(ContentDisplay.LEFT);
            btn.setGraphicTextGap(7);
        }
        return btn;
    }

    public static Button wsSecondaryBtn(String text, Node icon) {
        Button btn = new Button(text);
        btn.getStyleClass().add("ws-btn-secondary");
        if (icon != null) {
            btn.setGraphic(icon);
            btn.setContentDisplay(ContentDisplay.LEFT);
            btn.setGraphicTextGap(7);
        }
        return btn;
    }

    // ── Serko Brand & Public Landing Components ─────────────────

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

        Group scaledGroup = new Group(mark);
        StackPane pane = new StackPane(scaledGroup);
        pane.setPrefSize(size, size);
        pane.setMinSize(size, size);
        pane.setMaxSize(size, size);
        return pane;
    }

    /** Matches Serko's "serko.ai" lockup: clean dark "Incident" + gradient "IQ". */
    public static HBox brandWordmark(double fontSize, boolean lightOnDark) {
        HBox box = new HBox(0);
        box.setAlignment(Pos.CENTER_LEFT);

        Text lead = new Text("Incident");
        lead.setFont(Font.font(FONT_FAMILY, FontWeight.MEDIUM, fontSize));
        lead.setFill(lightOnDark ? Color.WHITE : Color.BLACK);

        Text dot = new Text(".");
        dot.setFont(Font.font(FONT_FAMILY, FontWeight.MEDIUM, fontSize));
        dot.setFill(Color.web(GRAD_ORANGE));

        Text iq = new Text("iq");
        iq.setFont(Font.font(FONT_FAMILY, FontWeight.MEDIUM, fontSize));
        iq.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.web(GRAD_ORANGE)),
            new Stop(0.45, Color.web(GRAD_PINK)),
            new Stop(0.75, Color.web(GRAD_PURPLE)),
            new Stop(1.0, Color.web(GRAD_BLUE))
        ));

        TextFlow tf = new TextFlow(lead, dot, iq);
        box.getChildren().add(tf);
        return box;
    }

    // ── Clean 16px SVG Stroke Icons (1.6px uniform stroke) ────────

    public static StackPane svgStrokeIcon(String svgPathData, double size, Color strokeColor, double strokeWidth) {
        SVGPath path = new SVGPath();
        path.setContent(svgPathData);
        path.setFill(Color.TRANSPARENT);
        path.setStroke(strokeColor);
        path.setStrokeWidth(strokeWidth);
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        path.setStrokeLineJoin(StrokeLineJoin.ROUND);

        double scale = size / 24.0;
        path.setScaleX(scale);
        path.setScaleY(scale);

        Group g = new Group(path);
        StackPane box = new StackPane(g);
        box.setPrefSize(size, size);
        box.setMinSize(size, size);
        box.setMaxSize(size, size);
        return box;
    }

    public static StackPane inboxIcon(double size, Color c) {
        return svgStrokeIcon("M4 4h16v16H4z M4 13h4l2 3h4l2-3h4", size, c, 1.75);
    }

    public static StackPane plusSquareIcon(double size, Color c) {
        return svgStrokeIcon("M12 7v10 M7 12h10 M4 4h16v16H4z", size, c, 1.75);
    }

    public static StackPane chartBarIcon(double size, Color c) {
        return svgStrokeIcon("M4 20h16 M7 16v-5 M12 16V7 M17 16v-8", size, c, 1.75);
    }

    public static StackPane refreshIcon(double size, Color c) {
        return svgStrokeIcon("M20 11a8.1 8.1 0 0 0-15.5-2m-.5-4v4h4 M4 13a8.1 8.1 0 0 0 15.5 2m.5 4v-4h-4", size, c, 1.75);
    }

    public static StackPane boltIcon(double size, Color c) {
        return svgStrokeIcon("M13 3L4 14h6l-1 7 9-11h-6z", size, c, 1.75);
    }

    public static StackPane layersIcon(double size, Color c) {
        return svgStrokeIcon("M12 3l9 4.5-9 4.5-9-4.5z M3 12l9 4.5 9-4.5 M3 16.5l9 4.5 9-4.5", size, c, 1.75);
    }

    public static StackPane clockIcon(double size, Color c) {
        return svgStrokeIcon("M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z M12 7v5l3 2", size, c, 1.75);
    }

    public static StackPane checkCircleIcon(double size, Color c) {
        return svgStrokeIcon("M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z M8.5 12.2l2.3 2.3 4.8-5", size, c, 1.75);
    }

    public static StackPane alertIcon(double size, Color c) {
        return svgStrokeIcon("M12 3L2 20h20z M12 9v5 M12 17.5h.01", size, c, 1.75);
    }

    public static StackPane arrowLeftIcon(double size, Color c) {
        return svgStrokeIcon("M19 12H5 M11 6l-6 6 6 6", size, c, 1.75);
    }

    public static StackPane bellIcon(double size, Color c) {
        return svgStrokeIcon("M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9 M10.3 20a2 2 0 0 0 3.4 0", size, c, 1.75);
    }

    public static StackPane terminalIcon(double size, Color c) {
        return svgStrokeIcon("M4 17l6-5-6-5 M12 19h8", size, c, 1.75);
    }

    public static StackPane shieldIcon(double size, Color c) {
        return svgStrokeIcon("M12 3l7 3v5c0 5-3.2 8.7-7 10-3.8-1.3-7-5-7-10V6z M9.5 12.2l2 2 3.5-4", size, c, 1.75);
    }

    public static StackPane copyIcon(double size, Color c) {
        return svgStrokeIcon("M9 9h11v11H9z M5 15H4V4h11v1", size, c, 1.75);
    }

    public static StackPane downloadIcon(double size, Color c) {
        return svgStrokeIcon("M12 4v11 M7 11l5 5 5-5 M5 20h14", size, c, 1.75);
    }

    public static StackPane editIcon(double size, Color c) {
        return svgStrokeIcon("M14 4l6 6L9 21H3v-6z M12 6l6 6", size, c, 1.75);
    }

    public static StackPane trashIcon(double size, Color c) {
        return svgStrokeIcon("M4 7h16 M9 7V4h6v3 M6 7l1 13h10l1-13 M10 11v6 M14 11v6", size, c, 1.75);
    }

    public static StackPane eyeIcon(double size, Color c) {
        return svgStrokeIcon("M2 12s3.8-7 10-7 10 7 10 7-3.8 7-10 7-10-7-10-7z M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z", size, c, 1.75);
    }

    public static StackPane eyeOffIcon(double size, Color c) {
        return svgStrokeIcon("M3 3l18 18 M10.6 10.6a2.5 2.5 0 0 0 2.8 2.8 M9.9 5.1A9.8 9.8 0 0 1 12 5c6.2 0 10 7 10 7a16.8 16.8 0 0 1-3.3 4.3 M6.6 6.6A17.1 17.1 0 0 0 2 12s3.8 7 10 7a9.7 9.7 0 0 0 4.4-1", size, c, 1.75);
    }

    public static StackPane searchIcon(double size, Color c) {
        return svgStrokeIcon("M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16z M21 21l-4.3-4.3", size, c, 1.75);
    }

    public static StackPane messageSquareIcon(double size, Color c) {
        return svgStrokeIcon("M4 5h16v11H8l-4 4z", size, c, 1.75);
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

    /** Clean Sentence-case eyebrow matching Serko's light "AI travel agent" and quiet workspace section leads. */
    public static Label eyebrowLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle(
            "-fx-text-fill: rgba(0, 0, 0, 0.56);" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: 500;"
        );
        return lbl;
    }

    public static Label wsSectionLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #52525B; -fx-font-size: 12px; -fx-font-weight: 500;");
        return lbl;
    }

    /**
     * Builds Serko-style multi-line display headline in Regular/Medium weight (500) with smooth inline accent gradient.
     */
    public static TextFlow gradientAccentFlow(String before, String accent, String after,
                                              double fontSize, TextAlignment align, boolean lightTheme) {
        TextFlow flow = new TextFlow();
        flow.setTextAlignment(align);
        flow.setLineSpacing(2);

        Color baseInk = lightTheme ? Color.BLACK : Color.WHITE;
        Font font = Font.font(FONT_FAMILY, FontWeight.MEDIUM, fontSize);

        if (before != null && !before.isEmpty()) {
            Text t1 = new Text(before);
            t1.setFont(font);
            t1.setFill(baseInk);
            flow.getChildren().add(t1);
        }
        if (accent != null && !accent.isEmpty()) {
            Text tAccent = new Text(accent);
            tAccent.setFont(font);
            tAccent.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web(GRAD_ORANGE)),
                new Stop(0.45, Color.web(GRAD_PINK)),
                new Stop(0.72, Color.web(GRAD_PURPLE)),
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
            btn.setContentDisplay(ContentDisplay.RIGHT);
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
            btn.setContentDisplay(ContentDisplay.RIGHT);
            btn.setGraphicTextGap(8);
            btn.setGraphic(chevronDownIcon(Color.BLACK, 0.72));
        }
        addPillHover(btn);
        return btn;
    }

    public static Button serkoTertiaryPill(String text, boolean withArrow) {
        Button btn = new Button(text);
        btn.getStyleClass().add("serko-btn-tertiary");
        if (withArrow) {
            btn.setContentDisplay(ContentDisplay.RIGHT);
            btn.setGraphicTextGap(7);
            btn.setGraphic(chevronRightIcon(Color.BLACK, 0.68));
        }
        addPillHover(btn);
        return btn;
    }

    public static Button serkoRainbowPill(String text, boolean withArrow) {
        return serkoTertiaryPill(text, withArrow);
    }

    public static void addPillHover(Button btn) {
        final Interpolator ease = Interpolator.SPLINE(0.16, 1.0, 0.3, 1.0);
        btn.setOnMouseEntered(e -> {
            Object prev = btn.getProperties().get("pillAnim");
            if (prev instanceof javafx.animation.Animation a) a.stop();

            ScaleTransition st = new ScaleTransition(Duration.millis(150), btn);
            st.setToX(1.028);
            st.setToY(1.028);
            st.setInterpolator(ease);

            ParallelTransition pt = new ParallelTransition(st);
            Node graphic = btn.getGraphic();
            if (graphic != null) {
                TranslateTransition gt = new TranslateTransition(Duration.millis(150), graphic);
                gt.setInterpolator(ease);
                if (btn.getStyleClass().contains("serko-btn-white")) {
                    gt.setToY(1.8);
                } else {
                    gt.setToX(2.6);
                }
                pt.getChildren().add(gt);
            }
            btn.getProperties().put("pillAnim", pt);
            pt.play();
        });
        btn.setOnMouseExited(e -> {
            Object prev = btn.getProperties().get("pillAnim");
            if (prev instanceof javafx.animation.Animation a) a.stop();

            ScaleTransition st = new ScaleTransition(Duration.millis(130), btn);
            st.setToX(1.0);
            st.setToY(1.0);
            st.setInterpolator(ease);

            ParallelTransition pt = new ParallelTransition(st);
            Node graphic = btn.getGraphic();
            if (graphic != null) {
                TranslateTransition gt = new TranslateTransition(Duration.millis(130), graphic);
                gt.setInterpolator(ease);
                gt.setToX(0);
                gt.setToY(0);
                pt.getChildren().add(gt);
            }
            btn.getProperties().put("pillAnim", pt);
            pt.play();
        });
    }

    /** Serko dotted trajectory divider (.u-line-divider-1). */
    public static Line dottedDivider(double width) {
        Line line = new Line(0, 0, width, 0);
        line.setStroke(Color.rgb(0, 0, 0, 0.25));
        line.setStrokeWidth(1.6);
        line.setStrokeLineCap(StrokeLineCap.ROUND);
        line.getStrokeDashArray().addAll(1.0, 10.0);
        return line;
    }

    /** Responsive Serko dotted trajectory divider that shrinks cleanly below maxWidth. */
    public static Region responsiveDottedDivider(double maxWidth) {
        Pane holder = new Pane();
        holder.setMinWidth(0);
        holder.setPrefWidth(maxWidth);
        holder.setMaxWidth(maxWidth);
        holder.setMinHeight(2);
        holder.setPrefHeight(2);
        holder.setMaxHeight(2);

        Line line = new Line(1.0, 1.0, 1.0, 1.0);
        line.setManaged(false);
        line.endXProperty().bind(Bindings.max(1.0, holder.widthProperty().subtract(1.0)));
        line.setStroke(Color.rgb(0, 0, 0, 0.25));
        line.setStrokeWidth(1.6);
        line.setStrokeLineCap(StrokeLineCap.ROUND);
        line.getStrokeDashArray().addAll(1.0, 10.0);

        holder.getChildren().add(line);
        return holder;
    }

    // ── Semantic Tags (4px Operate geometry, zero 999 pills in workspace) ──

    public static Label positiveBadge(String text) {
        Label badge = new Label(text);
        badge.setMnemonicParsing(false);
        badge.setMinWidth(Region.USE_PREF_SIZE);
        badge.setStyle(
            "-fx-background-color: " + POS_BG + ";" +
            "-fx-text-fill: " + POS_INK + ";" +
            "-fx-border-color: " + POS_BORDER + ";" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 4;" +
            "-fx-background-radius: 4;" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: 500;" +
            "-fx-padding: 2 7;"
        );
        return badge;
    }

    public static Label neutralTag(String text) {
        Label tag = new Label(text);
        tag.setMnemonicParsing(false);
        tag.setMinWidth(Region.USE_PREF_SIZE);
        tag.setStyle(
            "-fx-background-color: #F4F4F5;" +
            "-fx-text-fill: #3F3F46;" +
            "-fx-border-color: #E4E4E7;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 4;" +
            "-fx-background-radius: 4;" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: 500;" +
            "-fx-padding: 2 7;"
        );
        return tag;
    }

    public static Label severityBadge(String severity) {
        String raw = severity != null ? severity : "UNKNOWN";
        Label badge = new Label(capitalizeWord(raw));
        badge.setMnemonicParsing(false);
        badge.setMinWidth(Region.USE_PREF_SIZE);
        String bg, fg, br;
        switch (raw.toUpperCase(Locale.ROOT)) {
            case "CRITICAL" -> { bg = "#FEF2F2"; fg = "#991B1B"; br = "#FECACA"; }
            case "HIGH"     -> { bg = "#FFF7ED"; fg = "#9A3412"; br = "#FED7AA"; }
            case "MEDIUM"   -> { bg = "#EFF6FF"; fg = "#1D4ED8"; br = "#BFDBFE"; }
            case "LOW"      -> { bg = "#F0FDF4"; fg = "#166534"; br = "#BBF7D0"; }
            default         -> { bg = "#F4F4F5"; fg = "#3F3F46"; br = "#E4E4E7"; }
        }
        badge.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-text-fill: " + fg + ";" +
            "-fx-border-color: " + br + ";" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 4;" +
            "-fx-background-radius: 4;" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: 500;" +
            "-fx-padding: 2 7;"
        );
        return badge;
    }

    public static Label statusBadge(String status) {
        String raw = status != null ? status : "UNKNOWN";
        Label badge = new Label(capitalizeWord(raw));
        badge.setMnemonicParsing(false);
        badge.setMinWidth(Region.USE_PREF_SIZE);
        String bg, fg, br;
        switch (raw.toUpperCase(Locale.ROOT)) {
            case "OPEN"          -> { bg = "#FEF2F2"; fg = "#991B1B"; br = "#FECACA"; }
            case "INVESTIGATING" -> { bg = "#EFF6FF"; fg = "#1D4ED8"; br = "#BFDBFE"; }
            case "RESOLVED"      -> { bg = "#F0FDF4"; fg = "#166534"; br = "#BBF7D0"; }
            case "CLOSED"        -> { bg = "#F4F4F5"; fg = "#52525B"; br = "#E4E4E7"; }
            default              -> { bg = "#F4F4F5"; fg = "#3F3F46"; br = "#E4E4E7"; }
        }
        badge.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-text-fill: " + fg + ";" +
            "-fx-border-color: " + br + ";" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 4;" +
            "-fx-background-radius: 4;" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: 500;" +
            "-fx-padding: 2 7;"
        );
        return badge;
    }

    public static String capitalizeWord(String raw) {
        if (raw == null || raw.isBlank()) return "Unknown";
        String cleaned = raw.trim().replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1);
    }

    // ── Form & Section Helpers (Sentence-case, Zero Uppercase Transforms) ──

    public static VBox formField(String labelText, Node input) {
        Label lbl = new Label(labelText);
        lbl.setStyle(labelStyle());
        return new VBox(5, lbl, input);
    }

    public static VBox formFieldHint(String labelText, String hint, Node input) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.BASELINE_LEFT);
        Label lbl = new Label(labelText);
        lbl.setStyle(labelStyle());
        Label hintLbl = new Label(hint);
        hintLbl.setStyle("-fx-text-fill: " + INK_MUTED + "; -fx-font-size: 11.5px;");
        row.getChildren().addAll(lbl, hintLbl);
        return new VBox(5, row, input);
    }

    public static Label sectionDivider(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: " + INK_MUTED + "; -fx-font-size: 12px; -fx-font-weight: 500; -fx-padding: 6 0 0 0;");
        return lbl;
    }

    public static void makeHandCursor(Node n) {
        n.setCursor(Cursor.HAND);
    }
}
