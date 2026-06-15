package com.trafficsim.ui;

import com.trafficsim.domain.Lane;
import com.trafficsim.engine.SimulationController;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * Live display of traffic-flow metrics (testable signal for FR-03): how many vehicles exist, how
 * many are queued, crossing or done, plus emergency/suspension counts. These are the "what is the
 * road doing right now" numbers, as opposed to the scheduling-theory averages.
 */
public final class TrafficMetricDisplayer extends VBox {

    private final Label total;
    private final Label queued;
    private final Label crossing;
    private final Label completed;
    private final Label emergencies;
    private final Label suspended;

    public TrafficMetricDisplayer() {
        VBox card = UiStyles.card("Traffic Metrics");
        GridPane grid = UiStyles.metricGrid();
        total = UiStyles.addRow(grid, 0, "Vehicles total");
        queued = UiStyles.addRow(grid, 1, "Waiting in queues");
        crossing = UiStyles.addRow(grid, 2, "Crossing now");
        completed = UiStyles.addRow(grid, 3, "Cleared");
        emergencies = UiStyles.addRow(grid, 4, "Active emergencies");
        suspended = UiStyles.addRow(grid, 5, "Suspended (normal)");
        card.getChildren().add(grid);
        getChildren().add(card);
    }

    public void update(SimulationController c) {
        if (c == null) {
            reset();
            return;
        }
        int waiting = c.lanes().stream().mapToInt(Lane::queueLength).sum();
        total.setText(String.valueOf(c.totalVehicles()));
        queued.setText(String.valueOf(waiting));
        crossing.setText(c.running() == null ? "0" : "1 (V" + c.running().id() + ")");
        completed.setText(String.valueOf(c.completedCount()));
        emergencies.setText(String.valueOf(c.emergencyVehicleHandler().activeCount()));
        suspended.setText(String.valueOf(c.normalVehicleSuspender().suspendedCount()));
    }

    public void reset() {
        total.setText("-");
        queued.setText("-");
        crossing.setText("-");
        completed.setText("-");
        emergencies.setText("-");
        suspended.setText("-");
    }
}
