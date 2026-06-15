package com.trafficsim.ui;

import com.trafficsim.domain.Lane;
import com.trafficsim.domain.Vehicle;
import com.trafficsim.engine.SimEvent;
import com.trafficsim.engine.SimulationController;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.List;

/**
 * Animated view of the intersection (testable signal for FR-06). Each lane is drawn as an approach
 * road whose vehicles queue toward a shared intersection band on the right; the lane currently
 * holding the green light shows its vehicle crossing that band. Rendering reads only the
 * controller's current state, so it stays a passive view of the deterministic engine.
 *
 * <p>The layout is deliberately a queue-oriented schematic rather than a literal four-way junction:
 * it scales cleanly to any lane count and makes the scheduling behaviour (who is queued, who is
 * crossing, who was promoted) the visually obvious thing — which is what a cold demo needs.</p>
 */
public final class SimulationVisualizer extends Pane {

    private final Canvas canvas = new Canvas();
    private final VehicleMovementDisplayer vehicleMovementDisplayer = new VehicleMovementDisplayer();
    private final SignalChangeDisplayer signalChangeDisplayer = new SignalChangeDisplayer();

    public SimulationVisualizer() {
        getChildren().add(canvas);
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        setMinSize(560, 360);
    }

    public VehicleMovementDisplayer vehicleMovementDisplayer() {
        return vehicleMovementDisplayer;
    }

    public SignalChangeDisplayer signalChangeDisplayer() {
        return signalChangeDisplayer;
    }

    /** Routes a tick event to both the movement log and the signal-change log. */
    public void acceptEvent(SimEvent event) {
        vehicleMovementDisplayer.accept(event);
        signalChangeDisplayer.accept(event);
    }

    public void clearLogs() {
        vehicleMovementDisplayer.clear();
        signalChangeDisplayer.clear();
    }

    /** Redraws the whole scene from the controller's current state. Safe to call every frame. */
    public void render(SimulationController controller) {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // background
        gc.setFill(Color.web("#0b1220"));
        gc.fillRect(0, 0, w, h);

        if (controller == null || controller.lanes().isEmpty()) {
            drawCentredHint(gc, w, h, "Configure a scenario and press Start");
            return;
        }

        List<Lane> lanes = controller.lanes();
        Vehicle running = controller.running();

        double topPad = 46;
        double bottomPad = 14;
        double queueLeft = 96;
        double queueRight = w - 210;
        double signalX = queueRight + 26;
        double bandLeft = w - 168;
        double bandRight = w - 108;
        double exitX = w - 96;
        double rowH = Math.max(36, (h - topPad - bottomPad) / lanes.size());
        double vehH = Math.min(26, rowH * 0.5);
        double vehW = 30;

        drawHeader(gc, w, controller);

        for (int i = 0; i < lanes.size(); i++) {
            Lane lane = lanes.get(i);
            double yc = topPad + i * rowH + rowH / 2;
            boolean isGreen = running != null && running.laneId() == lane.id();

            // road
            gc.setFill(Color.web("#1f2937"));
            gc.fillRect(queueLeft, yc - rowH / 2 + 4, queueRight - queueLeft, rowH - 8);

            // lane label
            gc.setFill(Color.web("#cbd5e1"));
            gc.setFont(Font.font("System", FontWeight.SEMI_BOLD, 12));
            gc.setTextAlign(TextAlignment.LEFT);
            gc.fillText(lane.name(), 10, yc + 4);
            gc.setFill(Color.web("#64748b"));
            gc.setFont(Font.font(10));
            gc.fillText("queue " + lane.queueLength(), 10, yc + 18);

            // queued vehicles (head nearest the intersection)
            List<Vehicle> queued = lane.snapshot();
            for (int q = 0; q < queued.size(); q++) {
                double vx = queueRight - vehW - q * (vehW + 6);
                if (vx < queueLeft) {
                    break; // queue longer than the road; rest is implied by the "queue N" label
                }
                boolean suspended = controller.normalVehicleSuspender().isSuspended(queued.get(q).id());
                vehicleMovementDisplayer.drawVehicle(gc, queued.get(q), vx, yc - vehH / 2, vehW, vehH, suspended, false);
            }

            // signal light
            signalChangeDisplayer.drawSignal(gc, signalX, yc, 6, lane.signal());

            // intersection band
            gc.setFill(isGreen ? Color.web("#14532d") : Color.web("#111827"));
            gc.fillRect(bandLeft, yc - rowH / 2 + 4, bandRight - bandLeft, rowH - 8);
            gc.setStroke(Color.web("#334155"));
            gc.setLineWidth(1);
            gc.strokeRect(bandLeft, yc - rowH / 2 + 4, bandRight - bandLeft, rowH - 8);

            // crossing vehicle
            if (isGreen) {
                double p = progress(running);
                double cx = bandLeft + p * (exitX - bandLeft);
                vehicleMovementDisplayer.drawVehicle(gc, running, cx, yc - vehH / 2, vehW, vehH, false, true);
            }
        }
    }

    private double progress(Vehicle v) {
        if (v == null || v.burstTime() == 0) {
            return 0;
        }
        double p = (double) (v.burstTime() - v.remainingTime()) / v.burstTime();
        return Math.max(0, Math.min(1, p));
    }

    private void drawHeader(GraphicsContext gc, double w, SimulationController c) {
        gc.setFill(Color.web("#e2e8f0"));
        gc.setFont(Font.font("System", FontWeight.BOLD, 15));
        gc.setTextAlign(TextAlignment.LEFT);
        String algo = c.strategy() == null ? "-" : c.strategy().name();
        gc.fillText("Algorithm: " + algo, 12, 24);

        gc.setFont(Font.font(12));
        gc.setFill(Color.web("#94a3b8"));
        gc.setTextAlign(TextAlignment.RIGHT);
        String status = c.isFinished() ? "FINISHED" : "running";
        gc.fillText("tick " + c.clock().currentTick() + "   •   " + status
                + "   •   done " + c.completedCount() + "/" + c.totalVehicles(), w - 12, 24);
    }

    private void drawCentredHint(GraphicsContext gc, double w, double h, String text) {
        gc.setFill(Color.web("#64748b"));
        gc.setFont(Font.font(16));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(text, w / 2, h / 2);
    }
}
