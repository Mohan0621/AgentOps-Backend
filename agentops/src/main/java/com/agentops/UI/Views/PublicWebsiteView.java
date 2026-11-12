package com.agentops.UI.Views;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

/**
 * Full public-facing website shell matching https://www.serko.ai/ (Home, Platform, For teams, About).
 * Uses hybrid JS HTML5 Canvas (SerkoHeroGlobe) + pure JavaFX editorial UI components.
 */
public class PublicWebsiteView extends StackPane {

    public enum Page { HOME, PLATFORM, FOR_TEAMS, ABOUT }

    private final Consumer<Boolean> onOpenAuth; // true = Register, false = Login
    private final Runnable onOpenWorkspaceIfLoggedIn;
    private final ScrollPane scrollPane;
    private final VBox pageContainer;

    private StackPane navbarWrapper;
    private HBox navbar;
    private Button navPlatform, navTeams, navAbout;
    private Button navAuthLogin, navAuthStart;

    private static final Interpolator EASE_OUT = Interpolator.SPLINE(0.16, 1.0, 0.3, 1.0);

    private Page currentPage = Page.HOME;
    private boolean scrolledPastTop = false;
    private double faqAnchorFraction = 0.78;
    private Timeline heroCycleTimeline;
    private Timeline heroFloatTimeline;
    private Timeline livePulseTimeline;
    private Timeline typewriterTimeline;
    private TranslateTransition marqueeTransition;
    private SerkoHeroGlobe activeHeroGlobe;
    private final List<Region> revealBands = new ArrayList<>();

