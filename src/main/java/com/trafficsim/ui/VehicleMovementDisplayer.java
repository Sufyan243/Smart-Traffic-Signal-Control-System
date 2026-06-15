package com.trafficsim.ui;

import com.trafficsim.domain.Vehicle;
import com.trafficsim.engine.SimEvent;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Draws individual vehicle glyphs and keeps a short rolling log of vehicle movements (testable
 * signal for FR-06). {@link SimulationVisualizer} handles where each vehicle sits; this class is
 * responsible for what a vehicle looks like — colour by type, an emergency glow, a "suspended" grey
 * wash — and for narrating start/complete events so the activity feed reads like real traffic.
 */
public final class VehicleMovementDisplayer {

    private static final int LOG_LIMIT = 8;
    private final Deque<String> recentMovements = new ArrayDeque<>();

    /** Draws one vehicle as a rounded rectangle with its id, optionally dimmed if suspended. */
    public void drawVehicle(GraphicsContext gc, Vehicle vehicle,
                            double x, double y, double w, double h,
                            boolean suspended, boolean crossing) {
        Color base = vehicle.type().color();
        if (suspended) {
            base = base.deriveColor(0, 0.35, 1.0, 0.6); // washed out while suspended
        }

        if (vehicle.isEmergency()) {
            gc.setFill(Color.web("#ef4444", 0.35));
            gc.fillRoundRect(x - 3, y - 3, w + 6, h + 6, 10, 10); // glow halo
        } else if (vehicle.wasPromotedByAging()) {
            gc.setFill(Color.web("#fbbf24", 0.30));
            gc.fillRoundRect(x - 3, y - 3, w + 6, h + 6, 10, 10); // gold halo: starvation-prevention promotion
        }

        gc.setFill(base);
        gc.fillRoundRect(x, y, w, h, 8, 8);
        // gold outline persists on any vehicle that aging has promoted, so the flag stays visible
        gc.setStroke(crossing ? Color.WHITE : vehicle.wasPromotedByAging() ? Color.web("#fbbf24") : Color.web("#0f172a"));
        gc.setLineWidth(crossing || vehicle.wasPromotedByAging() ? 2.0 : 1.0);
        gc.strokeRoundRect(x, y, w, h, 8, 8);

        gc.setFill(Color.web("#0f172a"));
        gc.setFont(Font.font(11));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("V" + vehicle.id(), x + w / 2, y + h / 2 + 4);
    }

    /** Feeds movement and scheduling-decision events into the rolling activity log. */
    public void accept(SimEvent event) {
        switch (event.type()) {
            case VEHICLE_START, VEHICLE_COMPLETE, EMERGENCY_INJECTED,
                 EMERGENCY_GRANTED, NORMAL_SUSPENDED, AGING_PROMOTION ->
                    push("t=" + event.tick() + "  " + event.message());
            default -> { /* signal changes are logged separately */ }
        }
    }

    public List<String> recentMovements() {
        return List.copyOf(recentMovements);
    }

    public void clear() {
        recentMovements.clear();
    }

    private void push(String line) {
        recentMovements.addFirst(line);
        while (recentMovements.size() > LOG_LIMIT) {
            recentMovements.removeLast();
        }
    }
}
