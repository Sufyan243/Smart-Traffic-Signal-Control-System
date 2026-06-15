package com.trafficsim.ui;

import com.trafficsim.metrics.RunMetrics;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Locale;

/**
 * Shows the final outcome of a completed live run (testable signal for FR-02 "displays the
 * simulation results"). Hidden until a run finishes, then summarises the makespan, completion count
 * and the four averages for the algorithm that just ran.
 */
public final class SimulationResultDisplayer extends VBox {

    private final Label headline;
    private final Label makespan;
    private final Label completed;
    private final Label avgWaiting;
    private final Label avgTurnaround;

    public SimulationResultDisplayer() {
        VBox card = UiStyles.card("Simulation Result");
        headline = new Label("Run a simulation to see results.");
        headline.setFont(Font.font("System", FontWeight.SEMI_BOLD, 12));
        headline.setStyle("-fx-text-fill: " + UiStyles.MUTED + ";");

        GridPane grid = UiStyles.metricGrid();
        makespan = UiStyles.addRow(grid, 0, "Makespan (ticks)");
        completed = UiStyles.addRow(grid, 1, "Completed");
        avgWaiting = UiStyles.addRow(grid, 2, "Avg waiting");
        avgTurnaround = UiStyles.addRow(grid, 3, "Avg turnaround");

        card.getChildren().addAll(headline, grid);
        getChildren().add(card);
    }

    /** Populates the panel from the finished run's metrics. */
    public void show(RunMetrics metrics) {
        if (metrics == null) {
            return;
        }
        headline.setText(metrics.algorithmName() + " — finished");
        headline.setStyle("-fx-text-fill: " + UiStyles.ACCENT + ";");
        makespan.setText(String.valueOf(metrics.makespan()));
        completed.setText(metrics.completedVehicles() + "/" + metrics.totalVehicles());
        avgWaiting.setText(fmt(metrics.avgWaitingTime()));
        avgTurnaround.setText(fmt(metrics.avgTurnaroundTime()));
    }

    public void reset() {
        headline.setText("Run a simulation to see results.");
        headline.setStyle("-fx-text-fill: " + UiStyles.MUTED + ";");
        makespan.setText("-");
        completed.setText("-");
        avgWaiting.setText("-");
        avgTurnaround.setText("-");
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }
}
