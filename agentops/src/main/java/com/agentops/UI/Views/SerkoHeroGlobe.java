package com.agentops.UI.Views;

import java.net.URL;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * Hybrid JS HTML5 Canvas / Native JavaFX Canvas 3D rotating wireframe globe +
 * horizon backdrop.
 * Matches live serko.ai's translucent ice dome with fine dark charcoal
 * stippling and meridians.
 */
public class SerkoHeroGlobe extends StackPane {

    private boolean usingWebView = false;
    private WebEngine engine;
    private boolean pageLoaded = false;
    private boolean paused = false;
    private AnimationTimer fallbackTimer;
    private double targetPtrX = 0;
    private double targetPtrY = 0;
    private double lastSentX = 0;
    private double lastSentY = 0;
    private long lastPointerDispatchMs = 0;

    public SerkoHeroGlobe() {
        setMinSize(0, 0);
        setMouseTransparent(true);
        getStyleClass().add("serko-hero-bg");

        try {
            URL htmlUrl = getClass().getResource("/web/hero-globe.html");
            if (htmlUrl != null) {
                WebView webView = new WebView();
                webView.setMinSize(0, 0);
                webView.setMouseTransparent(true);
                webView.setContextMenuEnabled(false);
                webView.setFocusTraversable(false);
                engine = webView.getEngine();
                engine.setJavaScriptEnabled(true);
                engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                    if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                        pageLoaded = true;
                        applyWebPause();
                    }
                });
                engine.load(htmlUrl.toExternalForm());

                webView.prefWidthProperty().bind(widthProperty());
                webView.prefHeightProperty().bind(heightProperty());
                getChildren().add(webView);
                usingWebView = true;
            }
        } catch (Throwable ignored) {
            usingWebView = false;
        }

        if (!usingWebView) {
            installCanvasFallback();
        }
    }

    public void setPaused(boolean paused) {
        if (this.paused == paused)
            return;
        this.paused = paused;
        if (usingWebView) {
            applyWebPause();
        } else if (fallbackTimer != null) {
            if (paused)
                fallbackTimer.stop();
            else
                fallbackTimer.start();
        }
    }

    public void setPointer(double nx, double ny) {
        double clampedX = Math.max(-1.0, Math.min(1.0, nx));
        double clampedY = Math.max(-1.0, Math.min(1.0, ny));
        this.targetPtrX = clampedX;
        this.targetPtrY = clampedY;

        if (!usingWebView || !pageLoaded || engine == null || paused) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastPointerDispatchMs < 28) {
            return;
        }
        if (Math.abs(clampedX - lastSentX) < 0.015 && Math.abs(clampedY - lastSentY) < 0.015) {
            return;
        }
        lastPointerDispatchMs = now;
        lastSentX = clampedX;
        lastSentY = clampedY;
        try {
            engine.executeScript(String.format(java.util.Locale.ROOT,
                "if (window.setGlobePointer) window.setGlobePointer(%.3f, %.3f);", clampedX, clampedY));
        } catch (Throwable ignored) {
        }
    }

    public void resetPointer() {
        this.targetPtrX = 0;
        this.targetPtrY = 0;
        this.lastSentX = 0;
        this.lastSentY = 0;
        if (usingWebView && pageLoaded && engine != null) {
            try {
                engine.executeScript("if (window.setGlobePointer) window.setGlobePointer(0, 0);");
            } catch (Throwable ignored) {
            }
        }
    }

    private void applyWebPause() {
        if (!pageLoaded || engine == null)
            return;
        try {
            engine.executeScript("if (window.setGlobePaused) window.setGlobePaused(" + paused + ");");
        } catch (Throwable ignored) {
        }
    }

    private void installCanvasFallback() {
        Canvas canvas = new Canvas();
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        getChildren().add(canvas);

        fallbackTimer = new AnimationTimer() {
            double t = 0;
            double ptrX = 0;
            double ptrY = 0;

            @Override
            public void handle(long now) {
                t += 0.010;
                ptrX += (targetPtrX - ptrX) * 0.08;
                ptrY += (targetPtrY - ptrY) * 0.08;
                double w = canvas.getWidth();
                double h = canvas.getHeight();
                if (w <= 0 || h <= 0)
                    return;

                GraphicsContext g = canvas.getGraphicsContext2D();
                g.clearRect(0, 0, w, h);

                double cx = w * 0.5 + ptrX * 7.0;
                double cy = h * 0.74 + ptrY * 5.0;
                double r = Math.max(240, Math.min(w * 0.36, h * 0.33));

                RadialGradient inner = new RadialGradient(
                        0, 0, cx, cy - r * 0.32, r, false, CycleMethod.NO_CYCLE,
                        new Stop(0.0, Color.rgb(255, 255, 255, 0.42)),
                        new Stop(0.60, Color.rgb(150, 203, 250, 0.22)),
                        new Stop(1.0, Color.TRANSPARENT));
                g.setFill(inner);
                g.fillOval(cx - r, cy - r, r * 2, r * 2);

                g.setStroke(Color.rgb(0, 0, 0, 0.26));
                g.setLineWidth(1.1);
                g.strokeOval(cx - r, cy - r, r * 2, r * 2);

                g.setStroke(Color.rgb(0, 0, 0, 0.13));
                g.setLineWidth(0.85);
                for (int i = 0; i < 8; i++) {
                    double angle = (t * 0.35 + i * Math.PI / 8.0) % Math.PI;
                    double rx = Math.abs(Math.cos(angle)) * r;
                    g.strokeOval(cx - rx, cy - r, rx * 2, r * 2);
                }

                LinearGradient arcGrad = new LinearGradient(
                        cx - r, cy, cx + r, cy, false, CycleMethod.NO_CYCLE,
                        new Stop(0.0, Color.rgb(0, 0, 0, 0.18)),
                        new Stop(0.5, Color.web(UITheme.GRAD_BLUE, 0.45)),
                        new Stop(1.0, Color.rgb(0, 0, 0, 0.18)));
                g.setStroke(arcGrad);
                g.setLineWidth(1.25);
                g.strokeArc(cx - r * 0.75, cy - r * 0.45, r * 1.5, r * 0.8, 15, 150, javafx.scene.shape.ArcType.OPEN);
            }
        };
        if (!paused) {
            fallbackTimer.start();
        }
    }
}
