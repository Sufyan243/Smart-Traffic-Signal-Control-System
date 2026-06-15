package com.trafficsim.ui;

import com.trafficsim.metrics.RunMetrics;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Grouped bar chart comparing the average waiting, turnaround and response times of every algorithm
 * on the identical dataset (testable signal for FR-04). Lower bars are better, which makes the
 * trade-offs between the algorithms immediately legible at a defense.
 */
public final class ComparisonChartDisplayer extends VBox {

    private final BarChart<String, Number> chart;

    public ComparisonChartDisplayer() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Algorithm");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Ticks (lower is better)");

        chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Average Times by Algorithm");
        chart.setAnimated(false);
        chart.setPrefHeight(300);

        VBox card = UiStyles.card("Comparison Chart");
        card.getChildren().add(chart);
        getChildren().add(card);
    }

    /** Rebuilds the chart from a comparison result set. */
    public void show(List<RunMetrics> results) {
        chart.getData().clear();

        XYChart.Series<String, Number> waiting = new XYChart.Series<>();
        waiting.setName("Avg Waiting");
        XYChart.Series<String, Number> turnaround = new XYChart.Series<>();
        turnaround.setName("Avg Turnaround");
        XYChart.Series<String, Number> response = new XYChart.Series<>();
        response.setName("Avg Response");

        for (RunMetrics r : results) {
            waiting.getData().add(new XYChart.Data<>(r.algorithmKey(), r.avgWaitingTime()));
            turnaround.getData().add(new XYChart.Data<>(r.algorithmKey(), r.avgTurnaroundTime()));
            response.getData().add(new XYChart.Data<>(r.algorithmKey(), r.avgResponseTime()));
        }

        chart.getData().add(waiting);
        chart.getData().add(turnaround);
        chart.getData().add(response);
    }

    public void clear() {
        chart.getData().clear();
    }
}
