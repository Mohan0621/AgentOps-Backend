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
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

/**
 * Full public-facing website shell matching https://www.serko.ai/ (Home, Platform, For Teams, About).
 * Uses hybrid JS HTML5 Canvas (SerkoHeroGlobe) + pure JavaFX editorial UI components.
 */
public class PublicWebsiteView extends StackPane {

    public enum Page { HOME, PLATFORM, FOR_TEAMS, ABOUT }

    private final Consumer<Boolean> onOpenAuth; // true = Register, false = Login
    private final Runnable onOpenWorkspaceIfLoggedIn;
    private final ScrollPane scrollPane;
    private final VBox pageContainer;

    private HBox navbar;
    private Button navHome, navPlatform, navTeams, navAbout;
    private Button navAuthLogin, navAuthStart;

    private Page currentPage = Page.HOME;
    private boolean scrolledPastTop = false;
    private double faqAnchorFraction = 0.78;
    private Timeline heroCycleTimeline;
    private Timeline typewriterTimeline;
    private Timeline marqueeTimeline;
    private TranslateTransition marqueeTransition;

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

        navbar = buildNavbar();
        StackPane.setAlignment(navbar, Pos.TOP_CENTER);

        getChildren().addAll(scrollPane, navbar);

        scrollPane.vvalueProperty().addListener((obs, oldV, newV) -> {
            boolean shouldElevate = newV.doubleValue() > 0.02;
            if (shouldElevate != scrolledPastTop) {
                scrolledPastTop = shouldElevate;
                updateNavbarBackdrop();
            }
        });