    public PublicWebsiteView(Consumer<Boolean> onOpenAuth, Runnable onOpenWorkspaceIfLoggedIn) {
        this.onOpenAuth = onOpenAuth;
        this.onOpenWorkspaceIfLoggedIn = onOpenWorkspaceIfLoggedIn;

        setStyle("-fx-background-color: #FFFFFF;");

        pageContainer = new VBox(0);
        pageContainer.setFillWidth(true);
        pageContainer.setStyle("-fx-background-color: #FFFFFF;");

        scrollPane = new ScrollPane(pageContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("scroll-pane");

        navbarWrapper = buildNavbar();
        StackPane.setAlignment(navbarWrapper, Pos.TOP_CENTER);

        getChildren().addAll(scrollPane, navbarWrapper);

        scrollPane.vvalueProperty().addListener((obs, oldV, newV) -> {
            double v = newV.doubleValue();
            boolean shouldElevate = v > 0.015;
            if (shouldElevate != scrolledPastTop) {
                scrolledPastTop = shouldElevate;
                updateNavbarBackdrop();
            }
            boolean heroOffscreen = v > 0.25;
            if (activeHeroGlobe != null) {
                activeHeroGlobe.setPaused(heroOffscreen);
            }
            setHeroAmbientRunning(!heroOffscreen);
            checkScrollReveals();
        });

        // Snappier wheel scrolling while preserving smooth trackpad feel
        scrollPane.addEventFilter(ScrollEvent.SCROLL, ev -> {
            if (ev.getDeltaY() != 0 && pageContainer.getHeight() > scrollPane.getViewportBounds().getHeight()) {
                double scrollable = pageContainer.getHeight() - scrollPane.getViewportBounds().getHeight();
                double mult = Math.abs(ev.getDeltaY()) >= 20 ? 2.4 : 1.35;
                double deltaV = (-ev.getDeltaY() * mult) / scrollable;
                double next = Math.max(0.0, Math.min(1.0, scrollPane.getVvalue() + deltaV));
                scrollPane.setVvalue(next);
                ev.consume();
            }
        });

        navigateTo(Page.HOME);
    }

    public void setSceneActive(boolean active) {
        boolean heroActive = active && scrollPane.getVvalue() <= 0.25;
        if (activeHeroGlobe != null) {
            activeHeroGlobe.setPaused(!heroActive);
        }
        setHeroAmbientRunning(heroActive);
        if (marqueeTransition != null) {
            if (active) marqueeTransition.play();
            else marqueeTransition.pause();
        }
        if (typewriterTimeline != null) {
            if (active) typewriterTimeline.play();
            else typewriterTimeline.pause();
        }
    }

    private void setHeroAmbientRunning(boolean running) {
        if (heroCycleTimeline != null) {
            if (running) heroCycleTimeline.play();
            else heroCycleTimeline.pause();
        }
        if (heroFloatTimeline != null) {
            if (running) heroFloatTimeline.play();
            else heroFloatTimeline.pause();
        }
        if (livePulseTimeline != null) {
            if (running) livePulseTimeline.play();
            else livePulseTimeline.pause();
        }
    }

    // ── Public Controls ─────────────────────────────────────────

    public void navigateTo(Page page) {
        stopTimers();
        revealBands.clear();
        this.currentPage = page;

        pageContainer.getChildren().clear();
        switch (page) {
            case HOME      -> buildHomePage();
            case PLATFORM  -> buildPlatformPage();
            case FOR_TEAMS -> buildForTeamsPage();
            case ABOUT     -> buildAboutPage();
        }

        updateActiveNav();
        updateNavbarBackdrop();

        Platform.runLater(() -> {
            scrollPane.setVvalue(0);
            scrolledPastTop = false;
            updateNavbarBackdrop();
            checkScrollReveals();
            FadeTransition ft = new FadeTransition(Duration.millis(240), pageContainer);
            ft.setFromValue(0.35);
            ft.setToValue(1.0);
            ft.play();
        });
    }

    public void refreshAuthButtons(boolean loggedIn) {
        if (loggedIn) {
            navAuthLogin.setText("Workspace");
            navAuthLogin.setOnAction(e -> onOpenWorkspaceIfLoggedIn.run());
            navAuthStart.setText("Open app");
            navAuthStart.setOnAction(e -> onOpenWorkspaceIfLoggedIn.run());
        } else {
            navAuthLogin.setText("Log in");
            navAuthLogin.setOnAction(e -> onOpenAuth.accept(false));
            navAuthStart.setText("Get started");
            navAuthStart.setOnAction(e -> onOpenAuth.accept(true));
        }
    }

    private void stopTimers() {
        if (heroCycleTimeline != null) heroCycleTimeline.stop();
        if (heroFloatTimeline != null) heroFloatTimeline.stop();
        if (livePulseTimeline != null) livePulseTimeline.stop();
        if (typewriterTimeline != null) typewriterTimeline.stop();
        if (marqueeTransition != null) marqueeTransition.stop();
        heroCycleTimeline = null;
        heroFloatTimeline = null;
        livePulseTimeline = null;
        typewriterTimeline = null;
        marqueeTransition = null;
        if (activeHeroGlobe != null) {
            activeHeroGlobe.setPaused(true);
            activeHeroGlobe = null;
        }
    }

    // ── Serko Floating Center-Capsule Navbar (.nav1_component) ──

    private StackPane buildNavbar() {
        StackPane wrap = new StackPane();
        wrap.setAlignment(Pos.TOP_CENTER);
        wrap.setPickOnBounds(false);
        wrap.setMaxHeight(78);

        HBox bar = new HBox(0);
        bar.setAlignment(Pos.CENTER);
        bar.setPrefHeight(54);
        bar.setMaxHeight(54);

        // Left: 3 crisp editorial links (matching live serko.ai: Platform, For teams, About)
        navPlatform = makeNavLink("Platform",  () -> navigateTo(Page.PLATFORM));
        navTeams    = makeNavLink("For teams", () -> navigateTo(Page.FOR_TEAMS));
        navAbout    = makeNavLink("About",     () -> navigateTo(Page.ABOUT));

        HBox leftLinks = new HBox(4, navPlatform, navTeams, navAbout);
        leftLinks.setAlignment(Pos.CENTER_LEFT);
        leftLinks.setMinWidth(260);

        // Center: Signature white pill logo ("Incident.iq")
        HBox brandPill = new HBox(0);
        brandPill.setAlignment(Pos.CENTER);
        brandPill.setPadding(new Insets(7, 18, 7, 18));
        String brandNormal = "-fx-background-color: #FFFFFF; -fx-background-radius: 10; -fx-border-color: rgba(0,0,0,0.04); -fx-border-radius: 10; -fx-border-width: 1;";
        String brandHover  = "-fx-background-color: #FFFFFF; -fx-background-radius: 10; -fx-border-color: rgba(0,0,0,0.12); -fx-border-radius: 10; -fx-border-width: 1;";
        brandPill.setStyle(brandNormal);
        DropShadow pillShadow = new DropShadow();
        pillShadow.setColor(Color.rgb(0, 0, 0, 0.05));
        pillShadow.setRadius(10);
        pillShadow.setOffsetY(2);
        brandPill.setEffect(pillShadow);
        brandPill.setCursor(Cursor.HAND);
        brandPill.setOnMouseClicked(e -> navigateTo(Page.HOME));
        brandPill.getChildren().add(UITheme.brandWordmark(16.5, false));
        installCardLift(brandPill, -1.5, 1.03, pillShadow, UITheme.elevatedCardShadow(), brandNormal, brandHover);

        // Right: Log in + Rainbow-ringed "Get started >" pill
        navAuthLogin = new Button("Log in");
        navAuthLogin.getStyleClass().add("serko-nav-link");
        navAuthLogin.setOnAction(e -> onOpenAuth.accept(false));

        navAuthStart = UITheme.serkoTertiaryPill("Get started", true);
        navAuthStart.setOnAction(e -> onOpenAuth.accept(true));

        HBox rightActions = new HBox(8, navAuthLogin, navAuthStart);
        rightActions.setAlignment(Pos.CENTER_RIGHT);
        rightActions.setMinWidth(260);

        Region s1 = new Region(); HBox.setHgrow(s1, Priority.ALWAYS);
        Region s2 = new Region(); HBox.setHgrow(s2, Priority.ALWAYS);

        bar.getChildren().addAll(leftLinks, s1, brandPill, s2, rightActions);
        this.navbar = bar;
        this.navbarWrapper = wrap;
        wrap.getChildren().add(bar);
        updateNavbarBackdrop();
        return wrap;
    }

    private Button makeNavLink(String text, Runnable action) {
        Button btn = new Button(text);
        btn.getStyleClass().add("serko-nav-link");
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private void updateActiveNav() {
        for (Button b : List.of(navPlatform, navTeams, navAbout)) {
            b.getStyleClass().remove("serko-nav-link-active");
        }
        Button active = switch (currentPage) {
            case PLATFORM  -> navPlatform;
            case FOR_TEAMS -> navTeams;
            case ABOUT     -> navAbout;
            default        -> null;
        };
        if (active != null && !active.getStyleClass().contains("serko-nav-link-active")) {
            active.getStyleClass().add("serko-nav-link-active");
        }
    }

    private void updateNavbarBackdrop() {
        if (navbar == null || navbarWrapper == null) return;
        if (scrolledPastTop) {
            navbarWrapper.setPadding(new Insets(12, 36, 0, 36));
            navbar.setMaxWidth(860);
            navbar.setPadding(new Insets(6, 14, 6, 14));
            navbar.setStyle(
                "-fx-background-color: #FFFFFF;" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: rgba(0, 0, 0, 0.08);" +
                "-fx-border-radius: 14;" +
                "-fx-border-width: 1;"
            );
            DropShadow ds = new DropShadow();
            ds.setColor(Color.rgb(0, 0, 0, 0.08));
            ds.setRadius(20);
            ds.setOffsetY(6);
            navbar.setEffect(ds);
        } else {
            navbarWrapper.setPadding(new Insets(10, 32, 0, 32));
            navbar.setMaxWidth(Double.MAX_VALUE);
            navbar.setPadding(new Insets(6, 12, 6, 12));
            navbar.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
            navbar.setEffect(null);
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  PAGE 1: HOME LANDING (Matches https://www.serko.ai/)
    // ═════════════════════════════════════════════════════════════

    private void buildHomePage() {
        faqAnchorFraction = 0.80;
        pageContainer.getChildren().addAll(
            buildHomeHeroSection(),
            buildTechMarqueeStrip(),
            withScrollReveal(buildFeature1IceSection()),
            withScrollReveal(buildFeature2DisruptionSection()),
            withScrollReveal(buildFeature3SmartPillsSection()),
            withScrollReveal(buildFeature4WorkspacePreviewSection()),
            withScrollReveal(buildClosingCtaSection("Get started", "Got an outage piling up?", "Get it sorted with ", "IncidentIQ.",
                "Skip manual stack-trace grepping. Paste your logs and let 6 autonomous agents isolate root cause and draft the fix.")),
            withScrollReveal(buildFaqSection(defaultHomeFaqs())),
            buildLightFooter()
        );
    }

    private StackPane buildHomeHeroSection() {
        StackPane hero = new StackPane();
        hero.setMinHeight(1120);
        hero.setPrefHeight(1140);
        hero.setAlignment(Pos.TOP_CENTER);

        // Background: JS HTML5 Canvas 3D Charcoal Stippled Globe + Sky Horizon Gradient
        SerkoHeroGlobe globeBackdrop = new SerkoHeroGlobe();
        this.activeHeroGlobe = globeBackdrop;

        // Above-the-fold Hero Typography + CTAs, followed cleanly BELOW the globe dome by the Profile Showcase Widget
        VBox content = new VBox(14);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(142, 28, 56, 28));
        content.setMaxWidth(1060);

        Label eyebrow = UITheme.eyebrowLabel("AI incident agent");
        TextFlow titleFlow = UITheme.gradientAccentFlow(
            "The last incident ", "agent", "\nyou’ll ever need.", 58, TextAlignment.CENTER, true
        );
        titleFlow.setMaxWidth(760);

        Label sub = new Label(
            "IncidentIQ learns how your systems fail, isolates root cause in seconds,\n" +
            "and orchestrates 6 autonomous agents from alert to resolution."
        );
        sub.setTextAlignment(TextAlignment.CENTER);
        sub.setWrapText(true);
        sub.setMinHeight(Region.USE_PREF_SIZE);
        sub.setMaxWidth(580);
        sub.setStyle("-fx-text-fill: rgba(0, 0, 0, 0.72); -fx-font-size: 16px; -fx-font-weight: 400; -fx-line-spacing: 3px;");

        Button btnStart = UITheme.serkoBlackPill("Get started", true);
        btnStart.setPadding(new Insets(11, 24, 11, 24));
        btnStart.setOnAction(e -> onOpenAuth.accept(true));

        Button btnFaq = UITheme.serkoWhitePill("FAQs", true);
        btnFaq.setPadding(new Insets(11, 22, 11, 22));
        btnFaq.setOnAction(e -> scrollPane.setVvalue(faqAnchorFraction));

        HBox ctaRow = new HBox(10, btnStart, btnFaq);
        ctaRow.setAlignment(Pos.CENTER);
        ctaRow.setPadding(new Insets(8, 0, 0, 0));

        // Clean breathing space over rising dotted dome before layered Production Watch widget
        Region domeSpacer = new Region();
        domeSpacer.setMinHeight(360);
        domeSpacer.setPrefHeight(372);

        StackPane layeredWidget = buildHeroInteractiveCard();
        content.getChildren().addAll(eyebrow, titleFlow, sub, ctaRow, domeSpacer, layeredWidget);

        hero.getChildren().addAll(globeBackdrop, content);

        // Subtle 3D globe cursor parallax across the hero surface without intercepting clicks
        hero.addEventFilter(MouseEvent.MOUSE_MOVED, ev -> {
            double w = hero.getWidth();
            double h = hero.getHeight();
            if (w > 0 && h > 0) {
                double nx = ((ev.getX() / w) - 0.5) * 2.0;
                double ny = ((ev.getY() / h) - 0.5) * 2.0;
                globeBackdrop.setPointer(nx, ny);
            }
        });
        hero.addEventFilter(MouseEvent.MOUSE_EXITED, ev -> globeBackdrop.resetPointer());

        return hero;
    }

    /**
     * Layered hero profile + non-overlapping floating confirmation chips matching serko.ai hero1_profile_wrap.
     */
    private StackPane buildHeroInteractiveCard() {
        StackPane stage = new StackPane();
        stage.setMaxWidth(960);
        stage.setMinHeight(196);
        stage.setAlignment(Pos.CENTER);

        // Structured 3-column staggered layout guarantees zero overlap at every viewport width >= 1040px
        HBox stageRow = new HBox(18);
        stageRow.setAlignment(Pos.CENTER);
        stageRow.setMaxWidth(940);

        // 1. Left floating service route confirmation chip
        String chipBaseStyle =
            "-fx-background-color: #FFFFFF; -fx-background-radius: 16;" +
            "-fx-border-color: rgba(0,0,0,0.06); -fx-border-radius: 16; -fx-border-width: 1;";
        String chipHoverBlue =
            "-fx-background-color: #FFFFFF; -fx-background-radius: 16;" +
            "-fx-border-color: rgba(27,152,242,0.42); -fx-border-radius: 16; -fx-border-width: 1;";
        String chipHoverGreen =
            "-fx-background-color: #FFFFFF; -fx-background-radius: 16;" +
            "-fx-border-color: rgba(22,163,74,0.40); -fx-border-radius: 16; -fx-border-width: 1;";

        VBox routeChip = new VBox(3);
        routeChip.setPrefWidth(208);
        routeChip.setMinSize(204, Region.USE_PREF_SIZE);
        routeChip.setMaxSize(214, Region.USE_PREF_SIZE);
        routeChip.setPadding(new Insets(12, 16, 12, 16));
        routeChip.setStyle(chipBaseStyle);
        DropShadow routeBaseShadow = UITheme.softCardShadow();
        routeChip.setEffect(routeBaseShadow);
        routeChip.setCursor(Cursor.HAND);

        Label routeTitleLbl = medLabel("CHECKOUT-API -> PATCH", 11.5, "#000000");
        routeTitleLbl.setWrapText(false);
        routeTitleLbl.setMinWidth(Region.USE_PREF_SIZE);
        HBox routeHead = new HBox(6, UITheme.terminalIcon(14, Color.BLACK), routeTitleLbl);
        routeHead.setAlignment(Pos.CENTER_LEFT);
        Label routeMeta = medLabel("NullPointer · line 418", 11.5, "#71717A");
        routeMeta.setWrapText(false);
        routeMeta.setMinWidth(Region.USE_PREF_SIZE);
        routeChip.getChildren().addAll(routeHead, routeMeta);

        VBox leftCol = new VBox(routeChip);
        leftCol.setAlignment(Pos.TOP_CENTER);
        leftCol.setPadding(new Insets(0, 0, 38, 0));

        // 2. Right floating verified deployment chip
        HBox confirmChip = new HBox(10);
        confirmChip.setPrefWidth(244);
        confirmChip.setMinSize(238, Region.USE_PREF_SIZE);
        confirmChip.setMaxSize(248, Region.USE_PREF_SIZE);
        confirmChip.setAlignment(Pos.CENTER_LEFT);
        confirmChip.setPadding(new Insets(12, 16, 12, 16));
        confirmChip.setStyle(chipBaseStyle);
        DropShadow confBaseShadow = UITheme.softCardShadow();
        confirmChip.setEffect(confBaseShadow);
        confirmChip.setCursor(Cursor.HAND);

        Label confTitle = medLabel("Checkout Guard", 12, "#000000");
        confTitle.setWrapText(false);
        confTitle.setMinWidth(Region.USE_PREF_SIZE);
        Label confSub = medLabel("Auto-verified PR #88", 11, "#71717A");
        confSub.setWrapText(false);
        confSub.setMinWidth(Region.USE_PREF_SIZE);
        VBox confTexts = new VBox(2, confTitle, confSub);
        Label confBadge = new Label("Confirmed");
        confBadge.setMinWidth(Region.USE_PREF_SIZE);
        confBadge.setStyle(
            "-fx-background-color: " + UITheme.SERKO_POS_BG + "; -fx-text-fill: " + UITheme.SERKO_POS_INK + ";" +
            "-fx-font-size: 11px; -fx-font-weight: 500; -fx-padding: 3 8; -fx-background-radius: 6;"
        );
        Region confSpacer = spacerRegion();
        confirmChip.getChildren().addAll(confTexts, confSpacer, confBadge);

        VBox rightCol = new VBox(confirmChip);
        rightCol.setAlignment(Pos.TOP_CENTER);
        rightCol.setPadding(new Insets(0, 0, 48, 0));

        // 3. Center foreground Serko layered profile widget (#F6F6F6 container)
        VBox shell = new VBox(12);
        shell.setPrefWidth(388);
        shell.setMinWidth(356);
        shell.setMaxWidth(408);
        shell.setMaxHeight(Region.USE_PREF_SIZE);
        shell.setPadding(new Insets(14));
        String shellBaseStyle =
            "-fx-background-color: #F6F6F6;" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: rgba(0, 0, 0, 0.06);" +
            "-fx-border-radius: 20;" +
            "-fx-border-width: 1;";
        String shellHoverStyle =
            "-fx-background-color: #F6F6F6;" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: rgba(27, 152, 242, 0.34);" +
            "-fx-border-radius: 20;" +
            "-fx-border-width: 1;";
        shell.setStyle(shellBaseStyle);
        DropShadow shellShadow = new DropShadow();
        shellShadow.setColor(Color.rgb(0, 0, 0, 0.10));
        shellShadow.setRadius(32);
        shellShadow.setOffsetY(12);
        shell.setEffect(shellShadow);

        // Top white prompt choice bar (hero1_profile_choice_wrap)
        HBox queryBar = new HBox(10);
        queryBar.setAlignment(Pos.CENTER_LEFT);
        queryBar.setPadding(new Insets(11, 14, 11, 14));
        String qBarNormal = "-fx-background-color: #FFFFFF; -fx-background-radius: 14; -fx-border-color: transparent; -fx-border-radius: 14; -fx-border-width: 1;";
        String qBarHover  = "-fx-background-color: #FFFFFF; -fx-background-radius: 14; -fx-border-color: rgba(0,0,0,0.10); -fx-border-radius: 14; -fx-border-width: 1;";
        queryBar.setStyle(qBarNormal);
        queryBar.setCursor(Cursor.HAND);
        Tooltip.install(queryBar, new Tooltip("Click to cycle live agent triage traces"));

        StackPane checkBadge = new StackPane(UITheme.checkMarkIcon(Color.WHITE, 0.75));
        checkBadge.setPrefSize(22, 22);
        checkBadge.setMinSize(22, 22);
        checkBadge.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #FF8C51, #F56293, #CF69E0);" +
            "-fx-background-radius: 7;"
        );

        Label lblActiveTrace = new Label("PaymentGateway NPE triaged in 1.4s");
        lblActiveTrace.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: #000000;");

        Region qSp = new Region(); HBox.setHgrow(qSp, Priority.ALWAYS);
        StackPane arrowIconNormal = UITheme.svgStrokeIcon("M12 19V5 M6 11l6-6 6 6", 13, Color.web("#52525B"), 1.8);
        StackPane arrowIconHover  = UITheme.svgStrokeIcon("M12 19V5 M6 11l6-6 6 6", 13, Color.WHITE, 1.8);
        StackPane arrowCircle = new StackPane(arrowIconNormal);
        arrowCircle.setPrefSize(26, 26);
        arrowCircle.setMinSize(26, 26);
        String arrNormal = "-fx-background-color: #F6F6F6; -fx-background-radius: 999; -fx-border-color: rgba(0,0,0,0.06); -fx-border-radius: 999;";
        String arrHover  = "-fx-background-color: #000000; -fx-background-radius: 999; -fx-border-color: #000000; -fx-border-radius: 999;";
        arrowCircle.setStyle(arrNormal);

        queryBar.getChildren().addAll(checkBadge, lblActiveTrace, qSp, arrowCircle);

        // Bottom service watch profile strip
        HBox profileRow = new HBox(12);
        profileRow.setAlignment(Pos.CENTER_LEFT);
        profileRow.setPadding(new Insets(2, 4, 2, 4));
        profileRow.setCursor(Cursor.HAND);
        profileRow.setOnMouseClicked(e -> onOpenAuth.accept(false));

        StackPane avatarBadge = new StackPane(UITheme.brandLogoMark(18, false));
        avatarBadge.setPrefSize(34, 34);
        avatarBadge.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8;");

        Circle pulseRing = new Circle(4, Color.TRANSPARENT);
        pulseRing.setStroke(Color.web(UITheme.POS_DOT, 0.70));
        pulseRing.setStrokeWidth(1.6);
        StackPane.setAlignment(pulseRing, Pos.TOP_RIGHT);
        StackPane.setMargin(pulseRing, new Insets(-2, -2, 0, 0));

        Circle greenDot = new Circle(4, Color.web(UITheme.POS_DOT));
        StackPane.setAlignment(greenDot, Pos.TOP_RIGHT);
        StackPane.setMargin(greenDot, new Insets(-2, -2, 0, 0));
        avatarBadge.getChildren().addAll(pulseRing, greenDot);

        Label profName = medLabel("Production Watch", 15, "#000000");
        profName.setWrapText(false);
        profName.setMinWidth(Region.USE_PREF_SIZE);
        Label livePill = new Label("6 agents live");
        livePill.setMinWidth(Region.USE_PREF_SIZE);
        livePill.setStyle(
            "-fx-background-color: #FFFFFF; -fx-text-fill: #52525B;" +
            "-fx-border-color: rgba(0,0,0,0.06); -fx-border-radius: 999; -fx-background-radius: 999;" +
            "-fx-font-size: 11px; -fx-font-weight: 500; -fx-padding: 2 8;"
        );
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        StackPane bell = UITheme.bellIcon(16, Color.web("#52525B"));

        profileRow.getChildren().addAll(avatarBadge, profName, livePill, sp, bell);
        shell.getChildren().addAll(queryBar, profileRow);

        VBox centerCol = new VBox(shell);
        centerCol.setAlignment(Pos.BOTTOM_CENTER);
        centerCol.setPadding(new Insets(28, 0, 0, 0));

        stageRow.getChildren().addAll(leftCol, centerCol, rightCol);
        stage.getChildren().add(stageRow);

        // Live pulsing beacon around green status dot
        livePulseTimeline = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(pulseRing.scaleXProperty(), 1.0),
                new KeyValue(pulseRing.scaleYProperty(), 1.0),
                new KeyValue(pulseRing.opacityProperty(), 0.75)
            ),
            new KeyFrame(Duration.seconds(1.6),
                new KeyValue(pulseRing.scaleXProperty(), 2.3, EASE_OUT),
                new KeyValue(pulseRing.scaleYProperty(), 2.3, EASE_OUT),
                new KeyValue(pulseRing.opacityProperty(), 0.0, EASE_OUT)
            ),
            new KeyFrame(Duration.seconds(2.2),
                new KeyValue(pulseRing.scaleXProperty(), 1.0),
                new KeyValue(pulseRing.scaleYProperty(), 1.0),
                new KeyValue(pulseRing.opacityProperty(), 0.0)
            )
        );
        livePulseTimeline.setCycleCount(Timeline.INDEFINITE);
        livePulseTimeline.play();

        // Gentle buoyant floating motion on outer flanking wrappers (never collides with card hover translateY)
        final boolean[] routeHovered = {false};
        final boolean[] confHovered  = {false};
        final double[] floatPhase = {0};
        heroFloatTimeline = new Timeline(new KeyFrame(Duration.millis(32), ev -> {
            floatPhase[0] += 0.045;
            if (!routeHovered[0]) {
                leftCol.setTranslateY(Math.sin(floatPhase[0]) * 3.5);
            }
            if (!confHovered[0]) {
                rightCol.setTranslateY(Math.sin(floatPhase[0] + 1.9) * 4.0);
            }
        }));
        heroFloatTimeline.setCycleCount(Timeline.INDEFINITE);
        heroFloatTimeline.play();

        // Tactile hover lift + elevated shadows across all 3 hero cards
        installCardLift(routeChip, -5.0, 1.022, routeBaseShadow, UITheme.elevatedCardShadow(), chipBaseStyle, chipHoverBlue,
            inside -> routeHovered[0] = inside);
        installCardLift(confirmChip, -5.0, 1.022, confBaseShadow, UITheme.elevatedCardShadow(), chipBaseStyle, chipHoverGreen,
            inside -> confHovered[0] = inside);
        installCardLift(shell, -4.0, 1.012, shellShadow, UITheme.elevatedCardShadow(), shellBaseStyle, shellHoverStyle, null);

        // Interactive hover on queryBar + bell wiggle
        queryBar.setOnMouseEntered(e -> {
            queryBar.setStyle(qBarHover);
            arrowCircle.setStyle(arrHover);
            arrowCircle.getChildren().setAll(arrowIconHover);
            animateScale(arrowCircle, 1.08, 130);
        });
        queryBar.setOnMouseExited(e -> {
            queryBar.setStyle(qBarNormal);
            arrowCircle.setStyle(arrNormal);
            arrowCircle.getChildren().setAll(arrowIconNormal);
            animateScale(arrowCircle, 1.0, 130);
        });
        profileRow.setOnMouseEntered(e -> {
            RotateTransition rt = new RotateTransition(Duration.millis(110), bell);
            rt.setFromAngle(-14);
            rt.setToAngle(14);
            rt.setAutoReverse(true);
            rt.setCycleCount(4);
            rt.setOnFinished(done -> bell.setRotate(0));
            rt.play();
        });

        // Synchronized 3-card live telemetry cycle
        String[][] states = {
            {"PaymentGateway NPE triaged in 1.4s",    "CHECKOUT-API -> PATCH", "NullPointer · line 418",  "Checkout Guard",   "Auto-verified PR #88", "Confirmed"},
            {"HikariPool timeout resolved in 1.8s",   "LEDGER-DAO -> CONFIG",  "PoolExhausted · 30s wait", "HikariPool Guard", "Pool limit tuned #91", "Verified"},
            {"JWT clock-skew race isolated in 0.9s",  "AUTH-FILTER -> GUARD",  "ExpiredJwt · 45ms drift", "JwtSkew Leeway",   "Leeway patch #94",     "Confirmed"}
        };
        final int[] idx = {0};
        Runnable advanceScenario = () -> {
            idx[0] = (idx[0] + 1) % states.length;
            String[] next = states[idx[0]];
            crossfadeLabelSwap(lblActiveTrace, next[0]);
            crossfadeLabelSwap(routeTitleLbl, next[1]);
            crossfadeLabelSwap(routeMeta, next[2]);
            crossfadeLabelSwap(confTitle, next[3]);
            crossfadeLabelSwap(confSub, next[4]);
            if (!next[5].equals(confBadge.getText())) {
                crossfadeLabelSwap(confBadge, next[5]);
            }
        };

        heroCycleTimeline = new Timeline(new KeyFrame(Duration.seconds(3.8), ev -> advanceScenario.run()));
        heroCycleTimeline.setCycleCount(Timeline.INDEFINITE);
        heroCycleTimeline.play();

        // Clicking query bar or route/confirm chips cycles live telemetry immediately
        queryBar.setOnMouseClicked(e -> {
            advanceScenario.run();
            heroCycleTimeline.playFromStart();
        });
        routeChip.setOnMouseClicked(e -> {
            advanceScenario.run();
            heroCycleTimeline.playFromStart();
        });
        confirmChip.setOnMouseClicked(e -> {
            advanceScenario.run();
            heroCycleTimeline.playFromStart();
        });

        return stage;
    }

    // ── Smooth Monochrome Stack Marquee Strip ───────────────────

    private StackPane buildTechMarqueeStrip() {
        StackPane wrap = new StackPane();
        wrap.setMinHeight(78);
        wrap.setPrefHeight(78);
        wrap.setMaxHeight(78);
        wrap.setStyle("-fx-background-color: #FFFFFF;");
        Rectangle clipRect = new Rectangle();
        clipRect.widthProperty().bind(wrap.widthProperty());
        clipRect.heightProperty().bind(wrap.heightProperty());
        wrap.setClip(clipRect);

        String[] items = {
            "Spring Boot", "Groq LPU", "Gemini AI", "6-Agent Core",
            "Spring Security", "Hibernate JPA", "MySQL", "JavaFX 21"
        };

        HBox track = new HBox(32);
        track.setAlignment(Pos.CENTER_LEFT);
        track.setPadding(new Insets(0));
        track.setMinHeight(78);
        track.setPrefHeight(78);

        String itemNormal = "-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: #A1A1AA; -fx-padding: 5 12; -fx-background-color: transparent; -fx-background-radius: 999;";
        String itemHover  = "-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: #09090B; -fx-padding: 5 12; -fx-background-color: #F4F4F5; -fx-background-radius: 999;";

        for (int pass = 0; pass < 3; pass++) {
            for (String item : items) {
                Label lbl = new Label(item);
                lbl.setMinWidth(Region.USE_PREF_SIZE);
                lbl.setStyle(itemNormal);
                lbl.setCursor(Cursor.HAND);
                lbl.setOnMouseEntered(e -> {
                    lbl.setStyle(itemHover);
                    animateScale(lbl, 1.05, 140);
                });
                lbl.setOnMouseExited(e -> {
                    lbl.setStyle(itemNormal);
                    animateScale(lbl, 1.0, 140);
                });
                track.getChildren().add(lbl);
            }
        }

        Pane trackHost = new Pane(track);
        trackHost.setMinWidth(0);
        trackHost.setPrefHeight(78);
        trackHost.setMaxWidth(Double.MAX_VALUE);

        Region leftFade = new Region();
        leftFade.setMaxWidth(120);
        leftFade.setPrefWidth(120);
        leftFade.setMouseTransparent(true);
        leftFade.setStyle("-fx-background-color: linear-gradient(to right, #FFFFFF 0%, rgba(255,255,255,0) 100%);");
        StackPane.setAlignment(leftFade, Pos.CENTER_LEFT);

        Region rightFade = new Region();
        rightFade.setMaxWidth(120);
        rightFade.setPrefWidth(120);
        rightFade.setMouseTransparent(true);
        rightFade.setStyle("-fx-background-color: linear-gradient(to left, #FFFFFF 0%, rgba(255,255,255,0) 100%);");
        StackPane.setAlignment(rightFade, Pos.CENTER_RIGHT);

        marqueeTransition = new TranslateTransition(Duration.seconds(28), track);
        marqueeTransition.setFromX(0);
        marqueeTransition.setToX(-1140);
        marqueeTransition.setCycleCount(Timeline.INDEFINITE);
        marqueeTransition.setInterpolator(Interpolator.LINEAR);
        marqueeTransition.play();

        track.widthProperty().addListener((obs, oldW, newW) -> {
            double w = newW.doubleValue();
            if (w > 200 && marqueeTransition != null) {
                double onePass = (w + 32.0) / 3.0;
                if (Math.abs(marqueeTransition.getToX() + onePass) > 4.0) {
                    marqueeTransition.stop();
                    marqueeTransition.setToX(-onePass);
                    marqueeTransition.playFromStart();
                }
            }
        });

        wrap.setOnMouseEntered(e -> {
            if (marqueeTransition != null) marqueeTransition.pause();
        });
        wrap.setOnMouseExited(e -> {
            if (marqueeTransition != null) marqueeTransition.play();
        });

        wrap.getChildren().addAll(trackHost, leftFade, rightFade);
        return wrap;
    }

    // ── Feature 1: Ice Sky Section (#E8F3FE, matches live-02) ──

    private VBox buildFeature1IceSection() {
        VBox band = new VBox();
        band.setAlignment(Pos.CENTER);
        band.setPadding(new Insets(96, 64, 96, 64));
        band.setStyle("-fx-background-color: " + UITheme.BG_ICE + ";");

        GridPane grid = twoColumnSectionGrid(52);

        // Left Copy
        VBox left = new VBox(18);
        left.setAlignment(Pos.CENTER_LEFT);

        Label eyebrow = UITheme.eyebrowLabel("Instant AI diagnosis");
        Label h2 = new Label("Debug like it's\nalready solved.\nBecause it is.");
        h2.setStyle("-fx-font-size: 40px; -fx-font-weight: 500; -fx-text-fill: #000000; -fx-line-spacing: -2px;");

        Label body = new Label(
            "Stop grepping through 800-line stack traces manually. IncidentIQ isolates failing classes, " +
            "identifies root cause, and drafts verified fixes automatically."
        );
        body.setWrapText(true);
        body.setStyle("-fx-text-fill: rgba(0,0,0,0.68); -fx-font-size: 15.5px; -fx-line-spacing: 4px;");

        VBox perks = new VBox(12);
        perks.setPadding(new Insets(10, 0, 0, 0));
        HBox perkQuick = featureHighlightItem(UITheme.terminalIcon(18, Color.BLACK), "Quick LLM triage", "Isolate failing component, error type, and fix steps in under 2 seconds.");
        HBox perkDeep  = featureHighlightItem(UITheme.shieldIcon(18, Color.BLACK), "6-Agent deep dive", "Full root cause report saved directly to your PostgreSQL / MySQL audit trail.");
        perks.getChildren().addAll(
            perkQuick,
            UITheme.responsiveDottedDivider(340),
            perkDeep
        );

        left.getChildren().addAll(eyebrow, h2, body, perks);

        // Right Showcase Visual Card (pink-coral sunset frame + crisp large white .aa-card matching live-02)
        StackPane pinkFrame = new StackPane();
        pinkFrame.setPadding(new Insets(42));
        pinkFrame.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #FFB2BE, #F489AD, #E388F2);" +
            "-fx-background-radius: 24;"
        );

        VBox aaCard = new VBox(16);
        aaCard.setPadding(new Insets(22));
        String aaNormal = "-fx-background-color: #FFFFFF; -fx-background-radius: 20; -fx-border-color: transparent; -fx-border-radius: 20; -fx-border-width: 1;";
        String aaHover  = "-fx-background-color: #FFFFFF; -fx-background-radius: 20; -fx-border-color: rgba(244,137,173,0.45); -fx-border-radius: 20; -fx-border-width: 1;";
        aaCard.setStyle(aaNormal);
        DropShadow aaShadow = UITheme.softCardShadow();
        aaCard.setEffect(aaShadow);
        installCardLift(aaCard, -4.0, 1.012, aaShadow, UITheme.elevatedCardShadow(), aaNormal, aaHover, null);

        // Top minimal header matching live-02
        HBox topIcons = new HBox(10);
        topIcons.setAlignment(Pos.CENTER_LEFT);
        topIcons.getChildren().addAll(UITheme.brandLogoMark(18, false));

        Label plusSymbol = new Label("+");
        plusSymbol.setStyle("-fx-font-size: 18px; -fx-font-weight: 400; -fx-text-fill: #52525B;");

        VBox midContent = new VBox(10);
        midContent.setPadding(new Insets(8, 0, 8, 0));
        Label tagLine = new Label("Incident #402 · PaymentGateway");
        tagLine.setStyle("-fx-font-size: 12px; -fx-font-weight: 500; -fx-text-fill: #71717A;");
        Label diagTxt = new Label("Deadlock in CheckoutService.processOrder(). Connection pool exhausted at line 142.");
        diagTxt.setWrapText(true);
        diagTxt.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: #000000; -fx-line-spacing: 3px;");

        Button runBtn = UITheme.serkoBlackPill("Run 6-Agent investigation", true);
        runBtn.setPadding(new Insets(9, 18, 9, 18));
        runBtn.setOnAction(e -> onOpenAuth.accept(false));
        midContent.getChildren().addAll(tagLine, diagTxt, runBtn);

        Region midSp = new Region();
        midSp.setMinHeight(34);
        VBox.setVgrow(midSp, Priority.ALWAYS);

        // Bottom composer row matching live-02 ('P' avatar + pill composer + send circle)
        HBox composerRow = new HBox(10);
        composerRow.setAlignment(Pos.CENTER_LEFT);

        StackPane avatar = new StackPane(medLabel("I", 12, "#312E81"));
        avatar.setPrefSize(30, 30);
        avatar.setStyle("-fx-background-color: #C7D2FE; -fx-background-radius: 999;");

        HBox composerPill = new HBox(8);
        composerPill.setAlignment(Pos.CENTER_LEFT);
        composerPill.setPadding(new Insets(6, 8, 6, 14));
        String compNormal = "-fx-background-color: #FFFFFF; -fx-background-radius: 999; -fx-border-color: #E4E4E7; -fx-border-radius: 999; -fx-border-width: 1;";
        String compHover  = "-fx-background-color: #FAFAFA; -fx-background-radius: 999; -fx-border-color: #18181B; -fx-border-radius: 999; -fx-border-width: 1;";
        composerPill.setStyle(compNormal);
        HBox.setHgrow(composerPill, Priority.ALWAYS);

        Label composerPrompt = new Label("Analyze logs");
        composerPrompt.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #18181B;");
        Region cpSp = spacerRegion();
        StackPane upIconIdle  = UITheme.svgStrokeIcon("M12 19V5 M6 11l6-6 6 6", 12, Color.web("#71717A"), 1.8);
        StackPane upIconHover = UITheme.svgStrokeIcon("M12 19V5 M6 11l6-6 6 6", 12, Color.WHITE, 1.8);
        StackPane upBtn = new StackPane(upIconIdle);
        upBtn.setPrefSize(24, 24);
        String upNormal = "-fx-background-color: #F4F4F5; -fx-background-radius: 999;";
        String upHover  = "-fx-background-color: #000000; -fx-background-radius: 999;";
        upBtn.setStyle(upNormal);
        composerPill.getChildren().addAll(composerPrompt, cpSp, upBtn);
        composerPill.setCursor(Cursor.HAND);
        composerPill.setOnMouseEntered(e -> {
            composerPill.setStyle(compHover);
            upBtn.setStyle(upHover);
            upBtn.getChildren().setAll(upIconHover);
            animateScale(upBtn, 1.08, 130);
        });
        composerPill.setOnMouseExited(e -> {
            composerPill.setStyle(compNormal);
            upBtn.setStyle(upNormal);
            upBtn.getChildren().setAll(upIconIdle);
            animateScale(upBtn, 1.0, 130);
        });
        composerPill.setOnMouseClicked(e -> onOpenAuth.accept(false));

        // Interactive left perks preview the corresponding investigation mode inside aaCard
        Runnable selectQuickMode = () -> {
            crossfadeLabelSwap(tagLine, "Incident #402 · Quick LLM triage (1.4s)");
            crossfadeLabelSwap(diagTxt, "NullPointerException in PaymentGateway.authorize() — missing null check at line 418.");
            runBtn.setText("Run Quick diagnosis");
        };
        Runnable selectDeepMode = () -> {
            crossfadeLabelSwap(tagLine, "Incident #402 · 6-Agent deep dive");
            crossfadeLabelSwap(diagTxt, "Deadlock in CheckoutService.processOrder(). Connection pool exhausted at line 142.");
            runBtn.setText("Run 6-Agent investigation");
        };
        perkQuick.setOnMouseClicked(e -> selectQuickMode.run());
        perkDeep.setOnMouseClicked(e -> selectDeepMode.run());

        composerRow.getChildren().addAll(avatar, composerPill);

        aaCard.getChildren().addAll(topIcons, plusSymbol, midContent, midSp, composerRow);
        pinkFrame.getChildren().add(aaCard);

        grid.addRow(0, left, pinkFrame);
        band.getChildren().add(grid);
        return band;
    }

