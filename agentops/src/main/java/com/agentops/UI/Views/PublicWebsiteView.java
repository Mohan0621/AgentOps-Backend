package com.agentops.UI.Views;

import java.util.List;
import java.util.function.Consumer;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
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

    private Page currentPage = Page.HOME;
    private boolean scrolledPastTop = false;
    private double faqAnchorFraction = 0.78;
    private Timeline heroCycleTimeline;
    private Timeline typewriterTimeline;
    private TranslateTransition marqueeTransition;
    private SerkoHeroGlobe activeHeroGlobe;

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
            if (activeHeroGlobe != null) {
                activeHeroGlobe.setPaused(v > 0.25);
            }
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
        if (activeHeroGlobe != null) {
            activeHeroGlobe.setPaused(!active || scrollPane.getVvalue() > 0.25);
        }
        if (marqueeTransition != null) {
            if (active) marqueeTransition.play();
            else marqueeTransition.pause();
        }
        if (heroCycleTimeline != null) {
            if (active) heroCycleTimeline.play();
            else heroCycleTimeline.pause();
        }
    }

    // ── Public Controls ─────────────────────────────────────────

    public void navigateTo(Page page) {
        stopTimers();
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
        if (typewriterTimeline != null) typewriterTimeline.stop();
        if (marqueeTransition != null) marqueeTransition.stop();
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
        brandPill.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10;");
        DropShadow pillShadow = new DropShadow();
        pillShadow.setColor(Color.rgb(0, 0, 0, 0.05));
        pillShadow.setRadius(10);
        pillShadow.setOffsetY(2);
        brandPill.setEffect(pillShadow);
        brandPill.setCursor(Cursor.HAND);
        brandPill.setOnMouseClicked(e -> navigateTo(Page.HOME));
        brandPill.getChildren().add(UITheme.brandWordmark(16.5, false));

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
                "-fx-background-color: rgba(255, 255, 255, 0.94);" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: rgba(0, 0, 0, 0.06);" +
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
            buildFeature1IceSection(),
            buildFeature2DisruptionSection(),
            buildFeature3SmartPillsSection(),
            buildFeature4WorkspacePreviewSection(),
            buildClosingCtaSection("Get started", "Got an outage piling up?", "Get it sorted with ", "IncidentIQ.",
                "Skip manual stack-trace grepping. Paste your logs and let 6 autonomous agents isolate root cause and draft the fix."),
            buildFaqSection(defaultHomeFaqs()),
            buildLightFooter()
        );
    }

    private StackPane buildHomeHeroSection() {
        StackPane hero = new StackPane();
        hero.setMinHeight(1080);
        hero.setPrefHeight(1100);
        hero.setAlignment(Pos.TOP_CENTER);

        // Background: JS HTML5 Canvas 3D Charcoal Stippled Globe + Sky Horizon Gradient
        SerkoHeroGlobe globeBackdrop = new SerkoHeroGlobe();
        this.activeHeroGlobe = globeBackdrop;

        // Above-the-fold Hero Typography + CTAs, followed cleanly BELOW the globe dome by the Profile Showcase Widget
        VBox content = new VBox(14);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(142, 48, 56, 48));
        content.setMaxWidth(960);

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
        domeSpacer.setMinHeight(335);
        domeSpacer.setPrefHeight(345);

        StackPane layeredWidget = buildHeroInteractiveCard();
        content.getChildren().addAll(eyebrow, titleFlow, sub, ctaRow, domeSpacer, layeredWidget);

        hero.getChildren().addAll(globeBackdrop, content);
        return hero;
    }

    /**
     * Layered hero profile + floating confirmation chips matching serko.ai hero1_profile_wrap.
     */
    private StackPane buildHeroInteractiveCard() {
        StackPane stage = new StackPane();
        stage.setMaxWidth(680);
        stage.setMinHeight(210);
        stage.setAlignment(Pos.BOTTOM_CENTER);

        // Floating top-left service route confirmation chip (hero1_profile_route_wrap)
        VBox routeChip = new VBox(3);
        routeChip.setMaxWidth(215);
        routeChip.setPadding(new Insets(12, 16, 12, 16));
        routeChip.setStyle(
            "-fx-background-color: #FFFFFF; -fx-background-radius: 16;" +
            "-fx-border-color: rgba(0,0,0,0.06); -fx-border-radius: 16;"
        );
        routeChip.setEffect(UITheme.softCardShadow());
        HBox routeHead = new HBox(6, UITheme.terminalIcon(14, Color.BLACK), medLabel("CHECKOUT-API -> PATCH", 11.5, "#000000"));
        routeHead.setAlignment(Pos.CENTER_LEFT);
        Label routeMeta = medLabel("NullPointer · line 418", 11.5, "#71717A");
        routeChip.getChildren().addAll(routeHead, routeMeta);
        StackPane.setAlignment(routeChip, Pos.TOP_LEFT);
        StackPane.setMargin(routeChip, new Insets(10, 0, 0, 0));

        // Floating top-right verified deployment chip (hero1_profile_hotel_wrap)
        HBox confirmChip = new HBox(10);
        confirmChip.setMaxWidth(235);
        confirmChip.setAlignment(Pos.CENTER_LEFT);
        confirmChip.setPadding(new Insets(12, 16, 12, 16));
        confirmChip.setStyle(
            "-fx-background-color: #FFFFFF; -fx-background-radius: 16;" +
            "-fx-border-color: rgba(0,0,0,0.06); -fx-border-radius: 16;"
        );
        confirmChip.setEffect(UITheme.softCardShadow());
        VBox confTexts = new VBox(2, medLabel("HikariPool Guard", 12, "#000000"), medLabel("Auto-verified PR #88", 11, "#71717A"));
        Label confBadge = new Label("Confirmed");
        confBadge.setStyle(
            "-fx-background-color: " + UITheme.SERKO_POS_BG + "; -fx-text-fill: " + UITheme.SERKO_POS_INK + ";" +
            "-fx-font-size: 11px; -fx-font-weight: 500; -fx-padding: 3 8; -fx-background-radius: 6;"
        );
        confirmChip.getChildren().addAll(confTexts, confBadge);
        StackPane.setAlignment(confirmChip, Pos.TOP_RIGHT);
        StackPane.setMargin(confirmChip, new Insets(0, 6, 0, 0));

        // Foreground Serko layered profile widget (#F6F6F6 container with white prompt bar top + profile strip bottom)
        VBox shell = new VBox(12);
        shell.setMaxWidth(450);
        shell.setPadding(new Insets(14));
        shell.setStyle(
            "-fx-background-color: #F6F6F6;" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: rgba(0, 0, 0, 0.05);" +
            "-fx-border-radius: 20;"
        );
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.10));
        shadow.setRadius(32);
        shadow.setOffsetY(12);
        shell.setEffect(shadow);

        // Top white prompt choice bar (hero1_profile_choice_wrap)
        HBox queryBar = new HBox(10);
        queryBar.setAlignment(Pos.CENTER_LEFT);
        queryBar.setPadding(new Insets(11, 14, 11, 14));
        queryBar.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 14;");

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
        StackPane arrowCircle = new StackPane(UITheme.svgStrokeIcon("M12 19V5 M6 11l6-6 6 6", 13, Color.web("#52525B"), 1.8));
        arrowCircle.setPrefSize(26, 26);
        arrowCircle.setStyle("-fx-background-color: #F6F6F6; -fx-background-radius: 999; -fx-border-color: rgba(0,0,0,0.06); -fx-border-radius: 999;");

        queryBar.getChildren().addAll(checkBadge, lblActiveTrace, qSp, arrowCircle);

        // Bottom service watch profile strip
        HBox profileRow = new HBox(12);
        profileRow.setAlignment(Pos.CENTER_LEFT);
        profileRow.setPadding(new Insets(2, 4, 2, 4));

        StackPane avatarBadge = new StackPane(UITheme.brandLogoMark(18, false));
        avatarBadge.setPrefSize(34, 34);
        avatarBadge.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8;");
        Circle greenDot = new Circle(4, Color.web(UITheme.POS_DOT));
        StackPane.setAlignment(greenDot, Pos.TOP_RIGHT);
        StackPane.setMargin(greenDot, new Insets(-2, -2, 0, 0));
        avatarBadge.getChildren().add(greenDot);

        Label profName = medLabel("Production Watch", 15, "#000000");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        StackPane bell = UITheme.bellIcon(16, Color.web("#52525B"));

        profileRow.getChildren().addAll(avatarBadge, profName, sp, bell);
        shell.getChildren().addAll(queryBar, profileRow);

        StackPane.setAlignment(shell, Pos.BOTTOM_CENTER);
        stage.getChildren().addAll(routeChip, confirmChip, shell);

        String[] states = {
            "PaymentGateway NPE triaged in 1.4s",
            "HikariPool timeout resolved in 1.8s",
            "JWT clock-skew race isolated in 0.9s"
        };
        final int[] idx = {0};
        heroCycleTimeline = new Timeline(new KeyFrame(Duration.seconds(3.6), ev -> {
            idx[0] = (idx[0] + 1) % states.length;
            lblActiveTrace.setText(states[idx[0]]);
        }));
        heroCycleTimeline.setCycleCount(Timeline.INDEFINITE);
        heroCycleTimeline.play();

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

        HBox track = new HBox(56);
        track.setAlignment(Pos.CENTER_LEFT);
        track.setPadding(new Insets(0, 28, 0, 28));
        track.setMinHeight(78);
        track.setPrefHeight(78);

        for (int pass = 0; pass < 3; pass++) {
            for (String item : items) {
                Label lbl = new Label(item);
                lbl.setMinWidth(Region.USE_PREF_SIZE);
                lbl.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: #A1A1AA;");
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
        marqueeTransition.setInterpolator(javafx.animation.Interpolator.LINEAR);
        marqueeTransition.play();

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

        VBox perks = new VBox(16);
        perks.setPadding(new Insets(12, 0, 0, 0));
        perks.getChildren().addAll(
            featureHighlightItem(UITheme.terminalIcon(18, Color.BLACK), "Quick LLM triage", "Isolate failing component, error type, and fix steps in under 2 seconds."),
            UITheme.dottedDivider(340),
            featureHighlightItem(UITheme.shieldIcon(18, Color.BLACK), "6-Agent deep dive", "Full root cause report saved directly to your PostgreSQL / MySQL audit trail.")
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
        aaCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20;");
        aaCard.setEffect(UITheme.softCardShadow());

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
        composerPill.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 999; -fx-border-color: #E4E4E7; -fx-border-radius: 999;");
        HBox.setHgrow(composerPill, Priority.ALWAYS);

        Label composerPrompt = new Label("Analyze logs");
        composerPrompt.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #18181B;");
        Region cpSp = spacerRegion();
        StackPane upBtn = new StackPane(UITheme.svgStrokeIcon("M12 19V5 M6 11l6-6 6 6", 12, Color.web("#71717A"), 1.8));
        upBtn.setPrefSize(24, 24);
        upBtn.setStyle("-fx-background-color: #F4F4F5; -fx-background-radius: 999;");
        composerPill.getChildren().addAll(composerPrompt, cpSp, upBtn);
        composerPill.setCursor(Cursor.HAND);
        composerPill.setOnMouseClicked(e -> onOpenAuth.accept(false));

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
        StackPane badge = new StackPane(icon);
        badge.setPrefSize(40, 40);
        badge.setMinSize(40, 40);
        badge.setStyle(
            "-fx-background-color: rgba(255,255,255,0.65);" +
            "-fx-border-color: #CFE6FC; -fx-border-width: 1.5;" +
            "-fx-border-radius: 10; -fx-background-radius: 10;"
        );

        VBox texts = new VBox(2);
        Label t = medLabel(title, 15, "#000000");
        Label d = new Label(desc);
        d.setWrapText(true);
        d.setStyle("-fx-font-size: 13.5px; -fx-text-fill: rgba(0,0,0,0.65);");
        texts.getChildren().addAll(t, d);

        row.getChildren().addAll(badge, texts);
        return row;
    }

    // ── Feature 2: Disruption Alternatives 5-Card Strip (live-03) ─

    private VBox buildFeature2DisruptionSection() {
        VBox band = new VBox(34);
        band.setAlignment(Pos.CENTER);
        band.setPadding(new Insets(96, 56, 96, 56));
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
        panel.setPadding(new Insets(44, 28, 44, 28));
        panel.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #DFECFE 0%, #96CBFA 42%, #E388F2 76%, #FF8C51 100%);" +
            "-fx-background-radius: 28;"
        );

        Label panelCaption = new Label("Always monitoring.\nAlways ready with alternatives.");
        panelCaption.setTextAlignment(TextAlignment.CENTER);
        panelCaption.setStyle("-fx-font-size: 20px; -fx-font-weight: 500; -fx-text-fill: #000000;");

        // 5 side-by-side vertical cards matching live-03
        HBox cardsRow = new HBox(12);
        cardsRow.setAlignment(Pos.CENTER);
        cardsRow.getChildren().addAll(
            disruptionCard("Incident #402", "Checkout API", "30s timeout", true),
            disruptionCard("Fix option", "Pool Guard", "2m rollout", false),
            disruptionCard("Fix option", "Retry Backoff", "1m config", false),
            disruptionCard("Fix option", "Circuit Breaker", "3m patch", false),
            disruptionCard("Fix option", "Read Replica", "Auto-route", false)
        );

        panel.getChildren().addAll(panelCaption, cardsRow);
        band.getChildren().addAll(header, panel);
        return band;
    }

    private VBox disruptionCard(String subTxt, String titleTxt, String metaTxt, boolean isDisrupted) {
        VBox card = new VBox(12);
        card.setPrefWidth(178);
        card.setMinWidth(165);
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
            strip.setStyle(
                "-fx-background-color: rgba(255, 255, 255, " + alphas[i] + ");" +
                "-fx-background-radius: 14;"
            );

            StackPane coralDot = new StackPane(UITheme.checkMarkIcon(Color.WHITE, 0.72));
            coralDot.setPrefSize(22, 22);
            coralDot.setMinSize(22, 22);
            coralDot.setStyle("-fx-background-color: #FF897F; -fx-background-radius: 999;");

            Label lbl = medLabel(items[i], 14, "#000000");
            strip.getChildren().addAll(coralDot, lbl);
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
        frame.setCache(true);
        frame.setCacheHint(CacheHint.SPEED);

        // Left white sidebar inside preview
        VBox miniRail = new VBox(14);
        miniRail.setPrefWidth(196);
        miniRail.setPadding(new Insets(18, 14, 18, 14));
        miniRail.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 18 0 0 0;");

        HBox railTop = new HBox(UITheme.brandLogoMark(16, false), spacerRegion(), UITheme.inboxIcon(14, Color.web("#71717A")));
        Label newReport = medLabel("+  New incident", 12, "#000000");
        newReport.setCursor(Cursor.HAND);
        newReport.setOnMouseClicked(e -> onOpenAuth.accept(false));

        VBox recents = new VBox(7);
        Label recHead = new Label("Recents");
        recHead.setStyle("-fx-font-size: 11px; -fx-text-fill: #A1A1AA;");
        Label r1 = new Label("PaymentGateway NPE");
        Label r2 = new Label("HikariPool timeout");
        Label r3 = new Label("JWT token refresh race");
        for (Label r : new Label[]{r1, r2, r3}) {
            r.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #3F3F46;");
        }
        recents.getChildren().addAll(recHead, r1, r2, r3);
        miniRail.getChildren().addAll(railTop, newReport, recents);

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

        HBox banner = new HBox(8, UITheme.boltIcon(14, Color.web("#1B98F2")), medLabel("Inspect Incident #402 diagnosis", 12, "#18181B"), spacerRegion());
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setMaxWidth(420);
        banner.setPadding(new Insets(9, 14, 9, 14));
        banner.setStyle("-fx-background-color: rgba(255,255,255,0.68); -fx-background-radius: 10;");

        VBox promptBox = new VBox(18);
        promptBox.setMaxWidth(450);
        promptBox.setPadding(new Insets(16));
        promptBox.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 14;");
        promptBox.setCursor(Cursor.HAND);
        promptBox.setOnMouseClicked(e -> onOpenAuth.accept(false));
        Label promptTxt = new Label("Ask anything or open real workspace...");
        promptTxt.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #71717A;");
        HBox promptBottom = new HBox(new Label("+"), spacerRegion(), UITheme.serkoBlackPill("Open app", false));
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
        btn.setStyle(
            "-fx-background-color: #FFFFFF;" +
            "-fx-border-color: rgba(0,0,0,0.07);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 9 9 3 9;" +
            "-fx-background-radius: 9 9 3 9;"
        );
        DropShadow ds = new DropShadow();
        ds.setColor(Color.rgb(0, 0, 0, 0.06));
        ds.setRadius(4);
        ds.setOffsetY(1);
        btn.setEffect(ds);

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
        HBox dividerBox = new HBox(UITheme.dottedDivider(1160));
        dividerBox.setAlignment(Pos.CENTER);
        dividerBox.setPadding(new Insets(8, 56, 8, 56));

        VBox footer = new VBox(28);
        footer.setPadding(new Insets(48, 64, 38, 64));

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

        HBox rightCols = new HBox(56);
        rightCols.setAlignment(Pos.TOP_LEFT);
        rightCols.getChildren().addAll(
            footerColumn("Explore", List.of(
                footerLink("Home",      () -> navigateTo(Page.HOME)),
                footerLink("Platform",  () -> navigateTo(Page.PLATFORM)),
                footerLink("For teams", () -> navigateTo(Page.FOR_TEAMS)),
                footerLink("About",     () -> navigateTo(Page.ABOUT))
            )),
            footerColumn("Capabilities", List.of(
                footerLink("6-Agent pipeline", () -> navigateTo(Page.PLATFORM)),
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

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(138, 56, 48, 56));
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
        ctas.setPadding(new Insets(4, 0, 24, 0));

        // Interactive Platform Composer Card matching live-08-platform.png
        VBox composerCard = new VBox(14);
        composerCard.setMaxWidth(580);
        composerCard.setPadding(new Insets(22));
        composerCard.setStyle(
            "-fx-background-color: #FFFFFF; -fx-background-radius: 18;" +
            "-fx-border-color: rgba(0,0,0,0.06); -fx-border-radius: 18;"
        );
        composerCard.setEffect(UITheme.softCardShadow());

        HBox topFields = new HBox(24);
        VBox f1 = new VBox(4, microCap("SERVICE / MODULE"), medLabel("checkout-api · PaymentLedgerDao", 13, "#71717A"));
        VBox f2 = new VBox(4, microCap("SEVERITY"), medLabel("Critical · Open", 13, "#71717A"));
        HBox.setHgrow(f1, Priority.ALWAYS);
        topFields.getChildren().addAll(f1, f2);

        Line sep = new Line(0, 0, 530, 0);
        sep.setStroke(Color.web("#F4F4F5"));

        VBox detailsBlock = new VBox(8);
        detailsBlock.getChildren().add(microCap("ERROR LOG DETAILS AND CONTEXT"));

        HBox tokenPreview = new HBox(6);
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

        HBox bottomActionRow = new HBox(spacerRegion(), UITheme.serkoWhitePill("Analyze trace", false));
        ((Button) bottomActionRow.getChildren().get(0)).setOnAction(e -> onOpenAuth.accept(false));
        detailsBlock.getChildren().addAll(tokenPreview, bottomActionRow);

        composerCard.getChildren().addAll(topFields, sep, detailsBlock);

        String[][] tokenSamples = {
            {"SQLTransientConnectionException", "HikariPool-1"},
            {"ExpiredJwtException", "/api/incidents/my"},
            {"NullPointerException", "PaymentGateway.authorize"}
        };
        final int[] sIdx = {0};
        typewriterTimeline = new Timeline(new KeyFrame(Duration.seconds(3.2), ev -> {
            sIdx[0] = (sIdx[0] + 1) % tokenSamples.length;
            tErr.setText(tokenSamples[sIdx[0]][0]);
            tMod.setText(tokenSamples[sIdx[0]][1]);
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

        for (String[] st : stages) {
            Button sb = new Button(st[0]);
            sb.setMaxWidth(Double.MAX_VALUE);
            sb.setAlignment(Pos.CENTER_LEFT);
            sb.setStyle("-fx-background-color: #F6F6F6; -fx-text-fill: #000000; -fx-font-weight: 500; -fx-font-size: 13.5px; -fx-padding: 13 18; -fx-background-radius: 12; -fx-cursor: hand;");
            sb.setOnAction(e -> {
                detStage.setText(st[1]);
                detTitle.setText(st[0]);
                detDesc.setText(st[2]);
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
        l.setStyle("-fx-font-size: 10px; -fx-font-weight: 600; -fx-text-fill: #9CA3AF; -fx-letter-spacing: 0.6px;");
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
        c.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16; -fx-border-color: rgba(0,0,0,0.06); -fx-border-radius: 16;");
        c.setEffect(UITheme.softCardShadow());
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
        c.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 20;");
        HBox.setHgrow(c, Priority.ALWAYS);
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
            UITheme.dottedDivider(680),
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

    // ── Shared Helpers ──────────────────────────────────────────

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