        navigateTo(Page.HOME);
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
            FadeTransition ft = new FadeTransition(Duration.millis(260), pageContainer);
            ft.setFromValue(0.35);
            ft.setToValue(1.0);
            ft.play();
        });
    }

    public void refreshAuthButtons(boolean loggedIn) {
        if (loggedIn) {
            navAuthLogin.setText("Workspace");
            navAuthLogin.setOnAction(e -> onOpenWorkspaceIfLoggedIn.run());
            navAuthStart.setText("Open App");
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
        if (marqueeTimeline != null) marqueeTimeline.stop();
        if (marqueeTransition != null) marqueeTransition.stop();
    }

    // ── Floating Top Navigation Bar (Serko nav1_wrap) ───────────

    private HBox buildNavbar() {
        HBox bar = new HBox(0);
        bar.setAlignment(Pos.CENTER);
        bar.setPrefHeight(68);
        bar.setMaxHeight(68);
        bar.setPadding(new Insets(0, 36, 0, 36));

        // Left: Nav Menu Items
        navHome     = makeNavLink("Home",      () -> navigateTo(Page.HOME));
        navPlatform = makeNavLink("Platform",  () -> navigateTo(Page.PLATFORM));
        navTeams    = makeNavLink("For teams", () -> navigateTo(Page.FOR_TEAMS));
        navAbout    = makeNavLink("About",     () -> navigateTo(Page.ABOUT));

        HBox leftLinks = new HBox(4, navHome, navPlatform, navTeams, navAbout);
        leftLinks.setAlignment(Pos.CENTER_LEFT);
        leftLinks.setMinWidth(320);

        // Center: Serko-style Logo Mark + Wordmark
        HBox brandBox = new HBox(10);
        brandBox.setAlignment(Pos.CENTER);
        brandBox.setCursor(Cursor.HAND);
        brandBox.setOnMouseClicked(e -> navigateTo(Page.HOME));

        StackPane mark = UITheme.brandLogoMark(24, false);
        Label wordmark = new Label("IncidentIQ");
        wordmark.setStyle(
            "-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #000000; -fx-letter-spacing: -0.3px;"
        );
        brandBox.getChildren().addAll(mark, wordmark);

        // Right: Log in + Get started pill
        navAuthLogin = new Button("Log in");
        navAuthLogin.getStyleClass().add("serko-nav-link");
        navAuthLogin.setOnAction(e -> onOpenAuth.accept(false));

        navAuthStart = UITheme.serkoBlackPill("Get started", false);
        navAuthStart.setPadding(new Insets(8, 18, 8, 18));
        navAuthStart.setOnAction(e -> onOpenAuth.accept(true));

        HBox rightActions = new HBox(8, navAuthLogin, navAuthStart);
        rightActions.setAlignment(Pos.CENTER_RIGHT);
        rightActions.setMinWidth(320);

        Region s1 = new Region(); HBox.setHgrow(s1, Priority.ALWAYS);
        Region s2 = new Region(); HBox.setHgrow(s2, Priority.ALWAYS);

        bar.getChildren().addAll(leftLinks, s1, brandBox, s2, rightActions);
        this.navbar = bar;
        updateNavbarBackdrop();
        return bar;
    }

    private Button makeNavLink(String text, Runnable action) {
        Button btn = new Button(text);
        btn.getStyleClass().add("serko-nav-link");
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private void updateActiveNav() {
        for (Button b : List.of(navHome, navPlatform, navTeams, navAbout)) {
            b.getStyleClass().remove("serko-nav-link-active");
        }
        Button active = switch (currentPage) {
            case HOME      -> navHome;
            case PLATFORM  -> navPlatform;
            case FOR_TEAMS -> navTeams;
            case ABOUT     -> navAbout;
        };
        if (!active.getStyleClass().contains("serko-nav-link-active")) {
            active.getStyleClass().add("serko-nav-link-active");
        }
    }

    private void updateNavbarBackdrop() {
        if (navbar == null) return;
        if (scrolledPastTop) {
            navbar.setStyle(
                "-fx-background-color: #FFFFFF;" +
                "-fx-border-color: transparent transparent rgba(0, 0, 0, 0.08) transparent;" +
                "-fx-border-width: 0 0 1 0;"
            );
            DropShadow ds = new DropShadow();
            ds.setColor(Color.rgb(0, 0, 0, 0.05));
            ds.setRadius(14);
            ds.setOffsetY(3);
            navbar.setEffect(ds);
        } else {
            navbar.setStyle(
                "-fx-background-color: rgba(229, 242, 255, 0.45);" +
                "-fx-border-color: transparent;"
            );
            navbar.setEffect(null);
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  PAGE 1: HOME LANDING (Matches https://www.serko.ai/)
    // ═════════════════════════════════════════════════════════════

    private void buildHomePage() {
        faqAnchorFraction = 0.77;
        pageContainer.getChildren().addAll(
            buildHomeHeroSection(),
            buildTechMarqueeStrip(),
            buildFeature1IceSection(),
            buildFeature2DisruptionSection(),
            buildFeature3SmartPillsSection(),
            buildFeature4WorkspacePreviewSection(),
            buildClosingCtaSection("Start investigating", "Got an outage piling up?", "Get it sorted with ", "IncidentIQ.",
                "Skip manual stack-trace grepping. Paste your logs and let 6 autonomous agents isolate root cause and draft the fix."),
            buildFaqSection(defaultHomeFaqs()),
            buildDarkFooter()
        );
    }

    private StackPane buildHomeHeroSection() {
        StackPane hero = new StackPane();
        hero.setMinHeight(756);
        hero.setPrefHeight(756);
        hero.setAlignment(Pos.TOP_CENTER);

        // Background: JS HTML5 Canvas 3D Globe + Sky Horizon Gradient
        SerkoHeroGlobe globeBackdrop = new SerkoHeroGlobe();

        // Foreground Editorial Content + Floating Card
        VBox content = new VBox(16);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(106, 48, 32, 48));
        content.setMaxWidth(980);

        Label eyebrow = UITheme.eyebrowLabel("AI incident agent");
        TextFlow titleFlow = UITheme.gradientAccentFlow(
            "The last incident ", "agent", "\nyou’ll ever need.", 54, TextAlignment.CENTER, true
        );
        titleFlow.setMaxWidth(760);

        Label sub = new Label(
            "IncidentIQ learns how your systems fail, isolates root cause in seconds,\n" +
            "and orchestrates 6 autonomous agents from alert to resolution."
        );
        sub.setTextAlignment(TextAlignment.CENTER);
        sub.setWrapText(true);
        sub.setMinHeight(Region.USE_PREF_SIZE);
        sub.setMaxWidth(620);
        sub.setStyle("-fx-text-fill: rgba(0, 0, 0, 0.70); -fx-font-size: 16.5px; -fx-line-spacing: 3px;");

        Button btnStart = UITheme.serkoBlackPill("Start investigating", true);
        btnStart.setPadding(new Insets(12, 26, 12, 26));
        btnStart.setOnAction(e -> onOpenAuth.accept(true));

        Button btnFaq = UITheme.serkoWhitePill("FAQs", true);
        btnFaq.setPadding(new Insets(12, 24, 12, 24));
        btnFaq.setOnAction(e -> scrollPane.setVvalue(faqAnchorFraction));

        HBox ctaRow = new HBox(12, btnStart, btnFaq);
        ctaRow.setAlignment(Pos.CENTER);
        ctaRow.setPadding(new Insets(4, 0, 12, 0));

        // Signature Serko Floating Hero Profile & Live Investigation Widget
        VBox floatingWidget = buildHeroInteractiveCard();
        content.getChildren().addAll(eyebrow, titleFlow, sub, ctaRow, floatingWidget);

        hero.getChildren().addAll(globeBackdrop, content);
        return hero;
    }

    private VBox buildHeroInteractiveCard() {
        VBox shell = new VBox(14);
        shell.setMaxWidth(660);
        shell.setPadding(new Insets(20));
        shell.setStyle(
            "-fx-background-color: rgba(246, 246, 246, 0.94);" +
            "-fx-background-radius: 22;" +
            "-fx-border-color: rgba(255, 255, 255, 0.90);" +
            "-fx-border-radius: 22;" +
            "-fx-border-width: 1.5;"
        );
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.10));
        shadow.setRadius(38);
        shadow.setOffsetY(14);
        shell.setEffect(shadow);

        // Top Profile / Live Service Header Row (like "Paula's profile" on Serko)
        HBox topProfileRow = new HBox(12);
        topProfileRow.setAlignment(Pos.CENTER_LEFT);

        StackPane avatarBadge = new StackPane();
        avatarBadge.setPrefSize(38, 38);
        avatarBadge.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10;");
        Circle greenDot = new Circle(4.5, Color.web(UITheme.POS_DOT));
        StackPane.setAlignment(greenDot, Pos.TOP_RIGHT);
        StackPane.setMargin(greenDot, new Insets(-2, -2, 0, 0));
        javafx.scene.shape.SVGPath bolt = new javafx.scene.shape.SVGPath();
        bolt.setContent("M13 2L4 14h6l-1 8 9-12h-6l1-8z");
        bolt.setFill(Color.BLACK);
        bolt.setScaleX(0.82);
        bolt.setScaleY(0.82);
        avatarBadge.getChildren().addAll(bolt, greenDot);

        VBox profTitles = new VBox(2);
        Label profName = new Label("Production Watch · Live Triage");
        profName.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #000000;");
        Label profSub = new Label("Auto-monitoring checkout-api, auth-service & payment-worker");
        profSub.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #71717A;");
        profTitles.getChildren().addAll(profName, profSub);

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label statusChip = UITheme.positiveBadge("6 Agents Ready");
        topProfileRow.getChildren().addAll(avatarBadge, profTitles, sp, statusChip);

        // Animated Live Step Pills inside white card (matching hero1_profile_choice_wrap)
        HBox queryBar = new HBox(10);
        queryBar.setAlignment(Pos.CENTER_LEFT);
        queryBar.setPadding(new Insets(13, 16, 13, 16));
        queryBar.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 14; -fx-border-color: rgba(0,0,0,0.05); -fx-border-radius: 14;");

        StackPane checkCircle = new StackPane(UITheme.checkMarkIcon(Color.WHITE, 0.85));
        checkCircle.setPrefSize(22, 22);
        checkCircle.setStyle("-fx-background-color: #158142; -fx-background-radius: 999;");

        Label lblActiveTrace = new Label("NullPointerException at PaymentGateway.authorize:418");
        lblActiveTrace.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 600; -fx-text-fill: #000000;");

        Region qSp = new Region(); HBox.setHgrow(qSp, Priority.ALWAYS);
        Label lblDuration = new Label("Diagnosed in 1.4s");
        lblDuration.setStyle("-fx-font-size: 11.5px; -fx-font-weight: bold; -fx-text-fill: #1B98F2;");

        queryBar.getChildren().addAll(checkCircle, lblActiveTrace, qSp, lblDuration);

        // Two clean summary pill cards underneath (like Flight + Hotel confirmed row)
        HBox detailCards = new HBox(10);
        Label[] leftRefs = new Label[2]; // [0]=pill, [1]=body
        VBox leftSub = heroMiniCard("ROOT CAUSE AGENT", "Missing null guard on Stripe token metadata", "CRITICAL -> FIXED", false, leftRefs);
        Label[] rightRefs = new Label[2];
        VBox rightSub = heroMiniCard("RESOLUTION AGENT", "Added idempotency fallback + unit test verified", "Confirmed", true, rightRefs);
        HBox.setHgrow(leftSub, Priority.ALWAYS);
        HBox.setHgrow(rightSub, Priority.ALWAYS);
        detailCards.getChildren().addAll(leftSub, rightSub);

        shell.getChildren().addAll(topProfileRow, queryBar, detailCards);

        // Cycle simulated traces dynamically just like Serko's live hero showcase
        String[][] states = {
            {"NullPointerException at PaymentGateway.authorize:418", "Diagnosed in 1.4s", "Missing null guard on Stripe token metadata", "CRITICAL -> FIXED"},
            {"HikariPool-1 - Connection is not available (30s timeout)", "Triaged in 1.8s", "Unclosed transaction holding DB pool connections", "HIGH -> PATCHED"},
            {"JWT ExpiredJwtException across /api/incidents/my", "Resolved in 0.9s", "Clock skew + missing refresh grace window detected", "MEDIUM -> VERIFIED"}
        };
        final int[] idx = {0};
        heroCycleTimeline = new Timeline(new KeyFrame(Duration.seconds(3.6), ev -> {
            idx[0] = (idx[0] + 1) % states.length;
            String[] s = states[idx[0]];
            lblActiveTrace.setText(s[0]);
            lblDuration.setText(s[1]);
            leftRefs[1].setText(s[2]);
            leftRefs[0].setText(s[3]);
        }));
        heroCycleTimeline.setCycleCount(Timeline.INDEFINITE);
        heroCycleTimeline.play();

        return shell;
    }

    private VBox heroMiniCard(String tag, String title, String pill, boolean positivePill, Label[] refs) {
        VBox c = new VBox(6);
        c.setPadding(new Insets(12, 14, 12, 14));
        c.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 14; -fx-border-color: rgba(0,0,0,0.04); -fx-border-radius: 14;");

        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);
        Label tLbl = new Label(tag);
        tLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #71717A;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label pLbl = positivePill ? UITheme.positiveBadge(pill) : UITheme.statusBadge("INVESTIGATING");
        pLbl.setText(pill);
        top.getChildren().addAll(tLbl, sp, pLbl);

        Label body = new Label(title);
        body.setWrapText(true);
        body.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 600; -fx-text-fill: #111827;");
        if (refs != null && refs.length >= 2) {
            refs[0] = pLbl;
            refs[1] = body;
        }
        c.getChildren().addAll(top, body);
        return c;
    }

    // ── Smooth Infinite Tech Marquee Strip ──────────────────────

    private StackPane buildTechMarqueeStrip() {
        StackPane wrap = new StackPane();
        wrap.setMinHeight(74);
        wrap.setPrefHeight(74);
        wrap.setMaxHeight(74);
        wrap.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: transparent transparent #F1F1F4 transparent;");
        Rectangle clipRect = new Rectangle();
        clipRect.widthProperty().bind(wrap.widthProperty());
        clipRect.heightProperty().bind(wrap.heightProperty());
        wrap.setClip(clipRect);

        String[] items = {
            "Spring Boot 3.3", "Groq LPU Inference", "Gemini 1.5 AI", "6-Agent Pipeline",
            "Spring Security JWT", "MySQL Telemetry", "JavaFX 21 Desktop", "Sub-2s Root Cause"
        };

        HBox track = new HBox(48);
        track.setAlignment(Pos.CENTER_LEFT);
        track.setPadding(new Insets(0, 24, 0, 24));
        track.setMinHeight(74);
        track.setPrefHeight(74);

        for (int pass = 0; pass < 3; pass++) {
            for (String item : items) {
                HBox chip = new HBox(8);
                chip.setAlignment(Pos.CENTER_LEFT);
                chip.setMinWidth(Region.USE_PREF_SIZE);

                Circle dot = new Circle(2.5, Color.web("#D1D5DB"));
                Label lbl = new Label(item);
                lbl.setMinWidth(Region.USE_PREF_SIZE);
                lbl.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #9CA3AF;");
                chip.getChildren().addAll(dot, lbl);
                track.getChildren().add(chip);
            }
        }

        // Unconstrained Pane host lets track grow to its full natural width (~4200px) without expanding ScrollPane width
        javafx.scene.layout.Pane trackHost = new javafx.scene.layout.Pane(track);
        trackHost.setMinWidth(0);
        trackHost.setPrefHeight(74);
        trackHost.setMaxWidth(Double.MAX_VALUE);

        Region leftFade = new Region();
        leftFade.setMaxWidth(96);
        leftFade.setPrefWidth(96);
        leftFade.setMouseTransparent(true);
        leftFade.setStyle("-fx-background-color: linear-gradient(to right, #FFFFFF 0%, rgba(255,255,255,0) 100%);");
        StackPane.setAlignment(leftFade, Pos.CENTER_LEFT);

        Region rightFade = new Region();
        rightFade.setMaxWidth(96);
        rightFade.setPrefWidth(96);
        rightFade.setMouseTransparent(true);
        rightFade.setStyle("-fx-background-color: linear-gradient(to left, #FFFFFF 0%, rgba(255,255,255,0) 100%);");
        StackPane.setAlignment(rightFade, Pos.CENTER_RIGHT);

        marqueeTransition = new TranslateTransition(Duration.seconds(26), track);
        marqueeTransition.setFromX(0);
        marqueeTransition.setToX(-1220);
        marqueeTransition.setCycleCount(Timeline.INDEFINITE);
        marqueeTransition.setInterpolator(javafx.animation.Interpolator.LINEAR);
        marqueeTransition.play();

        wrap.getChildren().addAll(trackHost, leftFade, rightFade);
        return wrap;
    }

    // ── Feature 1: Ice Sky Section (Matches u-background-3) ─────

    private VBox buildFeature1IceSection() {
        VBox band = new VBox();
        band.setAlignment(Pos.CENTER);
        band.setPadding(new Insets(84, 56, 84, 56));
        band.setStyle("-fx-background-color: " + UITheme.BG_ICE + ";");

        GridPane grid = twoColumnSectionGrid(44);

        // Left Copy
        VBox left = new VBox(20);
        left.setAlignment(Pos.CENTER_LEFT);

        Label eyebrow = UITheme.eyebrowLabel("Instant AI Diagnosis");
        Label h2 = new Label("Debug like it's already\nsolved. Because it is.");
        h2.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #000000; -fx-line-spacing: -2px;");

        Label body = new Label(
            "Stop reading 800-line stack traces manually. IncidentIQ dissects exceptions automatically " +
            "and returns pinpointed root cause, impacted modules, and verified remediation steps."
        );
        body.setWrapText(true);
        body.setStyle("-fx-text-fill: rgba(0,0,0,0.70); -fx-font-size: 15.5px; -fx-line-spacing: 4px;");

        VBox perks = new VBox(16);
        perks.setPadding(new Insets(10, 0, 0, 0));
        perks.getChildren().addAll(
            featureHighlightItem("Quick LLM Triage", "Isolate error class, severity, and root cause in under 2 seconds."),
            UITheme.dottedDivider(360),
            featureHighlightItem("6-Agent Deep Dive", "Full automated markdown analysis saved permanently to audit logs.")
        );

        left.getChildren().addAll(eyebrow, h2, body, perks);

        // Right Showcase Visual Card (Chat-to-Fix Simulation like Serko's Cancellation visual)
        VBox visualOuter = new VBox(14);
        visualOuter.setPadding(new Insets(26));
        visualOuter.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, rgba(255,255,255,0.92), rgba(244,137,173,0.22), rgba(227,136,242,0.24));" +
            "-fx-background-radius: 26;" +
            "-fx-border-color: rgba(255,255,255,0.9);" +
            "-fx-border-radius: 26;"
        );
        visualOuter.setEffect(UITheme.softCardShadow());

        // User chat bubble right
        HBox userMsgRow = new HBox();
        userMsgRow.setAlignment(Pos.CENTER_RIGHT);
        Label userBubble = new Label("Diagnose Incident #402 — Checkout Timeout");
        userBubble.setStyle(
            "-fx-background-color: #000000; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-font-weight: 600;" +
            "-fx-padding: 10 16; -fx-background-radius: 16 16 4 16;"
        );
        userMsgRow.getChildren().add(userBubble);

        // AI response card left
        VBox aiCard = new VBox(12);
        aiCard.setPadding(new Insets(18));
        aiCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 18;");

        HBox aiHead = new HBox(8);
        aiHead.setAlignment(Pos.CENTER_LEFT);
        Label activeBadge = UITheme.positiveBadge("Active on your account");
        Label agentName = new Label("IncidentIQ Multi-Agent Core™");
        agentName.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #71717A;");
        aiHead.getChildren().addAll(activeBadge, agentName);

        Label aiResp = new Label("On it. Deadlock trace isolated in CheckoutService.processOrder(). Patch & rollback options ready.");
        aiResp.setWrapText(true);
        aiResp.setStyle("-fx-font-size: 13.5px; -fx-text-fill: #111827; -fx-font-weight: 500;");

        // Interactive Toggle Detail
        VBox traceCard = new VBox(8);
        traceCard.setPadding(new Insets(12, 14, 12, 14));
        traceCard.setStyle("-fx-background-color: #F6F6F6; -fx-background-radius: 12;");
        Label traceMeta = new Label("PaymentGateway  →  CheckoutService.java:142");
        traceMeta.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #000000;");
        Label fixMeta = new Label("Confidence 98%  ·  Fix Verified");
        fixMeta.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #158142; -fx-font-weight: bold;");
        traceCard.getChildren().addAll(traceMeta, fixMeta);

        Button btnAction = UITheme.serkoRainbowPill("Run 6-Agent Investigation", true);
        btnAction.setMaxWidth(Double.MAX_VALUE);
        btnAction.setOnAction(e -> onOpenAuth.accept(false));

        aiCard.getChildren().addAll(aiHead, aiResp, traceCard, btnAction);
        visualOuter.getChildren().addAll(userMsgRow, aiCard);

        grid.addRow(0, left, visualOuter);
        band.getChildren().add(grid);
        return band;
    }

    private HBox featureHighlightItem(String title, String desc) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        StackPane badge = new StackPane(UITheme.chevronRightIcon(Color.web(UITheme.GRAD_BLUE), 0.78));
        badge.setPrefSize(38, 38);
        badge.setMinSize(38, 38);
        badge.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12;");

        VBox texts = new VBox(2);
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 14.5px; -fx-font-weight: bold; -fx-text-fill: #000000;");
        Label d = new Label(desc);
        d.setWrapText(true);
        d.setStyle("-fx-font-size: 13px; -fx-text-fill: #4D4D4D;");
        texts.getChildren().addAll(t, d);

        row.getChildren().addAll(badge, texts);
        return row;
    }

    // ── Feature 2: Disruption Management Card (#FFFFFF) ─────────

    private VBox buildFeature2DisruptionSection() {
        VBox band = new VBox(34);
        band.setAlignment(Pos.CENTER);
        band.setPadding(new Insets(92, 56, 92, 56));
        band.setStyle("-fx-background-color: #FFFFFF;");

        VBox header = new VBox(12);
        header.setAlignment(Pos.CENTER);
        header.setMaxWidth(680);

        Label eyebrow = UITheme.eyebrowLabel("Disruption management");
        Label h2 = new Label("With you all the way,\nfrom booking the fix to deploy.");
        h2.setTextAlignment(TextAlignment.CENTER);
        h2.setStyle("-fx-font-size: 38px; -fx-font-weight: bold; -fx-text-fill: #000000;");

        Label sub = new Label(
            "Most monitoring tools leave engineers alone with raw alerts. IncidentIQ doesn't. " +
            "Production failing? It correlates stack logs and surfaces ranked fixes ready for action."
        );
        sub.setWrapText(true);
        sub.setTextAlignment(TextAlignment.CENTER);
        sub.setStyle("-fx-text-fill: rgba(0,0,0,0.70); -fx-font-size: 15.5px;");
        header.getChildren().addAll(eyebrow, h2, sub);

        // Centered Horizon Backdrop Card containing Crisp Itinerary-Style Incident Timeline Card
        StackPane panel = new StackPane();
        panel.setMaxWidth(880);
        panel.setPadding(new Insets(36));
        panel.setStyle(
            "-fx-background-color: radial-gradient(center 50% 60%, radius 75%, #FF8C51 0%, #F489AD 25%, #96CBFA 65%, #E8F3FE 100%);" +
            "-fx-background-radius: 28;"
        );

        VBox innerLayout = new VBox(18);
        innerLayout.setAlignment(Pos.CENTER);

        Label panelCaption = new Label("Always monitoring.  Always ready with alternatives.");
        panelCaption.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #000000;");

        // Interactive Card matching Serko's Flight Outbound Card
        VBox card = new VBox(16);
        card.setMaxWidth(520);
        card.setPadding(new Insets(22, 24, 22, 24));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20;");
        card.setEffect(UITheme.softCardShadow());

        HBox cardTop = new HBox(10);
        cardTop.setAlignment(Pos.CENTER_LEFT);
        StackPane heartDot = new StackPane(UITheme.brandLogoMark(18, false));
        Label lblService = new Label("INCIDENT #402 · PAYMENT-API");
        lblService.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #71717A;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label badge = UITheme.positiveBadge("Patch Ready");
        cardTop.getChildren().addAll(heartDot, lblService, sp, badge);

        HBox midRow = new HBox(12);
        midRow.setAlignment(Pos.CENTER_LEFT);
        VBox route = new VBox(2);
        Label rLabel = new Label("Disrupted service");
        rLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #71717A;");
        Label rCity = new Label("Checkout Gateway");
        rCity.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #000000;");
        route.getChildren().addAll(rLabel, rCity);

        Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);
        Label durationLbl = new Label("1.4s  ·  6 Agents Verified");
        durationLbl.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 600; -fx-text-fill: #4D4D4D;");
        midRow.getChildren().addAll(route, sp2, durationLbl);

        Line sep = UITheme.dottedDivider(470);

        HBox bottomRow = new HBox(12);
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        Label fixDesc = new Label("Recommended: Add Connection Pool Timeout Guard");
        fixDesc.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #111827;");
        Region sp3 = new Region(); HBox.setHgrow(sp3, Priority.ALWAYS);
        Button btnSelect = UITheme.serkoBlackPill("Inspect Fix", true);
        btnSelect.setPadding(new Insets(8, 18, 8, 18));
        btnSelect.setOnAction(e -> {
            badge.setText("Applied in Queue");
            btnSelect.setText("Open App");
            btnSelect.setOnAction(ev -> onOpenAuth.accept(false));
        });
        bottomRow.getChildren().addAll(fixDesc, sp3, btnSelect);

        card.getChildren().addAll(cardTop, midRow, sep, bottomRow);
        innerLayout.getChildren().addAll(panelCaption, card);
        panel.getChildren().add(innerLayout);

        band.getChildren().addAll(header, panel);
        return band;
    }

    // ── Feature 3: Smart Capability Pills (Matches feature3_wrap) ─

    private VBox buildFeature3SmartPillsSection() {
        VBox band = new VBox();
        band.setAlignment(Pos.CENTER);
        band.setPadding(new Insets(84, 56, 84, 56));
        band.setStyle("-fx-background-color: #FFFFFF;");

        GridPane grid = twoColumnSectionGrid(48);

        // Left
        VBox left = new VBox(18);
        left.setAlignment(Pos.CENTER_LEFT);
        Label eyebrow = UITheme.eyebrowLabel("Personalized engineering AI");
        Label h2 = new Label("Every investigation,\na little more intelligent.");
        h2.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #000000;");
        Label body = new Label(
            "The more incidents your team resolves, the clearer your operational signals get — " +
            "capturing recurring patterns, high-risk components, and exact remediation patterns."
        );
        body.setWrapText(true);
        body.setStyle("-fx-text-fill: rgba(0,0,0,0.70); -fx-font-size: 15.5px; -fx-line-spacing: 4px;");

        Button btnLearn = UITheme.serkoBlackPill("Learn more about IncidentIQ", true);
        btnLearn.setOnAction(e -> navigateTo(Page.PLATFORM));
        left.getChildren().addAll(eyebrow, h2, body, btnLearn);

        // Right Floating Pill Stack (like "Premium economy", "No layovers", "Hotel Wi-Fi", "Hotel with a gym")
        VBox rightCards = new VBox(12);
        rightCards.setPadding(new Insets(28));
        rightCards.setStyle(
            "-fx-background-color: #F6F6F6;" +
            "-fx-background-radius: 26;"
        );
        Label detailPreview = new Label("Click any capability pill to inspect how the pipeline executes it.");
        detailPreview.setWrapText(true);
        detailPreview.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #71717A; -fx-padding: 6 0 0 4;");

        String[][] pills = {
            {"Groq LPU sub-2s inference", "Runs ultra-fast initial stack-trace classification using llama/gpt-oss models."},
            {"Zero manual log grepping",  "Automatically isolates root exception line numbers and culprit dependencies."},
            {"6-Agent LangGraph flow",    "Six specialized agents pass context sequentially to validate every fix."},
            {"Built-in MTTR telemetry",   "Measures resolution speed and AI coverage directly from PostgreSQL/MySQL."}
        };

        for (String[] p : pills) {
            HBox pill = new HBox(12);
            pill.setAlignment(Pos.CENTER_LEFT);
            pill.setPadding(new Insets(14, 18, 14, 18));
            pill.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16; -fx-cursor: hand;");
            pill.setEffect(UITheme.softCardShadow());

            StackPane check = new StackPane(UITheme.checkMarkIcon(Color.web(UITheme.POS_DOT), 0.9));
            check.setPrefSize(28, 28);
            check.setStyle("-fx-background-color: " + UITheme.POS_BG + "; -fx-background-radius: 999;");

            Label lbl = new Label(p[0]);
            lbl.setStyle("-fx-font-size: 14.5px; -fx-font-weight: 600; -fx-text-fill: #000000;");

            Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
            Label hint = new Label("Active");
            hint.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #9CA3AF;");

            pill.getChildren().addAll(check, lbl, sp, hint);
            pill.setOnMouseClicked(e -> detailPreview.setText("✓  " + p[0] + ": " + p[1]));
            rightCards.getChildren().add(pill);
        }
        rightCards.getChildren().add(detailPreview);

        grid.addRow(0, left, rightCards);
        band.getChildren().add(grid);
        return band;
    }

    // ── Feature 4: "Made for engineering teams" Workspace Preview ─

    private VBox buildFeature4WorkspacePreviewSection() {
        VBox band = new VBox(34);
        band.setAlignment(Pos.CENTER);
        band.setPadding(new Insets(84, 56, 84, 56));
        band.setStyle("-fx-background-color: #FFFFFF;");

        VBox header = new VBox(12);
        header.setAlignment(Pos.CENTER);
        header.setMaxWidth(680);

        Label eyebrow = UITheme.eyebrowLabel("Made for engineering teams");
        Label h2 = new Label("Works for you.\nWorks for your team.");
        h2.setTextAlignment(TextAlignment.CENTER);
        h2.setStyle("-fx-font-size: 38px; -fx-font-weight: bold; -fx-text-fill: #000000;");

        Label sub = new Label(
            "Once signed in, your team can follow every incident, inspect AI diagnoses side-by-side, " +
            "and track resolution speed in real time. All in one place."
        );
        sub.setWrapText(true);
        sub.setTextAlignment(TextAlignment.CENTER);
        sub.setStyle("-fx-text-fill: rgba(0,0,0,0.70); -fx-font-size: 15.5px;");

        Button btnTeams = UITheme.serkoBlackPill("IncidentIQ for teams", true);
        btnTeams.setOnAction(e -> navigateTo(Page.FOR_TEAMS));
        header.getChildren().addAll(eyebrow, h2, sub, btnTeams);

        // Miniature Inside-Workspace Mockup matching Serko's "feature4_visual" preview card
        VBox frame = new VBox(16);
        frame.setMaxWidth(960);
        frame.setPadding(new Insets(22));
        frame.setStyle(
            "-fx-background-color: #F6F6F6;" +
            "-fx-background-radius: 26;" +
            "-fx-border-color: #EAEAEA;" +
            "-fx-border-radius: 26;"
        );
        frame.setEffect(UITheme.softCardShadow());

        // Top simulated app bar inside preview
        HBox appTop = new HBox(10);
        appTop.setAlignment(Pos.CENTER_LEFT);
        appTop.setPadding(new Insets(10, 16, 10, 16));
        appTop.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 14;");
        appTop.getChildren().addAll(
            UITheme.brandLogoMark(18, false),
            boldLabel("IncidentIQ Workspace", 13),
            spacerRegion(),
            UITheme.positiveBadge("Live · Connected to Spring Boot API")
        );

        HBox previewCols = new HBox(14);
        // Left table preview
        VBox leftQueue = new VBox(10);
        leftQueue.setPadding(new Insets(16));
        leftQueue.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16;");
        HBox.setHgrow(leftQueue, Priority.ALWAYS);
        leftQueue.getChildren().addAll(
            boldLabel("Active Incident Queue", 13),
            previewTableRow("#402", "PaymentGateway NullPointerException", "CRITICAL", true),
            previewTableRow("#401", "Redis Cache Eviction Spike",          "HIGH",     false),
            previewTableRow("#399", "JWT Refresh Race Condition",          "MEDIUM",   false)
        );

        // Right AI Agent summary preview
        VBox rightAi = new VBox(10);
        rightAi.setPrefWidth(340);
        rightAi.setPadding(new Insets(16));
        rightAi.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16;");
        Label aiTag = UITheme.positiveBadge("6/6 Agents Completed");
        Label aiSummary = new Label("Root Cause: Unchecked null metadata inside stripeChargeCallback() line 418.");
        aiSummary.setWrapText(true);
        aiSummary.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #374151;");
        Button openInside = UITheme.serkoRainbowPill("Open Real Workspace", true);
        openInside.setMaxWidth(Double.MAX_VALUE);
        openInside.setOnAction(e -> onOpenAuth.accept(false));
        rightAi.getChildren().addAll(boldLabel("AI Diagnosis Feed", 13), aiTag, aiSummary, spacerRegion(), openInside);

        previewCols.getChildren().addAll(leftQueue, rightAi);
        frame.getChildren().addAll(appTop, previewCols);

        band.getChildren().addAll(header, frame);
        return band;
    }

    private HBox previewTableRow(String id, String title, String sev, boolean selected) {
        HBox r = new HBox(10);
        r.setAlignment(Pos.CENTER_LEFT);
        r.setPadding(new Insets(9, 12, 9, 12));
        r.setStyle("-fx-background-color: " + (selected ? UITheme.BG_ICE : "#FAFAFA") + "; -fx-background-radius: 10;");
        Label lId = new Label(id);
        lId.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #71717A;");
        Label lTitle = new Label(title);
        lTitle.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 600; -fx-text-fill: #000000;");
        r.getChildren().addAll(lId, lTitle, spacerRegion(), UITheme.severityBadge(sev));
        return r;
    }

    // ── Bottom Horizon CTA Banner (Matches cta1_wrap) ───────────

    private VBox buildClosingCtaSection(String eyebrowTxt, String line1, String line2Before, String accentWord, String subTxt) {
        VBox cta = new VBox(14);
        cta.setAlignment(Pos.CENTER);
        cta.setPadding(new Insets(96, 48, 96, 48));
        cta.getStyleClass().add("serko-cta-bg");

        Label eyebrow = UITheme.eyebrowLabel(eyebrowTxt);
        TextFlow headline = UITheme.gradientAccentFlow(line1 + "\n" + line2Before, accentWord, "", 40, TextAlignment.CENTER, true);
        headline.setMaxWidth(760);

        Label sub = new Label(subTxt);
        sub.setWrapText(true);
        sub.setTextAlignment(TextAlignment.CENTER);
        sub.setMaxWidth(580);
        sub.setStyle("-fx-text-fill: rgba(0,0,0,0.72); -fx-font-size: 15.5px;");

        Button btn = UITheme.serkoBlackPill("Open Workspace", true);
        btn.setPadding(new Insets(12, 26, 12, 26));
        btn.setOnAction(e -> onOpenAuth.accept(true));

        HBox btnWrap = new HBox(btn);
        btnWrap.setAlignment(Pos.CENTER);
        btnWrap.setPadding(new Insets(10, 0, 0, 0));

        cta.getChildren().addAll(eyebrow, headline, sub, btnWrap);
        return cta;
    }

    // ── Interactive Expandable FAQ Accordion (Matches faq1_wrap) ─

    private VBox buildFaqSection(List<String[]> faqs) {
        VBox band = new VBox(26);
        band.setAlignment(Pos.CENTER);
        band.setPadding(new Insets(84, 56, 92, 56));
        band.setStyle("-fx-background-color: #FFFFFF;");

        VBox head = new VBox(8);
        head.setAlignment(Pos.CENTER);
        Label ey = UITheme.eyebrowLabel("FAQs");
        Label h2 = new Label("Got a question we haven't covered?\nHere's what engineers usually ask.");
        h2.setTextAlignment(TextAlignment.CENTER);
        h2.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #000000;");
        head.getChildren().addAll(ey, h2);

        VBox list = new VBox(10);
        list.setMaxWidth(640);

        for (String[] qa : faqs) {
            list.getChildren().add(faqAccordionItem(qa[0], qa[1]));
        }

        band.getChildren().addAll(head, list);
        return band;
    }

    private VBox faqAccordionItem(String q, String a) {
        VBox item = new VBox(0);
        item.setStyle(
            "-fx-background-color: #FFFFFF;" +
            "-fx-border-color: rgba(0,0,0,0.08);" +
            "-fx-border-radius: 14;" +
            "-fx-background-radius: 14;"
        );

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(16, 18, 16, 18));
        row.setCursor(Cursor.HAND);

        Label qLbl = new Label(q);
        qLbl.setWrapText(true);
        qLbl.setStyle("-fx-font-size: 14.5px; -fx-font-weight: 600; -fx-text-fill: #000000;");

        Region sp = spacerRegion();
        Label plus = new Label("+");
        plus.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #000000;");

        row.getChildren().addAll(qLbl, sp, plus);

        Label aLbl = new Label(a);
        aLbl.setWrapText(true);
        aLbl.setPadding(new Insets(0, 18, 18, 18));
        aLbl.setStyle("-fx-font-size: 13.5px; -fx-text-fill: #4D4D4D; -fx-line-spacing: 3px;");
        aLbl.setVisible(false);
        aLbl.setManaged(false);

        final boolean[] open = {false};
        row.setOnMouseClicked(e -> {
            open[0] = !open[0];
            aLbl.setVisible(open[0]);
            aLbl.setManaged(open[0]);
            item.setStyle(
                "-fx-background-color: " + (open[0] ? "#F6F6F6" : "#FFFFFF") + ";" +
                "-fx-border-color: rgba(0,0,0,0.08);" +
                "-fx-border-radius: 14;" +
                "-fx-background-radius: 14;"
            );
            RotateTransition rt = new RotateTransition(Duration.millis(180), plus);
            rt.setToAngle(open[0] ? 45 : 0);
            rt.play();
        });

        item.getChildren().addAll(row, aLbl);
        return item;
    }

    // ── Dark Editorial Footer (Matches footer_wrap) ─────────────

    private VBox buildDarkFooter() {
        VBox footer = new VBox(36);
        footer.setPadding(new Insets(64, 56, 42, 56));
        footer.setStyle("-fx-background-color: #000000;");

        HBox topCols = new HBox(48);
        topCols.setAlignment(Pos.TOP_LEFT);

        VBox left = new VBox(18);
        left.setMaxWidth(440);
        HBox.setHgrow(left, Priority.ALWAYS);

        StackPane whiteMark = UITheme.brandLogoMark(30, true);
        TextFlow fHeadline = UITheme.gradientAccentFlow(
            "Be there for the ", "releases", "\nthat matter.", 32, TextAlignment.LEFT, false
        );
        Button btnStartWhite = new Button("Get started");
        btnStartWhite.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
        btnStartWhite.setGraphicTextGap(8);
        btnStartWhite.setGraphic(UITheme.chevronRightIcon(Color.BLACK, 0.72));
        btnStartWhite.setStyle(
            "-fx-background-color: #FFFFFF; -fx-text-fill: #000000; -fx-font-weight: bold;" +
            "-fx-font-size: 13.5px; -fx-padding: 10 22; -fx-background-radius: 999; -fx-cursor: hand;"
        );
        btnStartWhite.setOnAction(e -> onOpenAuth.accept(true));
        left.getChildren().addAll(whiteMark, fHeadline, btnStartWhite);

        HBox rightCols = new HBox(48);
        rightCols.setAlignment(Pos.TOP_RIGHT);
        rightCols.getChildren().addAll(
            footerColumn("EXPLORE", List.of(
                footerLink("Home",      () -> navigateTo(Page.HOME)),
                footerLink("Platform",  () -> navigateTo(Page.PLATFORM)),
                footerLink("For teams", () -> navigateTo(Page.FOR_TEAMS)),
                footerLink("About",     () -> navigateTo(Page.ABOUT))
            )),
            footerColumn("CAPABILITIES", List.of(
                footerLink("6-Agent Pipeline",   () -> navigateTo(Page.PLATFORM)),
                footerLink("Quick LLM Triage",   () -> navigateTo(Page.PLATFORM)),
                footerLink("MTTR Analytics",     () -> navigateTo(Page.FOR_TEAMS))
            )),
            footerColumn("ACCOUNT", List.of(
                footerLink("Sign In",        () -> onOpenAuth.accept(false)),
                footerLink("Create Account", () -> onOpenAuth.accept(true))
            ))
        );

        Region midSp = spacerRegion();
        topCols.getChildren().addAll(left, midSp, rightCols);

        HBox bottomBar = new HBox();
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(20, 0, 0, 0));
        bottomBar.setStyle("-fx-border-color: rgba(255,255,255,0.14) transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        Label copy = new Label("© 2026 IncidentIQ Platform · Powered by Spring Boot & 6-Agent AI");
        copy.setStyle("-fx-text-fill: rgba(255,255,255,0.52); -fx-font-size: 12px;");
        bottomBar.getChildren().add(copy);

        footer.getChildren().addAll(topCols, bottomBar);
        return footer;
    }

    private VBox footerColumn(String title, List<Button> links) {
        VBox col = new VBox(8);
        Label t = new Label(title);
        t.setStyle("-fx-text-fill: rgba(255,255,255,0.48); -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 0.8px;");
        col.getChildren().add(t);
        col.getChildren().addAll(links);
        return col;
    }

    private Button footerLink(String text, Runnable action) {
        Button b = new Button(text);
        b.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.85);" +
            "-fx-font-size: 13px; -fx-padding: 3 0; -fx-cursor: hand;"
        );
        b.setOnMouseEntered(e -> b.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #FFFFFF;" +
            "-fx-font-size: 13px; -fx-padding: 3 0; -fx-underline: true; -fx-cursor: hand;"
        ));
        b.setOnMouseExited(e -> b.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.85);" +
            "-fx-font-size: 13px; -fx-padding: 3 0; -fx-cursor: hand;"
        ));
        b.setOnAction(e -> action.run());
        return b;
    }

    // ═════════════════════════════════════════════════════════════
    //  PAGE 2: PLATFORM (Matches https://www.serko.ai/platform)
    // ═════════════════════════════════════════════════════════════

    private void buildPlatformPage() {
        faqAnchorFraction = 0.78;
        StackPane hero = new StackPane();
        hero.setMinHeight(640);
        hero.getStyleClass().add("serko-hero-bg");

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(130, 56, 64, 56));
        content.setMaxWidth(920);

        Label eyebrow = UITheme.eyebrowLabel("AI incident software");
        TextFlow headline = UITheme.gradientAccentFlow(
            "Built around ", "autonomous agents", "\nfrom the ground up.", 48, TextAlignment.CENTER, true
        );
        Label sub = new Label(
            "Paste a production stack trace once. Six specialized agents analyze logs, classify failure domain, " +
            "evaluate severity, pinpoint root cause, and generate remediation steps automatically."
        );
        sub.setWrapText(true);
        sub.setTextAlignment(TextAlignment.CENTER);
        sub.setMaxWidth(640);
        sub.setStyle("-fx-text-fill: rgba(0,0,0,0.70); -fx-font-size: 16px;");

        // Interactive Typewriter Log-to-Insight Strip (mirrors platform's trip-planner-typewriter.js)
        HBox typeCard = new HBox(12);
        typeCard.setAlignment(Pos.CENTER_LEFT);
        typeCard.setMaxWidth(680);
        typeCard.setPadding(new Insets(16, 20, 16, 20));
        typeCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 18;");
        typeCard.setEffect(UITheme.softCardShadow());
        Label promptBadge = UITheme.positiveBadge("Live Agent Input");
        promptBadge.setMinWidth(Region.USE_PREF_SIZE);
        Label typewriterLbl = new Label("java.sql.SQLTransientConnectionException: HikariPool-1 - Connection is not available...");
        typewriterLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #111827;");
        HBox.setHgrow(typewriterLbl, Priority.ALWAYS);
        typeCard.getChildren().addAll(promptBadge, typewriterLbl);

        String[] samples = {
            "java.sql.SQLTransientConnectionException: HikariPool-1 - Connection timeout after 30000ms...",
            "io.jsonwebtoken.ExpiredJwtException: JWT expired at 2026-11-09T12:14:00Z on /api/analytics...",
            "java.lang.NullPointerException: Cannot invoke StripeToken.getId() because token is null..."
        };
        final int[] sIdx = {0};
        typewriterTimeline = new Timeline(new KeyFrame(Duration.seconds(3.2), ev -> {
            sIdx[0] = (sIdx[0] + 1) % samples.length;
            typewriterLbl.setText(samples[sIdx[0]]);
        }));
        typewriterTimeline.setCycleCount(Timeline.INDEFINITE);
        typewriterTimeline.play();

        Button btnTry = UITheme.serkoBlackPill("Get started", true);
        btnTry.setOnAction(e -> onOpenAuth.accept(true));

        content.getChildren().addAll(eyebrow, headline, sub, typeCard, btnTry);
        hero.getChildren().add(content);

        // 6-Agent Architecture Showcase Section
        VBox agentsBand = new VBox(28);
        agentsBand.setAlignment(Pos.CENTER);
        agentsBand.setPadding(new Insets(84, 56, 84, 56));
        agentsBand.setStyle("-fx-background-color: #FFFFFF;");

        Label aEye = UITheme.eyebrowLabel("AI-native technology");
        Label aH2 = new Label("What makes IncidentIQ different to a plain chatbot.");
        aH2.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #000000;");
        Label aSub = new Label("Click any of the 6 specialized agents below to inspect its stage in the pipeline:");
        aSub.setStyle("-fx-text-fill: #4D4D4D; -fx-font-size: 15px;");

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
        Label detTitle = new Label("1. Log Analysis Agent");
        detTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #000000;");
        Label detDesc = new Label("Parses raw stack traces, filters framework noise, and extracts top exception frames, SQL/HTTP error codes, and failing timestamps.");
        detDesc.setWrapText(true);
        detDesc.setStyle("-fx-font-size: 14.5px; -fx-text-fill: #1F2937; -fx-line-spacing: 3px;");
        stepDetailCard.getChildren().addAll(detStage, detTitle, detDesc);

        String[][] stages = {
            {"1. Log Analysis Agent",          "Agent 1 of 6", "Parses raw stack traces, filters framework noise, and extracts top exception frames, SQL/HTTP error codes, and failing classes."},
            {"2. Error Classification Agent",  "Agent 2 of 6", "Categorizes incidents cleanly (Database, Security/JWT, Memory/OOM, Network, API Integration) using historical signatures."},
            {"3. Severity Evaluation Agent",   "Agent 3 of 6", "Scores blast radius and urgency (CRITICAL / HIGH / MEDIUM / LOW) so critical outages surface first."},
            {"4. Root Cause Agent",            "Agent 4 of 6", "Synthesizes code-path evidence to pinpoint WHY the failure occurred rather than merely repeating the exception message."},
            {"5. Resolution Agent",            "Agent 5 of 6", "Generates prioritized remediation actions, configuration changes, and defensive code guards."},
            {"6. Report Generation Agent",     "Agent 6 of 6", "Compiles an executive summary + full structured Markdown report persisted directly to the Investigation database."}
        };

        for (String[] st : stages) {
            Button sb = new Button(st[0]);
            sb.setMaxWidth(Double.MAX_VALUE);
            sb.setAlignment(Pos.CENTER_LEFT);
            sb.setStyle("-fx-background-color: #F6F6F6; -fx-text-fill: #000000; -fx-font-weight: 600; -fx-font-size: 13.5px; -fx-padding: 13 18; -fx-background-radius: 14; -fx-cursor: hand;");
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
        sH2.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: #000000;");
        Label sSub = new Label("Protected with Spring Security stateless JWT authentication, parameterized JPA persistence, and environment-externalized AI keys.");
        sSub.setWrapText(true);
        sSub.setMaxWidth(620);
        sSub.setTextAlignment(TextAlignment.CENTER);
        sSub.setStyle("-fx-text-fill: #4D4D4D; -fx-font-size: 15px;");
        secBand.getChildren().addAll(sEye, sH2, sSub);

        pageContainer.getChildren().addAll(
            hero,
            agentsBand,
            secBand,
            buildClosingCtaSection("Set up", "Get going in an instant.", "Start triaging with ", "IncidentIQ.",
                "Create your engineering account, paste your first stack trace, and watch 6 agents go to work."),
            buildFaqSection(defaultHomeFaqs()),
            buildDarkFooter()
        );
    }

    // ═════════════════════════════════════════════════════════════
    //  PAGE 3: FOR TEAMS (Matches https://www.serko.ai/for-companies)
    // ═════════════════════════════════════════════════════════════

    private void buildForTeamsPage() {
        faqAnchorFraction = 0.78;
        StackPane hero = new StackPane();
        hero.setMinHeight(640);
        hero.getStyleClass().add("serko-hero-bg");

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(130, 56, 64, 56));
        content.setMaxWidth(960);

        Label eyebrow = UITheme.eyebrowLabel("Engineering operations management");
        TextFlow headline = UITheme.gradientAccentFlow(
            "A new way to run your\n", "incident", " response.", 50, TextAlignment.CENTER, true
        );
        Label sub = new Label(
            "IncidentIQ eliminates noisy manual triage and keeps engineering leads informed with real-time MTTR, " +
            "AI coverage metrics, and collaborative investigation timelines."
        );
        sub.setWrapText(true);
        sub.setTextAlignment(TextAlignment.CENTER);
        sub.setMaxWidth(640);
        sub.setStyle("-fx-text-fill: rgba(0,0,0,0.70); -fx-font-size: 16px;");

        // Multi-engineer floating status strip (mirrors Ella / Saur / Skyla status cards on /for-companies)
        HBox teamCards = new HBox(14);
        teamCards.setAlignment(Pos.CENTER);
        teamCards.setPadding(new Insets(14, 0, 10, 0));
        teamCards.getChildren().addAll(
            teamStatusCard("Checkout API",   "US-East",   "Reviewed by AI", true),
            teamStatusCard("Auth Gateway",   "EU-West",   "P0 Critical",    false),
            teamStatusCard("Billing Worker", "AP-South",  "Patch Ready",    true)
        );

        Button btnFree = UITheme.serkoBlackPill("Sign up free", true);
        btnFree.setOnAction(e -> onOpenAuth.accept(true));

        content.getChildren().addAll(eyebrow, headline, sub, teamCards, btnFree);
        hero.getChildren().add(content);

        // 3 Structured Enterprise Cards
        VBox gridBand = new VBox(24);
        gridBand.setAlignment(Pos.CENTER);
        gridBand.setPadding(new Insets(84, 56, 84, 56));
        gridBand.setStyle("-fx-background-color: #FFFFFF;");

        HBox cardsRow = new HBox(20);
        cardsRow.setMaxWidth(980);
        cardsRow.getChildren().addAll(
            enterpriseCard("INCIDENT WORKFLOW", "Skip the midnight war rooms", "Instant 6-agent triage gives on-call engineers concrete root causes in seconds instead of multi-hour log searches.", UITheme.BG_SOFT),
            enterpriseCard("TELEMETRY VISIBILITY", "Ask once, see real metrics", "Track Open vs Resolved volume, Critical backlog, MTTR hours, and AI coverage percentages live.", UITheme.BG_ICE),
            enterpriseCard("FAST ONBOARDING", "Zero external tooling setup", "Already connected to your Spring Boot REST backend, MySQL store, and Groq / Gemini keys.", UITheme.BG_SOFT)
        );
        gridBand.getChildren().add(cardsRow);

        pageContainer.getChildren().addAll(
            hero,
            gridBand,
            buildClosingCtaSection("Get started", "Start using ", "", "IncidentIQ today.",
                "Sign in or register in seconds and bring calm, AI-assisted incident response to your engineering team."),
            buildDarkFooter()
        );
    }

    private VBox teamStatusCard(String service, String region, String status, boolean positive) {
        VBox c = new VBox(8);
        c.setPrefWidth(220);
        c.setPadding(new Insets(16));
        c.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 18;");
        c.setEffect(UITheme.softCardShadow());
        Label s = boldLabel(service, 14);
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
        c.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 22;");
        HBox.setHgrow(c, Priority.ALWAYS);
        Label e = UITheme.eyebrowLabel(ey);
        Label t = new Label(title);
        t.setWrapText(true);
        t.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #000000;");
        Label d = new Label(desc);
        d.setWrapText(true);
        d.setStyle("-fx-font-size: 14px; -fx-text-fill: #4D4D4D; -fx-line-spacing: 3px;");
        c.getChildren().addAll(e, t, d);
        return c;
    }

    // ═════════════════════════════════════════════════════════════
    //  PAGE 4: ABOUT (Matches https://www.serko.ai/about)
    // ═════════════════════════════════════════════════════════════

    private void buildAboutPage() {
        faqAnchorFraction = 0.72;
        StackPane hero = new StackPane();
        hero.setMinHeight(560);
        hero.getStyleClass().add("serko-hero-bg");

        VBox content = new VBox(18);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(130, 56, 64, 56));
        content.setMaxWidth(840);

        Label eyebrow = UITheme.eyebrowLabel("IncidentIQ, your AI incident agent");
        TextFlow headline = UITheme.gradientAccentFlow(
            "We built IncidentIQ so engineers\ncan focus on ", "shipping.", "", 48, TextAlignment.CENTER, true
        );
        Label sub = new Label(
            "Software reliability should never depend on who happens to be awake at 2 AM parsing raw Java stack traces. " +
            "IncidentIQ pairs multi-agent reasoning with structured engineering workflows."
        );
        sub.setWrapText(true);
        sub.setTextAlignment(TextAlignment.CENTER);
        sub.setMaxWidth(640);
        sub.setStyle("-fx-text-fill: rgba(0,0,0,0.72); -fx-font-size: 16px;");

        HBox actions = new HBox(12, UITheme.serkoBlackPill("Explore Platform", true));
        actions.setAlignment(Pos.CENTER);
        ((Button) actions.getChildren().get(0)).setOnAction(e -> navigateTo(Page.PLATFORM));

        content.getChildren().addAll(eyebrow, headline, sub, actions);
        hero.getChildren().add(content);

        // Vision + Architecture Story
        VBox storyBand = new VBox(22);
        storyBand.setAlignment(Pos.CENTER);
        storyBand.setPadding(new Insets(84, 56, 84, 56));
        storyBand.setStyle("-fx-background-color: #FFFFFF;");

        VBox storyWrap = new VBox(18);
        storyWrap.setMaxWidth(720);
        storyWrap.getChildren().addAll(
            UITheme.eyebrowLabel("Our vision"),
            boldLabel("Always in your corner when production alerts fire.", 28),
            paraLabel("Modern distributed services generate thousands of log lines before anyone opens a terminal. IncidentIQ bridges raw telemetry with an orchestrated 6-agent pipeline built specifically for Spring Boot, cloud APIs, and JVM services."),
            UITheme.dottedDivider(680),
            boldLabel("AI-native by design", 22),
            paraLabel("Rather than pasting a prompt box over static tables, IncidentIQ connects authenticated JWT sessions, JPA incident lifecycles, sub-second Groq inference, and Gemini fallbacks into a cohesive desktop workbench.")
        );
        storyBand.getChildren().add(storyWrap);

        pageContainer.getChildren().addAll(
            hero,
            storyBand,
            buildFaqSection(defaultHomeFaqs()),
            buildDarkFooter()
        );
    }

    // ── Shared Layout & Copy Helpers ────────────────────────────

    private GridPane twoColumnSectionGrid(double hgap) {
        GridPane grid = new GridPane();
        grid.setMaxWidth(1040);
        grid.setHgap(hgap);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(48);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(52);
        grid.getColumnConstraints().addAll(c1, c2);
        return grid;
    }

    private Label boldLabel(String txt, double size) {
        Label l = new Label(txt);
        l.setWrapText(true);
        l.setStyle("-fx-font-size: " + size + "px; -fx-font-weight: bold; -fx-text-fill: #000000;");
        return l;
    }

    private Label paraLabel(String txt) {
        Label l = new Label(txt);
        l.setWrapText(true);
        l.setStyle("-fx-font-size: 15.5px; -fx-text-fill: #4D4D4D; -fx-line-spacing: 4px;");
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
                "How is this different from pasting stack traces into a generic chatbot?",
                "Every diagnosis runs directly against structured incident records, enforces severity/category extraction, and persists full Markdown investigation reports alongside live MTTR analytics."
            },
            new String[]{
                "Is my authentication and incident data isolated cleanly?",
                "Yes. Every API call uses stateless JWT Bearer authentication and executes locally against your Spring Boot backend and MySQL database."
            },
            new String[]{
                "Which AI providers are supported?",
                "IncidentIQ supports ultra-fast Groq LPU models (such as openai/gpt-oss-20b) as well as Gemini API fallback configured in application.properties."
            }
        );
    }
}