    private HBox featureHighlightItem(Node icon, String title, String desc) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 10, 8, 10));
        String rowNormal = "-fx-background-color: transparent; -fx-background-radius: 14;";
        String rowHover  = "-fx-background-color: rgba(255,255,255,0.58); -fx-background-radius: 14;";
        row.setStyle(rowNormal);
        row.setCursor(Cursor.HAND);

        StackPane badge = new StackPane(icon);
        badge.setPrefSize(40, 40);
        badge.setMinSize(40, 40);
        String badgeNormal =
            "-fx-background-color: rgba(255,255,255,0.65);" +
            "-fx-border-color: #CFE6FC; -fx-border-width: 1.5;" +
            "-fx-border-radius: 10; -fx-background-radius: 10;";
        String badgeHover =
            "-fx-background-color: #FFFFFF;" +
            "-fx-border-color: #1B98F2; -fx-border-width: 1.5;" +
            "-fx-border-radius: 10; -fx-background-radius: 10;";
        badge.setStyle(badgeNormal);

        VBox texts = new VBox(2);
        Label t = medLabel(title, 15, "#000000");
        Label d = new Label(desc);
        d.setWrapText(true);
        d.setStyle("-fx-font-size: 13.5px; -fx-text-fill: rgba(0,0,0,0.65);");
        texts.getChildren().addAll(t, d);

        row.setOnMouseEntered(e -> {
            row.setStyle(rowHover);
            badge.setStyle(badgeHover);
            animateScale(badge, 1.06, 150);
            animateTranslateX(row, 4.0, 150);
        });
        row.setOnMouseExited(e -> {
            row.setStyle(rowNormal);
            badge.setStyle(badgeNormal);
            animateScale(badge, 1.0, 140);
            animateTranslateX(row, 0.0, 140);
        });

        row.getChildren().addAll(badge, texts);
        return row;
    }

    // ── Feature 2: Disruption Alternatives 5-Card Strip (live-03) ─

    private VBox buildFeature2DisruptionSection() {
        VBox band = new VBox(34);
        band.setAlignment(Pos.CENTER);
        band.setPadding(new Insets(96, 36, 96, 36));
        band.setStyle("-fx-background-color: #FFFFFF;");

        VBox header = new VBox(12);
        header.setAlignment(Pos.CENTER);
        header.setMaxWidth(680);

        Label eyebrow = UITheme.eyebrowLabel("Disruption management");
        Label h2 = new Label("With you all the way,\nfrom alert to deployed fix.");
        h2.setTextAlignment(TextAlignment.CENTER);
        h2.setStyle("-fx-font-size: 40px; -fx-font-weight: 500; -fx-text-fill: #000000;");

        Label sub = new Label(
            "Most monitoring tools leave engineers alone with raw alerts. IncidentIQ doesn't. " +
            "Production failing? It correlates logs and surfaces ranked remediation alternatives."
        );
        sub.setWrapText(true);
        sub.setTextAlignment(TextAlignment.CENTER);
        sub.setStyle("-fx-text-fill: rgba(0,0,0,0.68); -fx-font-size: 15.5px;");
        header.getChildren().addAll(eyebrow, h2, sub);

        // Rounded Horizon Card Frame (matching live-03-feature2.png sky->purple->coral backdrop)
        VBox panel = new VBox(32);
        panel.setMaxWidth(1000);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(44, 22, 44, 22));
        panel.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #DFECFE 0%, #96CBFA 42%, #E388F2 76%, #FF8C51 100%);" +
            "-fx-background-radius: 28;"
        );

        Label panelCaption = new Label("Always monitoring.\nAlways ready with alternatives.");
        panelCaption.setTextAlignment(TextAlignment.CENTER);
        panelCaption.setStyle("-fx-font-size: 20px; -fx-font-weight: 500; -fx-text-fill: #000000;");

        // 5 side-by-side vertical cards matching live-03 with interactive selection & hover lift
        HBox cardsRow = new HBox(10);
        cardsRow.setAlignment(Pos.CENTER);

        final List<VBox> fixCards = new java.util.ArrayList<>();
        final List<Button> fixButtons = new java.util.ArrayList<>();
        final int[] selectedIdx = {-1};

        String[][] fixData = {
            {"Fix option", "Pool Guard",      "2m rollout"},
            {"Fix option", "Retry Backoff",   "1m config"},
            {"Fix option", "Circuit Breaker", "3m patch"},
            {"Fix option", "Read Replica",    "Auto-route"}
        };

        String cardBase = "-fx-background-color: #FFFFFF; -fx-background-radius: 18; -fx-border-color: transparent; -fx-border-radius: 18; -fx-border-width: 1.5;";
        String cardHover = "-fx-background-color: #FFFFFF; -fx-background-radius: 18; -fx-border-color: rgba(27,152,242,0.45); -fx-border-radius: 18; -fx-border-width: 1.5;";
        String cardSelected = "-fx-background-color: #FFFFFF; -fx-background-radius: 18; -fx-border-color: #16A34A; -fx-border-radius: 18; -fx-border-width: 1.5;";

        String btnBlackNormal = "-fx-background-color: #000000; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: 500; -fx-padding: 8 10; -fx-background-radius: 999; -fx-cursor: hand;";
        String btnBlackHover  = "-fx-background-color: #27272A; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: 500; -fx-padding: 8 10; -fx-background-radius: 999; -fx-cursor: hand;";
        String btnSelStyle    = "-fx-background-color: " + UITheme.SERKO_POS_BG + "; -fx-text-fill: " + UITheme.SERKO_POS_INK + "; -fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 8 10; -fx-background-radius: 999; -fx-cursor: hand;";

        for (int i = 0; i < fixData.length; i++) {
            final int idx = i;
            VBox fc = disruptionCard(fixData[i][0], fixData[i][1], fixData[i][2], false);
            Button fb = (Button) fc.getChildren().get(fc.getChildren().size() - 1);
            DropShadow baseSh = UITheme.softCardShadow();
            fc.setStyle(cardBase);
            fc.setEffect(baseSh);
            fixCards.add(fc);
            fixButtons.add(fb);

            fc.setOnMouseEntered(e -> {
                fc.setEffect(UITheme.elevatedCardShadow());
                if (selectedIdx[0] != idx) fc.setStyle(cardHover);
                animateLiftAndScale(fc, -6.0, 1.02, 160);
            });
            fc.setOnMouseExited(e -> {
                fc.setEffect(baseSh);
                if (selectedIdx[0] != idx) {
                    fc.setStyle(cardBase);
                    animateLiftAndScale(fc, 0.0, 1.0, 150);
                } else {
                    fc.setStyle(cardSelected);
                    animateLiftAndScale(fc, -3.0, 1.01, 150);
                }
            });

            fb.setOnMouseEntered(e -> {
                if (selectedIdx[0] != idx) fb.setStyle(btnBlackHover);
                animateScale(fb, 1.03, 130);
            });
            fb.setOnMouseExited(e -> {
                if (selectedIdx[0] != idx) fb.setStyle(btnBlackNormal);
                animateScale(fb, 1.0, 130);
            });
            fb.setOnAction(e -> {
                selectedIdx[0] = (selectedIdx[0] == idx) ? -1 : idx;
                for (int k = 0; k < fixCards.size(); k++) {
                    boolean active = (k == selectedIdx[0]);
                    fixCards.get(k).setStyle(active ? cardSelected : cardBase);
                    fixButtons.get(k).setText(active ? "Selected" : "Select");
                    fixButtons.get(k).setStyle(active ? btnSelStyle : btnBlackNormal);
                    animateLiftAndScale(fixCards.get(k), active ? -3.0 : 0.0, active ? 1.01 : 1.0, 160);
                }
            });
        }

        VBox disruptedCard = disruptionCard("Incident #402", "Checkout API", "30s timeout", true);
        String disBase  = "linear-gradient(to bottom, #EBF3FE, #F3EAFF)";
        String disStyleNormal = "-fx-background-color: " + disBase + "; -fx-background-radius: 18; -fx-border-color: transparent; -fx-border-radius: 18; -fx-border-width: 1.5;";
        String disStyleHover  = "-fx-background-color: " + disBase + "; -fx-background-radius: 18; -fx-border-color: rgba(245,158,11,0.45); -fx-border-radius: 18; -fx-border-width: 1.5;";
        DropShadow disShadow = UITheme.softCardShadow();
        disruptedCard.setStyle(disStyleNormal);
        disruptedCard.setEffect(disShadow);
        installCardLift(disruptedCard, -6.0, 1.02, disShadow, UITheme.elevatedCardShadow(), disStyleNormal, disStyleHover, null);

        Button seeAltBtn = (Button) disruptedCard.getChildren().get(disruptedCard.getChildren().size() - 1);
        String altBtnNormal = "-fx-background-color: rgba(255,255,255,0.72); -fx-text-fill: #000000; -fx-font-size: 12px; -fx-font-weight: 500; -fx-padding: 8 10; -fx-background-radius: 999; -fx-cursor: hand;";
        String altBtnHover  = "-fx-background-color: #FFFFFF; -fx-text-fill: #000000; -fx-font-size: 12px; -fx-font-weight: 500; -fx-padding: 8 10; -fx-background-radius: 999; -fx-cursor: hand;";
        seeAltBtn.setStyle(altBtnNormal);
        seeAltBtn.setOnMouseEntered(e -> { seeAltBtn.setStyle(altBtnHover); animateScale(seeAltBtn, 1.03, 130); });
        seeAltBtn.setOnMouseExited(e -> { seeAltBtn.setStyle(altBtnNormal); animateScale(seeAltBtn, 1.0, 130); });
        seeAltBtn.setOnAction(e -> {
            // Staggered spotlight pulse across the 4 ranked fix options
            for (int i = 0; i < fixCards.size(); i++) {
                final int idx = i;
                VBox target = fixCards.get(idx);
                Timeline pulse = new Timeline(
                    new KeyFrame(Duration.millis(idx * 75.0)),
                    new KeyFrame(Duration.millis(idx * 75.0 + 140),
                        ev -> {
                            if (selectedIdx[0] != idx) target.setStyle(cardHover);
                            animateLiftAndScale(target, -9.0, 1.035, 140);
                        }),
                    new KeyFrame(Duration.millis(idx * 75.0 + 360),
                        ev -> {
                            boolean active = (selectedIdx[0] == idx);
                            target.setStyle(active ? cardSelected : cardBase);
                            animateLiftAndScale(target, active ? -3.0 : 0.0, active ? 1.01 : 1.0, 180);
                        })
                );
                pulse.play();
            }
        });

        cardsRow.getChildren().add(disruptedCard);
        cardsRow.getChildren().addAll(fixCards);

        panel.getChildren().addAll(panelCaption, cardsRow);
        band.getChildren().addAll(header, panel);
        return band;
    }

    private VBox disruptionCard(String subTxt, String titleTxt, String metaTxt, boolean isDisrupted) {
        VBox card = new VBox(12);
        card.setPrefWidth(178);
        card.setMinWidth(154);
        card.setMaxWidth(184);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setPadding(new Insets(14));
        card.setStyle(
            "-fx-background-color: " +
            (isDisrupted ? "linear-gradient(to bottom, #EBF3FE, #F3EAFF)" : "#FFFFFF") + ";" +
            "-fx-background-radius: 18;"
        );
        card.setEffect(UITheme.softCardShadow());

        // Top row: service mark + Disrupted tag on card 1
        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        StackPane miniIcon = isDisrupted
            ? UITheme.alertIcon(15, Color.web("#B45309"))
            : UITheme.boltIcon(15, Color.web("#1B98F2"));
        Region sp = spacerRegion();
        top.getChildren().addAll(miniIcon, sp);
        if (isDisrupted) {
            Label tag = new Label("Disrupted");
            tag.setStyle("-fx-background-color: #FDE68A; -fx-text-fill: #78350F; -fx-font-size: 10.5px; -fx-font-weight: 500; -fx-padding: 3 8; -fx-background-radius: 6;");
            top.getChildren().add(tag);
        }

        VBox head = new VBox(2);
        Label s = new Label(subTxt);
        s.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(0,0,0,0.56);");
        Label t = medLabel(titleTxt, 15, "#000000");
        head.getChildren().addAll(s, t);

        HBox metaRow = new HBox(6, UITheme.clockIcon(12, Color.web("#71717A")), new Label(metaTxt));
        metaRow.setAlignment(Pos.CENTER_LEFT);
        ((Label) metaRow.getChildren().get(1)).setStyle("-fx-font-size: 11px; -fx-text-fill: #71717A;");

        // Quiet skeleton placeholders matching live-03 card rhythm
        VBox skel = new VBox(5);
        Region sk1 = new Region(); sk1.setPrefHeight(10); sk1.setMaxWidth(115); sk1.setStyle("-fx-background-color: rgba(0,0,0,0.06); -fx-background-radius: 3;");
        Region sk2 = new Region(); sk2.setPrefHeight(10); sk2.setMaxWidth(90);  sk2.setStyle("-fx-background-color: rgba(0,0,0,0.06); -fx-background-radius: 3;");
        skel.getChildren().addAll(sk1, sk2);

        Button btn = new Button(isDisrupted ? "See alternatives" : "Select");
        btn.setMaxWidth(Double.MAX_VALUE);
        if (isDisrupted) {
            btn.setStyle("-fx-background-color: rgba(255,255,255,0.65); -fx-text-fill: #000000; -fx-font-size: 12px; -fx-font-weight: 500; -fx-padding: 8 10; -fx-background-radius: 999; -fx-cursor: hand;");
        } else {
            btn.setStyle("-fx-background-color: #000000; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: 500; -fx-padding: 8 10; -fx-background-radius: 999; -fx-cursor: hand;");
            btn.setOnAction(e -> {
                btn.setText("Selected");
                btn.setStyle("-fx-background-color: " + UITheme.SERKO_POS_BG + "; -fx-text-fill: " + UITheme.SERKO_POS_INK + "; -fx-font-size: 12px; -fx-font-weight: 500; -fx-padding: 8 10; -fx-background-radius: 999;");
            });
        }

        card.getChildren().addAll(top, head, metaRow, skel, btn);
        return card;
    }

    // ── Feature 3: Frosted Glass Overlay Artwork (live-04) ──────

    private VBox buildFeature3SmartPillsSection() {
        VBox band = new VBox();
        band.setAlignment(Pos.CENTER);
        band.setPadding(new Insets(96, 64, 96, 64));
        band.setStyle("-fx-background-color: #FFFFFF;");

        GridPane grid = twoColumnSectionGrid(52);

        VBox left = new VBox(18);
        left.setAlignment(Pos.CENTER_LEFT);
        Label eyebrow = UITheme.eyebrowLabel("Personalized engineering AI");
        Label h2 = new Label("Every investigation,\na little more intelligent.");
        h2.setStyle("-fx-font-size: 40px; -fx-font-weight: 500; -fx-text-fill: #000000;");
        Label body = new Label(
            "The more incidents your team resolves, the clearer your operational telemetry becomes — " +
            "capturing recurring failure patterns, risky services, and exact fixes."
        );
        body.setWrapText(true);
        body.setStyle("-fx-text-fill: rgba(0,0,0,0.68); -fx-font-size: 15.5px; -fx-line-spacing: 4px;");

        Button btnLearn = UITheme.serkoBlackPill("Learn more about IncidentIQ", true);
        btnLearn.setOnAction(e -> navigateTo(Page.PLATFORM));
        left.getChildren().addAll(eyebrow, h2, body, btnLearn);

        // Right Atmospheric Sky Visual Box with 4 cascading translucent white strips (matches live-04-feature3.png)
        StackPane artwork = new StackPane();
        artwork.setMinHeight(390);
        artwork.setPadding(new Insets(28));
        artwork.setStyle(
            "-fx-background-color: " +
            "radial-gradient(center 25% 30%, radius 70%, #96CBFA 0%, #5BB1F7 40%, #1B98F2 100%);" +
            "-fx-background-radius: 24;"
        );

        // Subtle warm sunset glow accent on right corner (echoing red coat warmth in Serko live-04)
        Region warmGlow = new Region();
        warmGlow.setMaxSize(280, 260);
        warmGlow.setStyle(
            "-fx-background-color: radial-gradient(center 75% 70%, radius 75%, rgba(255,99,92,0.55) 0%, rgba(244,137,173,0.28) 50%, transparent 100%);" +
            "-fx-background-radius: 24;"
        );
        StackPane.setAlignment(warmGlow, Pos.BOTTOM_RIGHT);

        VBox frostedStack = new VBox(10);
        frostedStack.setMaxWidth(340);
        frostedStack.setAlignment(Pos.BOTTOM_RIGHT);
        StackPane.setAlignment(frostedStack, Pos.BOTTOM_RIGHT);

        String[] items = {
            "Groq sub-2s inference",
            "Zero manual grepping",
            "6-agent root cause",
            "Built-in MTTR metrics"
        };
        double[] alphas = {0.94, 0.84, 0.72, 0.58};
        for (int i = 0; i < items.length; i++) {
            HBox strip = new HBox(12);
            strip.setAlignment(Pos.CENTER_LEFT);
            strip.setPadding(new Insets(14, 18, 14, 16));
            String stripNormal =
                "-fx-background-color: rgba(255, 255, 255, " + alphas[i] + ");" +
                "-fx-background-radius: 14;";
            String stripHover =
                "-fx-background-color: #FFFFFF;" +
                "-fx-background-radius: 14;";
            strip.setStyle(stripNormal);
            strip.setCursor(Cursor.HAND);

            StackPane coralDot = new StackPane(UITheme.checkMarkIcon(Color.WHITE, 0.72));
            coralDot.setPrefSize(22, 22);
            coralDot.setMinSize(22, 22);
            coralDot.setStyle("-fx-background-color: #FF897F; -fx-background-radius: 999;");

            Label lbl = medLabel(items[i], 14, "#000000");
            strip.getChildren().addAll(coralDot, lbl);

            DropShadow stripHoverShadow = UITheme.softCardShadow();
            strip.setOnMouseEntered(e -> {
                strip.setStyle(stripHover);
                strip.setEffect(stripHoverShadow);
                animateTranslateX(strip, -8.0, 160);
                animateScale(coralDot, 1.15, 160);
            });
            strip.setOnMouseExited(e -> {
                strip.setStyle(stripNormal);
                strip.setEffect(null);
                animateTranslateX(strip, 0.0, 150);
                animateScale(coralDot, 1.0, 150);
            });

            frostedStack.getChildren().add(strip);
        }

        artwork.getChildren().addAll(warmGlow, frostedStack);
        grid.addRow(0, left, artwork);
        band.getChildren().add(grid);
        return band;
    }

    // ── Feature 4: "Works for your team." Flush Bottom Preview (live-05) ─

    private VBox buildFeature4WorkspacePreviewSection() {
        VBox band = new VBox(36);
        band.setAlignment(Pos.CENTER);
        band.setPadding(new Insets(92, 56, 0, 56));
        band.getStyleClass().add("serko-feature4-bg");

        VBox header = new VBox(12);
        header.setAlignment(Pos.CENTER);
        header.setMaxWidth(640);

        Label h2 = new Label("Works for your team.");
        h2.setTextAlignment(TextAlignment.CENTER);
        h2.setStyle("-fx-font-size: 42px; -fx-font-weight: 500; -fx-text-fill: #000000;");

        Label sub = new Label(
            "Once connected, your team can track every incident, inspect AI diagnoses in real time, " +
            "and surface root cause across services. All in one place."
        );
        sub.setWrapText(true);
        sub.setTextAlignment(TextAlignment.CENTER);
        sub.setStyle("-fx-text-fill: rgba(0,0,0,0.68); -fx-font-size: 15.5px;");

        Button btnTeams = UITheme.serkoBlackPill("IncidentIQ for teams", true);
        btnTeams.setOnAction(e -> navigateTo(Page.FOR_TEAMS));
        header.getChildren().addAll(h2, sub, btnTeams);

        // Flush bottom preview frame replicating Serko live-05 app chrome
        HBox frame = new HBox(0);
        frame.setMaxWidth(960);
        frame.setMinHeight(380);
        frame.setStyle(
            "-fx-background-color: #FFFFFF;" +
            "-fx-background-radius: 18 18 0 0;" +
            "-fx-border-color: rgba(0,0,0,0.08);" +
            "-fx-border-width: 1 1 0 1;" +
            "-fx-border-radius: 18 18 0 0;"
        );
        DropShadow ds = new DropShadow();
        ds.setColor(Color.rgb(0, 0, 0, 0.12));
        ds.setRadius(24);
        ds.setOffsetY(4);
        frame.setEffect(ds);

        // Left white sidebar inside preview
        VBox miniRail = new VBox(14);
        miniRail.setPrefWidth(204);
        miniRail.setPadding(new Insets(18, 14, 18, 14));
        miniRail.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 18 0 0 0;");

        HBox railTop = new HBox(UITheme.brandLogoMark(16, false), spacerRegion(), UITheme.inboxIcon(14, Color.web("#71717A")));
        Label newReport = medLabel("+  New incident", 12, "#000000");
        newReport.setPadding(new Insets(5, 8, 5, 8));
        String newRepNormal = "-fx-font-size: 12px; -fx-font-weight: 500; -fx-text-fill: #000000; -fx-background-color: transparent; -fx-background-radius: 8;";
        String newRepHover  = "-fx-font-size: 12px; -fx-font-weight: 500; -fx-text-fill: #000000; -fx-background-color: #F4F4F5; -fx-background-radius: 8;";
        newReport.setStyle(newRepNormal);
        newReport.setMaxWidth(Double.MAX_VALUE);
        newReport.setCursor(Cursor.HAND);
        newReport.setOnMouseEntered(e -> newReport.setStyle(newRepHover));
        newReport.setOnMouseExited(e -> newReport.setStyle(newRepNormal));
        newReport.setOnMouseClicked(e -> onOpenAuth.accept(false));

        VBox recents = new VBox(4);
        Label recHead = new Label("Recents");
        recHead.setStyle("-fx-font-size: 11px; -fx-text-fill: #A1A1AA; -fx-padding: 0 8;");
        String[] recentNames = {"PaymentGateway NPE", "HikariPool timeout", "JWT token refresh race"};
        String[] bannerMsgs  = {
            "Inspect Incident #402 · PaymentGateway NPE",
            "Inspect Incident #398 · HikariPool timeout",
            "Inspect Incident #391 · JWT token refresh race"
        };

        // Main pastel sky/violet canvas inside preview matching live-05
        VBox innerCanvas = new VBox(16);
        innerCanvas.setAlignment(Pos.CENTER);
        innerCanvas.setPadding(new Insets(36, 40, 24, 40));
        HBox.setHgrow(innerCanvas, Priority.ALWAYS);
        innerCanvas.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #EBF4FE, #F5ECFF, #DCEBFF);" +
            "-fx-background-radius: 12 16 0 0;"
        );

        Label greet = new Label("Operations Workbench");
        greet.setStyle("-fx-font-size: 22px; -fx-font-weight: 500; -fx-text-fill: #000000;");
        Label subGreet = new Label("Paste a stack trace or inspect active alerts");
        subGreet.setStyle("-fx-font-size: 12.5px; -fx-text-fill: rgba(0,0,0,0.60);");

        Label bannerText = medLabel(bannerMsgs[0], 12, "#18181B");
        HBox banner = new HBox(8, UITheme.boltIcon(14, Color.web("#1B98F2")), bannerText, spacerRegion());
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setMaxWidth(420);
        banner.setPadding(new Insets(9, 14, 9, 14));
        String bannerNormal = "-fx-background-color: rgba(255,255,255,0.72); -fx-background-radius: 10; -fx-border-color: transparent; -fx-border-radius: 10; -fx-border-width: 1;";
        String bannerHover  = "-fx-background-color: #FFFFFF; -fx-background-radius: 10; -fx-border-color: rgba(27,152,242,0.35); -fx-border-radius: 10; -fx-border-width: 1;";
        banner.setStyle(bannerNormal);
        banner.setCursor(Cursor.HAND);
        banner.setOnMouseEntered(e -> { banner.setStyle(bannerHover); animateScale(banner, 1.015, 140); });
        banner.setOnMouseExited(e -> { banner.setStyle(bannerNormal); animateScale(banner, 1.0, 140); });
        banner.setOnMouseClicked(e -> onOpenAuth.accept(false));

        recents.getChildren().add(recHead);
        final int[] activeRecent = {0};
        final List<Label> recentRows = new java.util.ArrayList<>();
        String recNormal = "-fx-font-size: 11.5px; -fx-text-fill: #52525B; -fx-padding: 5 8; -fx-background-color: transparent; -fx-background-radius: 7;";
        String recHover  = "-fx-font-size: 11.5px; -fx-text-fill: #09090B; -fx-padding: 5 8; -fx-background-color: #F4F4F5; -fx-background-radius: 7;";
        String recActive = "-fx-font-size: 11.5px; -fx-font-weight: 500; -fx-text-fill: #09090B; -fx-padding: 5 8; -fx-background-color: #EFF6FF; -fx-background-radius: 7;";

        for (int i = 0; i < recentNames.length; i++) {
            final int idx = i;
            Label r = new Label(recentNames[i]);
            r.setMaxWidth(Double.MAX_VALUE);
            r.setCursor(Cursor.HAND);
            r.setStyle(i == 0 ? recActive : recNormal);
            recentRows.add(r);
            r.setOnMouseEntered(e -> {
                if (activeRecent[0] != idx) r.setStyle(recHover);
            });
            r.setOnMouseExited(e -> {
                r.setStyle(activeRecent[0] == idx ? recActive : recNormal);
            });
            r.setOnMouseClicked(e -> {
                activeRecent[0] = idx;
                for (int k = 0; k < recentRows.size(); k++) {
                    recentRows.get(k).setStyle(k == idx ? recActive : recNormal);
                }
                crossfadeLabelSwap(bannerText, bannerMsgs[idx]);
            });
            recents.getChildren().add(r);
        }
        miniRail.getChildren().addAll(railTop, newReport, recents);

        VBox promptBox = new VBox(18);
        promptBox.setMaxWidth(450);
        promptBox.setPadding(new Insets(16));
        String promptNormal = "-fx-background-color: #FFFFFF; -fx-background-radius: 14; -fx-border-color: transparent; -fx-border-radius: 14; -fx-border-width: 1;";
        String promptHover  = "-fx-background-color: #FFFFFF; -fx-background-radius: 14; -fx-border-color: rgba(0,0,0,0.12); -fx-border-radius: 14; -fx-border-width: 1;";
        promptBox.setStyle(promptNormal);
        promptBox.setCursor(Cursor.HAND);
        installCardLift(promptBox, -3.0, 1.01, null, UITheme.softCardShadow(), promptNormal, promptHover, null);
        promptBox.setOnMouseClicked(e -> onOpenAuth.accept(false));
        Label promptTxt = new Label("Ask anything or open real workspace...");
        promptTxt.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #71717A;");
        Button openAppBtn = UITheme.serkoBlackPill("Open app", false);
        openAppBtn.setOnAction(e -> onOpenAuth.accept(false));
        HBox promptBottom = new HBox(new Label("+"), spacerRegion(), openAppBtn);
        promptBottom.setAlignment(Pos.CENTER_LEFT);
        promptBox.getChildren().addAll(promptTxt, promptBottom);

        innerCanvas.getChildren().addAll(greet, subGreet, banner, promptBox);
        frame.getChildren().addAll(miniRail, innerCanvas);

        band.getChildren().addAll(header, frame);
        return band;
    }

    // ── Bottom Horizon CTA Banner (Matches cta1_wrap) ───────────

    private VBox buildClosingCtaSection(String eyebrowTxt, String line1, String line2Before, String accentWord, String subTxt) {
        VBox cta = new VBox(14);
        cta.setAlignment(Pos.CENTER);
        cta.setPadding(new Insets(96, 48, 96, 48));
        cta.getStyleClass().add("serko-cta-bg");

        Label eyebrow = UITheme.eyebrowLabel(eyebrowTxt);
        TextFlow headline = UITheme.gradientAccentFlow(line1 + "\n" + line2Before, accentWord, "", 42, TextAlignment.CENTER, true);
        headline.setMaxWidth(760);

        Label sub = new Label(subTxt);
        sub.setWrapText(true);
        sub.setTextAlignment(TextAlignment.CENTER);
        sub.setMaxWidth(580);
        sub.setStyle("-fx-text-fill: rgba(0,0,0,0.68); -fx-font-size: 15.5px;");

        Button btn = UITheme.serkoBlackPill("Get started", true);
        btn.setPadding(new Insets(12, 24, 12, 24));
        btn.setOnAction(e -> onOpenAuth.accept(true));

        HBox btnWrap = new HBox(btn);
        btnWrap.setAlignment(Pos.CENTER);
        btnWrap.setPadding(new Insets(10, 0, 0, 0));

        cta.getChildren().addAll(eyebrow, headline, sub, btnWrap);
        return cta;
    }

    // ── Staggered Content-Fitted FAQ Accordion (Matches live-06) ─

    private VBox buildFaqSection(List<String[]> faqs) {
        VBox band = new VBox(28);
        band.setAlignment(Pos.CENTER);
        band.setPadding(new Insets(88, 56, 72, 56));
        band.setStyle("-fx-background-color: #FFFFFF;");

        VBox head = new VBox(8);
        head.setAlignment(Pos.CENTER);
        Label ey = UITheme.eyebrowLabel("FAQs");
        Label h2 = new Label("Got a question we haven't covered?\nHere's what engineers usually ask.");
        h2.setTextAlignment(TextAlignment.CENTER);
        h2.setStyle("-fx-font-size: 30px; -fx-font-weight: 500; -fx-text-fill: #000000;");
        head.getChildren().addAll(ey, h2);

        VBox list = new VBox(12);
        list.setAlignment(Pos.CENTER_LEFT);
        list.setMaxWidth(540);

        for (String[] qa : faqs) {
            list.getChildren().add(faqBubbleItem(qa[0], qa[1]));
        }

        band.getChildren().addAll(head, list);
        return band;
    }

    private VBox faqBubbleItem(String q, String a) {
        VBox wrap = new VBox(6);
        wrap.setAlignment(Pos.CENTER_LEFT);

        // Compact speech-bubble pill fitted cleanly to question text (matches serko .faq1_toggle_button!)
        HBox btn = new HBox(10);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setMaxWidth(Region.USE_PREF_SIZE);
        btn.setPadding(new Insets(10, 14, 10, 14));
        btn.setCursor(Cursor.HAND);
        String faqNormal =
            "-fx-background-color: #FFFFFF;" +
            "-fx-border-color: rgba(0,0,0,0.08);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 9 9 3 9;" +
            "-fx-background-radius: 9 9 3 9;";
        String faqHover =
            "-fx-background-color: #FAFAFA;" +
            "-fx-border-color: rgba(0,0,0,0.20);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 9 9 3 9;" +
            "-fx-background-radius: 9 9 3 9;";
        btn.setStyle(faqNormal);
        DropShadow ds = new DropShadow();
        ds.setColor(Color.rgb(0, 0, 0, 0.06));
        ds.setRadius(4);
        ds.setOffsetY(1);
        btn.setEffect(ds);

        DropShadow dsHover = new DropShadow();
        dsHover.setColor(Color.rgb(0, 0, 0, 0.11));
        dsHover.setRadius(10);
        dsHover.setOffsetY(3);
        installCardLift(btn, -1.5, 1.012, ds, dsHover, faqNormal, faqHover);

        Label qLbl = new Label(q);
        qLbl.setWrapText(true);
        qLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: 400; -fx-text-fill: #000000;");

        Node chev = UITheme.chevronDownIcon(Color.BLACK, 0.65);
        btn.getChildren().addAll(qLbl, chev);

        Label aLbl = new Label(a);
        aLbl.setWrapText(true);
        aLbl.setMaxWidth(520);
        aLbl.setPadding(new Insets(12, 16, 12, 16));
        aLbl.setStyle(
            "-fx-background-color: rgba(0,0,0,0.035); -fx-background-radius: 9 9 9 3;" +
            "-fx-font-size: 13.5px; -fx-text-fill: #4D4D4D; -fx-line-spacing: 3px;"
        );
        aLbl.setVisible(false);
        aLbl.setManaged(false);

        final boolean[] open = {false};
        btn.setOnMouseClicked(e -> {
            open[0] = !open[0];
            aLbl.setVisible(open[0]);
            aLbl.setManaged(open[0]);
            if (open[0]) {
                aLbl.setOpacity(0);
                aLbl.setTranslateY(-4);
                FadeTransition ft = new FadeTransition(Duration.millis(180), aLbl);
                ft.setToValue(1.0);
                TranslateTransition tt = new TranslateTransition(Duration.millis(180), aLbl);
                tt.setToY(0);
                tt.setInterpolator(EASE_OUT);
                new ParallelTransition(ft, tt).play();
            }
            RotateTransition rt = new RotateTransition(Duration.millis(180), chev);
            rt.setToAngle(open[0] ? 180 : 0);
            rt.play();
        });

        wrap.getChildren().addAll(btn, aLbl);
        return wrap;
    }

    // ── Light Editorial Footer + Full-Width Dotted Hairline (live-07) ─

    private VBox buildLightFooter() {
        VBox footerWrap = new VBox(0);
        footerWrap.setStyle("-fx-background-color: #FFFFFF;");

        // Full-width subtle dotted horizontal divider matching live-07-footer.png
        Region footerDivider = UITheme.responsiveDottedDivider(1160);
        HBox.setHgrow(footerDivider, Priority.ALWAYS);
        HBox dividerBox = new HBox(footerDivider);
        dividerBox.setAlignment(Pos.CENTER);
        dividerBox.setPadding(new Insets(8, 56, 8, 56));

        VBox footer = new VBox(28);
        footer.setPadding(new Insets(48, 56, 38, 56));

        HBox topCols = new HBox(48);
        topCols.setAlignment(Pos.TOP_LEFT);

        VBox left = new VBox(16);
        left.setMaxWidth(380);
        HBox.setHgrow(left, Priority.ALWAYS);

        StackPane darkMark = UITheme.brandLogoMark(28, false);

        Text fBold = new Text("Be there");
        fBold.setFont(Font.font(UITheme.FONT_FAMILY, FontWeight.MEDIUM, 24));
        fBold.setFill(Color.BLACK);
        Text fMuted = new Text(" for the incidents\nthat matter.");
        fMuted.setFont(Font.font(UITheme.FONT_FAMILY, FontWeight.NORMAL, 24));
        fMuted.setFill(Color.web("#71717A"));
        TextFlow fHeadline = new TextFlow(fBold, fMuted);

        Button btnStartBlack = UITheme.serkoBlackPill("Get started", true);
        btnStartBlack.setPadding(new Insets(10, 20, 10, 20));
        btnStartBlack.setOnAction(e -> onOpenAuth.accept(true));
        left.getChildren().addAll(darkMark, fHeadline, btnStartBlack);

        HBox rightCols = new HBox(44);
        rightCols.setAlignment(Pos.TOP_LEFT);
        rightCols.getChildren().addAll(
            footerColumn("Explore", List.of(
                footerLink("Home",      () -> navigateTo(Page.HOME)),
                footerLink("Platform",  () -> navigateTo(Page.PLATFORM)),
                footerLink("For teams", () -> navigateTo(Page.FOR_TEAMS)),
                footerLink("About",     () -> navigateTo(Page.ABOUT))
            )),
            footerColumn("Capabilities", List.of(
                footerLink("6-Agent pipeline", () -> {
                    navigateTo(Page.PLATFORM);
                    Platform.runLater(() -> scrollPane.setVvalue(0.32));
                }),
                footerLink("Quick LLM triage", () -> navigateTo(Page.PLATFORM)),
                footerLink("MTTR analytics",   () -> navigateTo(Page.FOR_TEAMS))
            )),
            footerColumn("Account", List.of(
                footerLink("Log in",         () -> onOpenAuth.accept(false)),
                footerLink("Create account", () -> onOpenAuth.accept(true))
            ))
        );

        Region midSp = spacerRegion();
        topCols.getChildren().addAll(left, midSp, rightCols);

        HBox bottomBar = new HBox();
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        Label copy = new Label("© 2026 IncidentIQ");
        copy.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 12px;");
        bottomBar.getChildren().add(copy);

        footer.getChildren().addAll(topCols, bottomBar);
        footerWrap.getChildren().addAll(dividerBox, footer);
        return footerWrap;
    }

    private VBox footerColumn(String title, List<Button> links) {
        VBox col = new VBox(8);
        Label t = new Label(title);
        t.setStyle("-fx-text-fill: #000000; -fx-font-size: 13px; -fx-font-weight: 500; -fx-padding: 0 0 4 0;");
        col.getChildren().add(t);
        col.getChildren().addAll(links);
        return col;
    }

    private Button footerLink(String text, Runnable action) {
        Button b = new Button(text);
        b.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #52525B;" +
            "-fx-font-size: 13px; -fx-font-weight: 400; -fx-padding: 2 0; -fx-cursor: hand;"
        );
        b.setOnMouseEntered(e -> b.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #000000;" +
            "-fx-font-size: 13px; -fx-font-weight: 400; -fx-padding: 2 0; -fx-underline: true; -fx-cursor: hand;"
        ));
        b.setOnMouseExited(e -> b.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #52525B;" +
            "-fx-font-size: 13px; -fx-font-weight: 400; -fx-padding: 2 0; -fx-cursor: hand;"
        ));
        b.setOnAction(e -> action.run());
        return b;
    }

    // ═════════════════════════════════════════════════════════════
    //  PAGE 2: PLATFORM (Matches live-08-platform.png Soft Blue Halo)
    // ═════════════════════════════════════════════════════════════

    private void buildPlatformPage() {
        faqAnchorFraction = 0.78;
        StackPane hero = new StackPane();
        hero.setMinHeight(760);
        hero.getStyleClass().add("serko-platform-halo-bg");

        VBox content = new VBox(14);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(82, 56, 40, 56));
        content.setMaxWidth(900);

        Label eyebrow = UITheme.eyebrowLabel("AI incident software");
        TextFlow headline = UITheme.gradientAccentFlow(
            "Built around ", "autonomous agents", "\nfrom the ground up.", 52, TextAlignment.CENTER, true
        );
        Label sub = new Label(
            "Paste a production stack trace once. Six specialized agents analyze logs, classify failure domain, " +
            "evaluate severity, and surface verified remediation steps automatically."
        );
        sub.setWrapText(true);
        sub.setTextAlignment(TextAlignment.CENTER);
        sub.setMaxWidth(600);
        sub.setStyle("-fx-text-fill: rgba(0,0,0,0.68); -fx-font-size: 16px;");

        Button btnTry = UITheme.serkoBlackPill("Get started", true);
        btnTry.setPadding(new Insets(11, 24, 11, 24));
        btnTry.setOnAction(e -> onOpenAuth.accept(true));
        Button btnFaq = UITheme.serkoWhitePill("FAQs", true);
        btnFaq.setPadding(new Insets(11, 22, 11, 22));
        btnFaq.setOnAction(e -> scrollPane.setVvalue(faqAnchorFraction));
        HBox ctas = new HBox(10, btnTry, btnFaq);
        ctas.setAlignment(Pos.CENTER);
        ctas.setPadding(new Insets(0, 0, 8, 0));

        // Interactive Platform Composer Card matching live-08-platform.png
        VBox composerCard = new VBox(14);
        composerCard.setMaxWidth(640);
        composerCard.setPadding(new Insets(22));
        String compCardNormal =
            "-fx-background-color: #FFFFFF; -fx-background-radius: 18;" +
            "-fx-border-color: rgba(0,0,0,0.06); -fx-border-radius: 18; -fx-border-width: 1;";
        String compCardHover =
            "-fx-background-color: #FFFFFF; -fx-background-radius: 18;" +
            "-fx-border-color: rgba(27,152,242,0.32); -fx-border-radius: 18; -fx-border-width: 1;";
        composerCard.setStyle(compCardNormal);
        DropShadow compShadow = UITheme.softCardShadow();
        composerCard.setEffect(compShadow);
        installCardLift(composerCard, -3.5, 1.008, compShadow, UITheme.elevatedCardShadow(), compCardNormal, compCardHover);

        HBox topFields = new HBox(24);
        Label svcVal = medLabel("checkout-api · PaymentLedgerDao", 13, "#71717A");
        Label sevVal = medLabel("Critical · Open", 13, "#71717A");
        VBox f1 = new VBox(4, microCap("SERVICE / MODULE"), svcVal);
        VBox f2 = new VBox(4, microCap("SEVERITY"), sevVal);
        HBox.setHgrow(f1, Priority.ALWAYS);
        topFields.getChildren().addAll(f1, f2);

        Region sep = new Region();
        sep.setMinHeight(1);
        sep.setPrefHeight(1);
        sep.setMaxHeight(1);
        sep.setMaxWidth(Double.MAX_VALUE);
        sep.setStyle("-fx-background-color: #F4F4F5;");

        VBox detailsBlock = new VBox(10);
        detailsBlock.getChildren().add(microCap("ERROR LOG DETAILS AND CONTEXT"));

        FlowPane tokenPreview = new FlowPane(6, 6);
        tokenPreview.setAlignment(Pos.CENTER_LEFT);
        Label tPre = new Label("Saw");
        Label tErr = highlightToken("SQLTransientConnectionException", "#F3E8FF", "#6B21A8");
        Label tMid = new Label("after 30s on");
        Label tMod = highlightToken("HikariPool-1", "#FCE7F3", "#9D174D");
        Label tEnd = new Label("settlement path.");
        for (Label l : new Label[]{tPre, tMid, tEnd}) {
            l.setStyle("-fx-font-size: 13.5px; -fx-text-fill: #18181B;");
        }
        tokenPreview.getChildren().addAll(tPre, tErr, tMid, tMod, tEnd);

        // Simulated 6-agent pipeline demo panel (clearly framed as product sample preview)
        VBox simOutputBox = new VBox(6);
        simOutputBox.setPadding(new Insets(12, 14, 12, 14));
        simOutputBox.setStyle("-fx-background-color: #EFF6FF; -fx-border-color: #DBEAFE; -fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;");
        HBox simTop = new HBox(6, UITheme.positiveBadge("Sample 6-agent trace"), UITheme.neutralTag("Pipeline demo"));
        simTop.setAlignment(Pos.CENTER_LEFT);
        Label simRoot = new Label("Root cause: Connections leaked on retry loop in recordSettlement() holding transactions past 30s maxLifetime.");
        simRoot.setWrapText(true);
        simRoot.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #18181B; -fx-font-weight: 500;");
        Label simFix = new Label("Remediation: Wrap ledger writes in @Transactional(timeout = 10) and enable leakDetectionThreshold=5000.");
        simFix.setWrapText(true);
        simFix.setStyle(UITheme.monoStyle(11.5, "#1D4ED8"));
        simOutputBox.getChildren().addAll(simTop, simRoot, simFix);
        simOutputBox.setVisible(false);
        simOutputBox.setManaged(false);

        // Sample selector pills + Inspect output toggle + Analyze CTA
        String[] sampleChips = {"HikariPool", "ExpiredJwt", "NullPointer"};
        HBox chipTrack = new HBox(5);
        chipTrack.setAlignment(Pos.CENTER_LEFT);
        List<Button> chipBtns = new ArrayList<>();
        String cActive = "-fx-background-color: #09090B; -fx-text-fill: #FFFFFF; -fx-font-size: 11px; -fx-font-weight: 500; -fx-padding: 4 10; -fx-background-radius: 999; -fx-cursor: hand;";
        String cIdle   = "-fx-background-color: #F4F4F5; -fx-text-fill: #52525B; -fx-font-size: 11px; -fx-font-weight: 500; -fx-padding: 4 10; -fx-background-radius: 999; -fx-cursor: hand;";
        for (String name : sampleChips) {
            Button cb = new Button(name);
            cb.setMinWidth(Region.USE_PREF_SIZE);
            chipBtns.add(cb);
            chipTrack.getChildren().add(cb);
        }

        Button btnInspectDemo = UITheme.serkoWhitePill("Inspect agent output", false);
        btnInspectDemo.setPadding(new Insets(7, 14, 7, 14));
        btnInspectDemo.setOnAction(e -> {
            boolean show = !simOutputBox.isVisible();
            simOutputBox.setVisible(show);
            simOutputBox.setManaged(show);
            btnInspectDemo.setText(show ? "Hide agent output" : "Inspect agent output");
        });

        Button btnAnalyze = UITheme.serkoBlackPill("Analyze trace", true);
        btnAnalyze.setPadding(new Insets(8, 18, 8, 18));
        btnAnalyze.setOnAction(e -> onOpenAuth.accept(false));
        HBox bottomActionRow = new HBox(8, chipTrack, spacerRegion(), btnInspectDemo, btnAnalyze);
        bottomActionRow.setAlignment(Pos.CENTER_LEFT);
        detailsBlock.getChildren().addAll(tokenPreview, simOutputBox, bottomActionRow);

        composerCard.getChildren().addAll(topFields, sep, detailsBlock);

        String[][] tokenSamples = {
            {
                "SQLTransientConnectionException", "HikariPool-1",
                "checkout-api · PaymentLedgerDao", "Critical · Open", "Saw", "after 30s on", "settlement path.",
                "Root cause: Connections leaked on retry loop in recordSettlement() holding transactions past 30s maxLifetime.",
                "Remediation: Wrap ledger writes in @Transactional(timeout = 10) and set leakDetectionThreshold=5000."
            },
            {
                "ExpiredJwtException", "/api/incidents/my",
                "auth-filter · JwtAuthFilter", "High · Investigating", "Threw", "validating bearer token at", "with 45ms skew.",
                "Root cause: Access token clock skew across edge replica nodes rejected borderline tokens before refresh.",
                "Remediation: Configure Jwts.parser().clockSkewSeconds(30) and align NTP daemon sync."
            },
            {
                "NullPointerException", "PaymentGateway.authorize",
                "billing-core · GatewayClient", "Critical · Open", "Hit", "dereferencing merchant metadata inside", "at line 418.",
                "Root cause: Optional PSP settlement map returned null when multi-currency flag is unset on sandbox accounts.",
                "Remediation: Guard null settlement metadata with Optional.ofNullable(...) and fallback currency ISO check."
            }
        };
        final int[] sIdx = {0};
        final boolean[] manualLocked = {false};
        Runnable applySample = () -> {
            int i = sIdx[0];
            for (int k = 0; k < chipBtns.size(); k++) {
                chipBtns.get(k).setStyle(k == i ? cActive : cIdle);
            }
            String[] row = tokenSamples[i];
            crossfadeLabelSwap(tErr, row[0]);
            crossfadeLabelSwap(tMod, row[1]);
            crossfadeLabelSwap(svcVal, row[2]);
            crossfadeLabelSwap(sevVal, row[3]);
            tPre.setText(row[4]);
            tMid.setText(row[5]);
            tEnd.setText(row[6]);
            simRoot.setText(row[7]);
            simFix.setText(row[8]);
        };
        for (int k = 0; k < chipBtns.size(); k++) {
            final int sampleIdx = k;
            chipBtns.get(k).setStyle(k == 0 ? cActive : cIdle);
            chipBtns.get(k).setOnAction(e -> {
                manualLocked[0] = true;
                if (typewriterTimeline != null) typewriterTimeline.stop();
                sIdx[0] = sampleIdx;
                applySample.run();
                if (!simOutputBox.isVisible()) {
                    simOutputBox.setVisible(true);
                    simOutputBox.setManaged(true);
                    btnInspectDemo.setText("Hide agent output");
                }
            });
        }

        typewriterTimeline = new Timeline(new KeyFrame(Duration.seconds(3.6), ev -> {
            if (!manualLocked[0]) {
                sIdx[0] = (sIdx[0] + 1) % tokenSamples.length;
                applySample.run();
            }
        }));
        typewriterTimeline.setCycleCount(Timeline.INDEFINITE);
        typewriterTimeline.play();

        content.getChildren().addAll(eyebrow, headline, sub, ctas, composerCard);
        hero.getChildren().add(content);

        // 6-Agent Architecture Showcase Section
        VBox agentsBand = new VBox(28);
        agentsBand.setAlignment(Pos.CENTER);
        agentsBand.setPadding(new Insets(84, 56, 84, 56));
        agentsBand.setStyle("-fx-background-color: #FFFFFF;");

        Label aEye = UITheme.eyebrowLabel("AI-native technology");
        Label aH2 = new Label("What makes IncidentIQ different from a plain chatbot.");
        aH2.setStyle("-fx-font-size: 32px; -fx-font-weight: 500; -fx-text-fill: #000000;");
        Label aSub = new Label("Select any agent below to inspect its role in the autonomous investigation pipeline:");
        aSub.setStyle("-fx-text-fill: #52525B; -fx-font-size: 15px;");

        HBox pipelineWrap = new HBox(24);
        pipelineWrap.setMaxWidth(980);
        pipelineWrap.setAlignment(Pos.CENTER);

        VBox stepList = new VBox(8);
        stepList.setPrefWidth(360);
        VBox stepDetailCard = new VBox(12);
        stepDetailCard.setPadding(new Insets(28));
        stepDetailCard.setStyle("-fx-background-color: " + UITheme.BG_ICE + "; -fx-background-radius: 22;");
        HBox.setHgrow(stepDetailCard, Priority.ALWAYS);

        Label detStage = UITheme.positiveBadge("Agent 1 of 6");
        Label detTitle = medLabel("1. Log Analysis Agent", 22, "#000000");
        Label detDesc = new Label("Parses raw stack traces, filters framework noise, and extracts exception frames, SQL/HTTP codes, and failing timestamps.");
        detDesc.setWrapText(true);
        detDesc.setStyle("-fx-font-size: 14.5px; -fx-text-fill: #1F2937; -fx-line-spacing: 3px;");
        stepDetailCard.getChildren().addAll(detStage, detTitle, detDesc);

        String[][] stages = {
            {"1. Log Analysis Agent",          "Agent 1 of 6", "Parses raw stack traces, filters framework noise, and extracts top exception frames, SQL/HTTP error codes, and failing classes."},
            {"2. Error Classification Agent",  "Agent 2 of 6", "Categorizes incidents cleanly (Database, Security/JWT, Memory/OOM, Network, API Integration) using historical signatures."},
            {"3. Severity Evaluation Agent",   "Agent 3 of 6", "Scores blast radius and urgency (Critical / High / Medium / Low) so outages surface cleanly."},
            {"4. Root Cause Agent",            "Agent 4 of 6", "Synthesizes code-path evidence to pinpoint why the failure occurred rather than repeating exception text."},
            {"5. Resolution Agent",            "Agent 5 of 6", "Generates prioritized remediation actions, configuration changes, and defensive code guards."},
            {"6. Report Generation Agent",     "Agent 6 of 6", "Compiles an executive summary and structured Markdown report persisted directly to the database."}
        };

        String sbNormal = "-fx-background-color: #F6F6F6; -fx-text-fill: #3F3F46; -fx-font-weight: 500; -fx-font-size: 13.5px; -fx-padding: 13 18; -fx-background-radius: 12; -fx-border-color: transparent; -fx-border-radius: 12; -fx-border-width: 1.5; -fx-cursor: hand;";
        String sbHover  = "-fx-background-color: #ECECEE; -fx-text-fill: #000000; -fx-font-weight: 500; -fx-font-size: 13.5px; -fx-padding: 13 18; -fx-background-radius: 12; -fx-border-color: transparent; -fx-border-radius: 12; -fx-border-width: 1.5; -fx-cursor: hand;";
        String sbActive = "-fx-background-color: #FFFFFF; -fx-text-fill: #000000; -fx-font-weight: 600; -fx-font-size: 13.5px; -fx-padding: 13 18; -fx-background-radius: 12; -fx-border-color: #1B98F2; -fx-border-radius: 12; -fx-border-width: 1.5; -fx-cursor: hand;";

        final int[] activeStageIdx = {0};
        final List<Button> stageBtns = new java.util.ArrayList<>();
        for (int i = 0; i < stages.length; i++) {
            final int idx = i;
            String[] st = stages[i];
            Button sb = new Button(st[0]);
            sb.setMaxWidth(Double.MAX_VALUE);
            sb.setAlignment(Pos.CENTER_LEFT);
            sb.setStyle(i == 0 ? sbActive : sbNormal);
            sb.setEffect(i == 0 ? UITheme.softCardShadow() : null);
            stageBtns.add(sb);

            sb.setOnMouseEntered(e -> {
                if (activeStageIdx[0] != idx) {
                    sb.setStyle(sbHover);
                    animateTranslateX(sb, 4.0, 140);
                }
            });
            sb.setOnMouseExited(e -> {
                if (activeStageIdx[0] != idx) {
                    sb.setStyle(sbNormal);
                    animateTranslateX(sb, 0.0, 140);
                }
            });
            sb.setOnAction(e -> {
                activeStageIdx[0] = idx;
                for (int k = 0; k < stageBtns.size(); k++) {
                    boolean active = (k == idx);
                    stageBtns.get(k).setStyle(active ? sbActive : sbNormal);
                    stageBtns.get(k).setEffect(active ? UITheme.softCardShadow() : null);
                    animateTranslateX(stageBtns.get(k), active ? 4.0 : 0.0, 150);
                }
                detStage.setText(st[1]);
                crossfadeLabelSwap(detTitle, st[0]);
                crossfadeLabelSwap(detDesc, st[2]);
            });
            stepList.getChildren().add(sb);
        }

        pipelineWrap.getChildren().addAll(stepList, stepDetailCard);
        agentsBand.getChildren().addAll(aEye, aH2, aSub, pipelineWrap);

        // Security strip
        VBox secBand = new VBox(10);
        secBand.setAlignment(Pos.CENTER);
        secBand.setPadding(new Insets(64, 56, 64, 56));
        secBand.setStyle("-fx-background-color: #F6F6F6;");
        Label sEye = UITheme.eyebrowLabel("Platform security");
        Label sH2 = new Label("Your logs stay your team's data.");
        sH2.setStyle("-fx-font-size: 30px; -fx-font-weight: 500; -fx-text-fill: #000000;");
        Label sSub = new Label("Protected with Spring Security stateless JWT authentication, parameterized JPA persistence, and environment-isolated AI keys.");
        sSub.setWrapText(true);
        sSub.setMaxWidth(620);
        sSub.setTextAlignment(TextAlignment.CENTER);
        sSub.setStyle("-fx-text-fill: #52525B; -fx-font-size: 15px;");
        secBand.getChildren().addAll(sEye, sH2, sSub);

        pageContainer.getChildren().addAll(
            hero,
            agentsBand,
            secBand,
            buildClosingCtaSection("Set up", "Get going in an instant.", "Start triaging with ", "IncidentIQ.",
                "Create your engineering account, paste your first stack trace, and watch 6 agents go to work."),
            buildFaqSection(defaultHomeFaqs()),
            buildLightFooter()
        );
    }

    private Label highlightToken(String text, String bg, String fg) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; -fx-font-size: 13px; -fx-font-weight: 500; -fx-padding: 2 7; -fx-background-radius: 5;");
        return l;
    }

    private Label microCap(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 10px; -fx-font-weight: 600; -fx-text-fill: #9CA3AF;");
        return l;
    }

    // ═════════════════════════════════════════════════════════════
    //  PAGE 3: FOR TEAMS (Matches https://www.serko.ai/for-companies)
    // ═════════════════════════════════════════════════════════════

    private void buildForTeamsPage() {
        faqAnchorFraction = 0.78;
        StackPane hero = new StackPane();
        hero.setMinHeight(640);
        hero.getStyleClass().add("serko-platform-halo-bg");

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(138, 56, 64, 56));
        content.setMaxWidth(960);

        Label eyebrow = UITheme.eyebrowLabel("Engineering operations management");
        TextFlow headline = UITheme.gradientAccentFlow(
            "A new way to run your\n", "incident", " response.", 52, TextAlignment.CENTER, true
        );
        Label sub = new Label(
            "IncidentIQ eliminates noisy manual triage and keeps engineering leads informed with live MTTR, " +
            "AI coverage metrics, and collaborative investigation timelines."
        );
        sub.setWrapText(true);
        sub.setTextAlignment(TextAlignment.CENTER);
        sub.setMaxWidth(620);
        sub.setStyle("-fx-text-fill: rgba(0,0,0,0.68); -fx-font-size: 16px;");

        HBox teamCards = new HBox(14);
        teamCards.setAlignment(Pos.CENTER);
        teamCards.setPadding(new Insets(14, 0, 10, 0));
        teamCards.getChildren().addAll(
            teamStatusCard("Checkout API",   "US-East",   "Reviewed by AI", true),
            teamStatusCard("Auth Gateway",   "EU-West",   "Critical",       false),
            teamStatusCard("Billing Worker", "AP-South",  "Patch ready",    true)
        );

        Button btnFree = UITheme.serkoBlackPill("Get started", true);
        btnFree.setPadding(new Insets(11, 24, 11, 24));
        btnFree.setOnAction(e -> onOpenAuth.accept(true));

        content.getChildren().addAll(eyebrow, headline, sub, teamCards, btnFree);
        hero.getChildren().add(content);

        VBox gridBand = new VBox(24);
        gridBand.setAlignment(Pos.CENTER);
        gridBand.setPadding(new Insets(84, 56, 84, 56));
        gridBand.setStyle("-fx-background-color: #FFFFFF;");

        HBox cardsRow = new HBox(20);
        cardsRow.setMaxWidth(980);
        cardsRow.getChildren().addAll(
            enterpriseCard("Incident workflow", "Skip the midnight war rooms", "Instant 6-agent triage gives on-call engineers concrete root causes in seconds instead of manual log searches.", UITheme.BG_SOFT),
            enterpriseCard("Telemetry visibility", "Ask once, see real metrics", "Track Open vs Resolved volume, Critical backlog, MTTR hours, and AI coverage percentages live.", UITheme.BG_ICE),
            enterpriseCard("Fast onboarding", "Zero external tooling setup", "Connected directly to your Spring Boot REST backend, MySQL database, and Groq / Gemini keys.", UITheme.BG_SOFT)
        );
        gridBand.getChildren().add(cardsRow);

        pageContainer.getChildren().addAll(
            hero,
            gridBand,
            buildClosingCtaSection("Get started", "Start using ", "", "IncidentIQ today.",
                "Sign in or register in seconds and bring calm, AI-assisted incident response to your engineering team."),
            buildLightFooter()
        );
    }

    private VBox teamStatusCard(String service, String region, String status, boolean positive) {
        VBox c = new VBox(8);
        c.setPrefWidth(220);
        c.setPadding(new Insets(16));
        String cNormal = "-fx-background-color: #FFFFFF; -fx-background-radius: 16; -fx-border-color: rgba(0,0,0,0.06); -fx-border-radius: 16; -fx-border-width: 1;";
        String cHover  = "-fx-background-color: #FFFFFF; -fx-background-radius: 16; -fx-border-color: rgba(27,152,242,0.38); -fx-border-radius: 16; -fx-border-width: 1;";
        c.setStyle(cNormal);
        DropShadow baseSh = UITheme.softCardShadow();
        c.setEffect(baseSh);
        c.setCursor(Cursor.HAND);
        installCardLift(c, -4.5, 1.02, baseSh, UITheme.elevatedCardShadow(), cNormal, cHover);
        Label s = medLabel(service, 14.5, "#000000");
        Label r = new Label(region);
        r.setStyle("-fx-font-size: 12px; -fx-text-fill: #71717A;");
        Label badge = positive ? UITheme.positiveBadge(status) : UITheme.severityBadge("CRITICAL");
        badge.setText(status);
        c.getChildren().addAll(s, r, badge);
        return c;
    }

    private VBox enterpriseCard(String ey, String title, String desc, String bg) {
        VBox c = new VBox(12);
        c.setPadding(new Insets(26));
        String eNormal = "-fx-background-color: " + bg + "; -fx-background-radius: 20; -fx-border-color: transparent; -fx-border-radius: 20; -fx-border-width: 1;";
        String eHover  = "-fx-background-color: " + bg + "; -fx-background-radius: 20; -fx-border-color: rgba(0,0,0,0.12); -fx-border-radius: 20; -fx-border-width: 1;";
        c.setStyle(eNormal);
        HBox.setHgrow(c, Priority.ALWAYS);
        installCardLift(c, -4.0, 1.012, null, UITheme.softCardShadow(), eNormal, eHover);
        Label e = UITheme.eyebrowLabel(ey);
        Label t = medLabel(title, 20, "#000000");
        t.setWrapText(true);
        Label d = new Label(desc);
        d.setWrapText(true);
        d.setStyle("-fx-font-size: 14px; -fx-text-fill: #52525B; -fx-line-spacing: 3px;");
        c.getChildren().addAll(e, t, d);
        return c;
    }

    // ═════════════════════════════════════════════════════════════
    //  PAGE 4: ABOUT (Matches https://www.serko.ai/about)
    // ═════════════════════════════════════════════════════════════

    private void buildAboutPage() {
        faqAnchorFraction = 0.72;
        StackPane hero = new StackPane();
        hero.setMinHeight(580);
        hero.getStyleClass().add("serko-platform-halo-bg");

        VBox content = new VBox(18);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(138, 56, 64, 56));
        content.setMaxWidth(840);

        Label eyebrow = UITheme.eyebrowLabel("IncidentIQ, your AI incident agent");
        TextFlow headline = UITheme.gradientAccentFlow(
            "We built IncidentIQ so engineers\ncan focus on ", "shipping.", "", 50, TextAlignment.CENTER, true
        );
        Label sub = new Label(
            "Software reliability should never depend on who happens to be awake at 2 AM parsing raw stack traces. " +
            "IncidentIQ pairs multi-agent reasoning with structured engineering workflows."
        );
        sub.setWrapText(true);
        sub.setTextAlignment(TextAlignment.CENTER);
        sub.setMaxWidth(620);
        sub.setStyle("-fx-text-fill: rgba(0,0,0,0.68); -fx-font-size: 16px;");

        Button btnExplore = UITheme.serkoBlackPill("Explore Platform", true);
        btnExplore.setPadding(new Insets(11, 24, 11, 24));
        btnExplore.setOnAction(e -> navigateTo(Page.PLATFORM));

        content.getChildren().addAll(eyebrow, headline, sub, btnExplore);
        hero.getChildren().add(content);

        VBox storyBand = new VBox(22);
        storyBand.setAlignment(Pos.CENTER);
        storyBand.setPadding(new Insets(84, 56, 84, 56));
        storyBand.setStyle("-fx-background-color: #FFFFFF;");

        VBox storyWrap = new VBox(18);
        storyWrap.setMaxWidth(720);
        storyWrap.getChildren().addAll(
            UITheme.eyebrowLabel("Our vision"),
            medLabel("Always in your corner when production alerts fire.", 28, "#000000"),
            paraLabel("Modern distributed services generate thousands of log lines before anyone opens a terminal. IncidentIQ bridges raw telemetry with an orchestrated 6-agent pipeline built specifically for Spring Boot, cloud APIs, and JVM services."),
            UITheme.responsiveDottedDivider(680),
            medLabel("AI-native by design", 22, "#000000"),
            paraLabel("Rather than pasting a prompt box over static tables, IncidentIQ connects authenticated JWT sessions, JPA incident lifecycles, sub-second Groq inference, and Gemini fallbacks into a cohesive desktop workbench.")
        );
        storyBand.getChildren().add(storyWrap);

        pageContainer.getChildren().addAll(
            hero,
            storyBand,
            buildFaqSection(defaultHomeFaqs()),
            buildLightFooter()
        );
    }

    // ── Shared Helpers & Motion Utilities ───────────────────────

    private void installCardLift(Region node, double hoverLiftY, double hoverScale,
                                 Effect baseEffect, Effect hoverEffect,
                                 String normalStyle, String hoverStyle) {
        installCardLift(node, hoverLiftY, hoverScale, baseEffect, hoverEffect, normalStyle, hoverStyle, null);
    }

    private void installCardLift(Region node, double hoverLiftY, double hoverScale,
                                 Effect baseEffect, Effect hoverEffect,
                                 String normalStyle, String hoverStyle,
                                 Consumer<Boolean> onHoverState) {
        node.setOnMouseEntered(e -> {
            if (onHoverState != null) onHoverState.accept(true);
            if (hoverStyle != null) node.setStyle(hoverStyle);
            if (hoverEffect != null) node.setEffect(hoverEffect);
            animateLiftAndScale(node, hoverLiftY, hoverScale, 165);
        });
        node.setOnMouseExited(e -> {
            if (onHoverState != null) onHoverState.accept(false);
            if (normalStyle != null) node.setStyle(normalStyle);
            node.setEffect(baseEffect);
            animateLiftAndScale(node, 0.0, 1.0, 150);
        });
    }

    private void animateLiftAndScale(Node node, double toY, double toScale, double millis) {
        Object prev = node.getProperties().get("liftAnim");
        if (prev instanceof Animation a) a.stop();

        TranslateTransition tt = new TranslateTransition(Duration.millis(millis), node);
        tt.setToY(toY);
        tt.setInterpolator(EASE_OUT);

        ScaleTransition st = new ScaleTransition(Duration.millis(millis), node);
        st.setToX(toScale);
        st.setToY(toScale);
        st.setInterpolator(EASE_OUT);

        ParallelTransition pt = new ParallelTransition(tt, st);
        node.getProperties().put("liftAnim", pt);
        pt.play();
    }

    private void animateScale(Node node, double toScale, double millis) {
        Object prev = node.getProperties().get("scaleAnim");
        if (prev instanceof Animation a) a.stop();

        ScaleTransition st = new ScaleTransition(Duration.millis(millis), node);
        st.setToX(toScale);
        st.setToY(toScale);
        st.setInterpolator(EASE_OUT);
        node.getProperties().put("scaleAnim", st);
        st.play();
    }

    private void animateTranslateX(Node node, double toX, double millis) {
        Object prev = node.getProperties().get("txAnim");
        if (prev instanceof Animation a) a.stop();

        TranslateTransition tt = new TranslateTransition(Duration.millis(millis), node);
        tt.setToX(toX);
        tt.setInterpolator(EASE_OUT);
        node.getProperties().put("txAnim", tt);
        tt.play();
    }

    private void crossfadeLabelSwap(Label lbl, String nextText) {
        Object prev = lbl.getProperties().get("swapAnim");
        if (prev instanceof Animation a) a.stop();
        lbl.setTranslateY(0);

        FadeTransition out = new FadeTransition(Duration.millis(110), lbl);
        out.setToValue(0.15);
        out.setOnFinished(e -> {
            lbl.setText(nextText);
            lbl.setTranslateY(4);
            FadeTransition in = new FadeTransition(Duration.millis(180), lbl);
            in.setToValue(1.0);
            in.setInterpolator(EASE_OUT);
            TranslateTransition slide = new TranslateTransition(Duration.millis(180), lbl);
            slide.setToY(0);
            slide.setInterpolator(EASE_OUT);
            ParallelTransition pt = new ParallelTransition(in, slide);
            lbl.getProperties().put("swapAnim", pt);
            pt.play();
        });
        lbl.getProperties().put("swapAnim", out);
        out.play();
    }

    private <T extends Region> T withScrollReveal(T band) {
        band.setOpacity(0.92);
        band.setTranslateY(12.0);
        band.getProperties().put("revealed", false);
        revealBands.add(band);
        return band;
    }

    private void checkScrollReveals() {
        if (revealBands.isEmpty() || scrollPane.getScene() == null) return;
        Bounds vpScene = scrollPane.localToScene(scrollPane.getBoundsInLocal());
        if (vpScene == null || vpScene.getHeight() <= 0) return;

        for (Region band : revealBands) {
            if (Boolean.TRUE.equals(band.getProperties().get("revealed"))) continue;
            Bounds bScene = band.localToScene(band.getBoundsInLocal());
            if (bScene != null && bScene.getMinY() <= vpScene.getMaxY() - 48) {
                band.getProperties().put("revealed", true);
                FadeTransition ft = new FadeTransition(Duration.millis(300), band);
                ft.setToValue(1.0);
                ft.setInterpolator(EASE_OUT);

                TranslateTransition tt = new TranslateTransition(Duration.millis(340), band);
                tt.setToY(0.0);
                tt.setInterpolator(EASE_OUT);

                new ParallelTransition(ft, tt).play();
            }
        }
    }

    private GridPane twoColumnSectionGrid(double hgap) {
        GridPane grid = new GridPane();
        grid.setMaxWidth(1060);
        grid.setHgap(hgap);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(47);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(53);
        grid.getColumnConstraints().addAll(c1, c2);
        return grid;
    }

    private Label medLabel(String txt, double size, String colorHex) {
        Label l = new Label(txt);
        l.setWrapText(true);
        l.setStyle("-fx-font-size: " + size + "px; -fx-font-weight: 500; -fx-text-fill: " + colorHex + ";");
        return l;
    }

    private Label paraLabel(String txt) {
        Label l = new Label(txt);
        l.setWrapText(true);
        l.setStyle("-fx-font-size: 15.5px; -fx-text-fill: #52525B; -fx-line-spacing: 4px;");
        return l;
    }

    private Region spacerRegion() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    private List<String[]> defaultHomeFaqs() {
        return List.of(
            new String[]{
                "What is IncidentIQ?",
                "IncidentIQ is an AI-native incident management platform powered by Spring Boot, Groq / Gemini LLMs, and a 6-agent orchestration pipeline built to turn raw error logs into actionable root causes."
            },
            new String[]{
                "How does the 6-agent investigation pipeline work?",
                "When triggered, six specialized stages run in sequence: Log Analysis, Error Classification, Severity Evaluation, Root Cause Analysis, Resolution Planning, and Report Generation."
            },
            new String[]{
                "How is this different from pasting stack traces into a chatbot?",
                "Every diagnosis runs directly against structured incident records, enforces severity/category extraction, and persists full Markdown investigation reports alongside live MTTR analytics."
            },
            new String[]{
                "Is my authentication and incident data isolated?",
                "Yes. Every API call uses stateless JWT Bearer authentication and executes locally against your Spring Boot backend and MySQL database."
            },
            new String[]{
                "Which AI providers are supported?",
                "IncidentIQ supports fast Groq LPU inference as well as Gemini API fallback configured in application.properties."
            }
        );
    }
}
