package com.trafficsim.ui;

import com.trafficsim.metrics.RunMetrics;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.Locale;

/**
 * Live display of the four headline scheduling-performance metrics (testable signal for FR-03):
 * average waiting time, average turnaround time, average response time and throughput. Values are
 * recomputed from completed vehicles as the run progresses.
 */
public final class PerformanceMetricDisplayer extends VBox {

    private final Label avgWaiting;
    private final Label avgTurnaround;
    private final Label avgResponse;
    private final Label throughput;

    public PerformanceMetricDisplayer() {
        VBox card = UiStyles.card("Performance Metrics");
        GridPane grid = UiStyles.metricGrid();
        avgWaiting = UiStyles.addRow(grid, 0, "Avg waiting (ticks)");
        avgTurnaround = UiStyles.addRow(grid, 1, "Avg turnaround (ticks)");
        avgResponse = UiStyles.addRow(grid, 2, "Avg response (ticks)");
        throughput = UiStyles.addRow(grid, 3, "Throughput /100 ticks");
        card.getChildren().add(grid);
        getChildren().add(card);
    }

    /** Updates the figures from a (possibly partial) metrics snapshot. */
    public void update(RunMetrics metrics) {
        if (metrics == null) {
            reset();
            return;
        }
        avgWaiting.setText(fmt(metrics.avgWaitingTime()));
        avgTurnaround.setText(fmt(metrics.avgTurnaroundTime()));
        avgResponse.setText(fmt(metrics.avgResponseTime()));
        throughput.setText(fmt(metrics.throughputPer100Ticks()));
    }

    public void reset() {
        avgWaiting.setText("-");
        avgTurnaround.setText("-");
        avgResponse.setText("-");
        throughput.setText("-");
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }
}
