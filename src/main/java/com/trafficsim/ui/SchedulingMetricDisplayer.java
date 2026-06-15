package com.trafficsim.ui;

import com.trafficsim.domain.Vehicle;
import com.trafficsim.engine.SimulationController;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * Live display of scheduling-internal state (testable signal for FR-03): the active algorithm,
 * current tick, which lane holds the green light, the vehicle currently crossing and whether the
 * algorithm is preemptive. This is the "what is the scheduler deciding" panel.
 */
public final class SchedulingMetricDisplayer extends VBox {

    private final Label algorithm;
    private final Label tick;
    private final Label greenLane;
    private final Label currentVehicle;
    private final Label preemptive;
    private final Label agingPromotions;

    public SchedulingMetricDisplayer() {
        VBox card = UiStyles.card("Scheduling State");
        GridPane grid = UiStyles.metricGrid();
        algorithm = UiStyles.addRow(grid, 0, "Algorithm");
        tick = UiStyles.addRow(grid, 1, "Current tick");
        greenLane = UiStyles.addRow(grid, 2, "Green light");
        currentVehicle = UiStyles.addRow(grid, 3, "Crossing");
        preemptive = UiStyles.addRow(grid, 4, "Preemptive?");
        agingPromotions = UiStyles.addRow(grid, 5, "Aging promotions");
        card.getChildren().add(grid);
        getChildren().add(card);
    }

    public void update(SimulationController c) {
        if (c == null || c.strategy() == null) {
            reset();
            return;
        }
        Vehicle run = c.running();
        algorithm.setText(c.strategy().name());
        tick.setText(String.valueOf(c.clock().currentTick()));
        greenLane.setText(run == null ? "none (idle)" : c.lanes().get(run.laneId()).name());
        currentVehicle.setText(run == null ? "-"
                : "V" + run.id() + " (" + run.remainingTime() + "/" + run.burstTime() + " left)");
        preemptive.setText(c.strategy().isPreemptive() ? "yes" : "no");
        agingPromotions.setText(String.valueOf(c.agingPromotionCount()));
    }

    public void reset() {
        algorithm.setText("-");
        tick.setText("-");
        greenLane.setText("-");
        currentVehicle.setText("-");
        preemptive.setText("-");
        agingPromotions.setText("-");
    }
}
