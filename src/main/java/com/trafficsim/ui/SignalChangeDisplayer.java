package com.trafficsim.ui;

import com.trafficsim.domain.SignalState;
import com.trafficsim.engine.SimEvent;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Draws the per-lane traffic light and logs signal changes in real time (testable signal for FR-06).
 * A small three-lamp housing is rendered for each lane with the active colour lit, and every change
 * of the green light is recorded for the activity feed.
 */
public final class SignalChangeDisplayer {

    private static final int LOG_LIMIT = 8;
    private final Deque<String> recentChanges = new ArrayDeque<>();

    /** Draws a compact vertical traffic light (red over yellow over green) with one lamp lit. */
    public void drawSignal(GraphicsContext gc, double cx, double cy, double lampRadius, SignalState state) {
        double housingW = lampRadius * 2 + 8;
        double housingH = lampRadius * 6 + 16;
        double hx = cx - housingW / 2;
        double hy = cy - housingH / 2;

        gc.setFill(Color.web("#1e293b"));
        gc.fillRoundRect(hx, hy, housingW, housingH, 8, 8);

        drawLamp(gc, cx, hy + 4 + lampRadius, lampRadius, Color.web("#ef4444"), state == SignalState.RED);
        drawLamp(gc, cx, hy + 8 + lampRadius * 3, lampRadius, Color.web("#f59e0b"), state == SignalState.YELLOW);
        drawLamp(gc, cx, hy + 12 + lampRadius * 5, lampRadius, Color.web("#22c55e"), state == SignalState.GREEN);
    }

    private void drawLamp(GraphicsContext gc, double cx, double cy, double r, Color on, boolean lit) {
        gc.setFill(lit ? on : on.deriveColor(0, 1, 0.25, 1)); // dimmed when off
        gc.fillOval(cx - r, cy - r, r * 2, r * 2);
    }

    public void accept(SimEvent event) {
        if (event.type() == SimEvent.Type.SIGNAL_CHANGE) {
            push("t=" + event.tick() + "  " + event.message());
        }
    }

    public List<String> recentChanges() {
        return List.copyOf(recentChanges);
    }

    public void clear() {
        recentChanges.clear();
    }

    private void push(String line) {
        recentChanges.addFirst(line);
        while (recentChanges.size() > LOG_LIMIT) {
            recentChanges.removeLast();
        }
    }
}
