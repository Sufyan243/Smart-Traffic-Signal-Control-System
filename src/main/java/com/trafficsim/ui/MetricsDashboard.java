package com.trafficsim.ui;

import com.trafficsim.engine.SimulationController;
import com.trafficsim.metrics.RunMetrics;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Right-hand dashboard (testable signal for FR-03/FR-04). A "Live" tab carries the three real-time
 * metric panels, the result summary and an activity feed; a "Comparison" tab carries the table and
 * chart produced by running every algorithm on the same dataset.
 */
public final class MetricsDashboard extends TabPane {

    private final PerformanceMetricDisplayer performanceMetricDisplayer = new PerformanceMetricDisplayer();
    private final TrafficMetricDisplayer trafficMetricDisplayer = new TrafficMetricDisplayer();
    private final SchedulingMetricDisplayer schedulingMetricDisplayer = new SchedulingMetricDisplayer();
    private final SimulationResultDisplayer simulationResultDisplayer = new SimulationResultDisplayer();
    private final ComparisonTableDisplayer comparisonTableDisplayer = new ComparisonTableDisplayer();
    private final ComparisonChartDisplayer comparisonChartDisplayer = new ComparisonChartDisplayer();
    private final GanttChartDisplayer ganttChartDisplayer = new GanttChartDisplayer();

    private final TextArea movementFeed = readOnlyArea();
    private final TextArea signalFeed = readOnlyArea();

    public MetricsDashboard() {
        setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
        setPrefWidth(360);

        VBox live = new VBox(12,
                schedulingMetricDisplayer,
                trafficMetricDisplayer,
                performanceMetricDisplayer,
                simulationResultDisplayer,
                activityCard());
        live.setPadding(new Insets(12));
        live.setStyle("-fx-background-color: " + UiStyles.BG_PANEL + ";");

        ScrollPane liveScroll = new ScrollPane(live);
        liveScroll.setFitToWidth(true);
        liveScroll.setStyle("-fx-background: " + UiStyles.BG_PANEL + "; -fx-background-color: " + UiStyles.BG_PANEL + ";");

        VBox comparison = new VBox(12, comparisonTableDisplayer, comparisonChartDisplayer, ganttChartDisplayer);
        comparison.setPadding(new Insets(12));
        comparison.setStyle("-fx-background-color: " + UiStyles.BG_PANEL + ";");
        ScrollPane comparisonScroll = new ScrollPane(comparison);
        comparisonScroll.setFitToWidth(true);
        comparisonScroll.setStyle("-fx-background: " + UiStyles.BG_PANEL + "; -fx-background-color: " + UiStyles.BG_PANEL + ";");

        Tab liveTab = new Tab("Live", liveScroll);
        Tab comparisonTab = new Tab("Comparison", comparisonScroll);
        getTabs().addAll(liveTab, comparisonTab);
    }

    /** Refreshes the three live panels from the current engine state and partial metrics. */
    public void updateLive(SimulationController controller, RunMetrics liveMetrics) {
        schedulingMetricDisplayer.update(controller);
        trafficMetricDisplayer.update(controller);
        performanceMetricDisplayer.update(liveMetrics);
    }

    public void updateActivity(List<String> movements, List<String> signals) {
        movementFeed.setText(String.join("\n", movements));
        signalFeed.setText(String.join("\n", signals));
    }

    public void showResult(RunMetrics finalMetrics) {
        simulationResultDisplayer.show(finalMetrics);
    }

    public void showComparison(List<RunMetrics> results) {
        comparisonTableDisplayer.show(results);
        comparisonChartDisplayer.show(results);
        ganttChartDisplayer.show(results);
        getSelectionModel().select(1); // jump to the Comparison tab
    }

    public void reset() {
        performanceMetricDisplayer.reset();
        trafficMetricDisplayer.reset();
        schedulingMetricDisplayer.reset();
        simulationResultDisplayer.reset();
        movementFeed.clear();
        signalFeed.clear();
    }

    private VBox activityCard() {
        VBox card = UiStyles.card("Activity Feed");
        Label movesLabel = mutedLabel("Vehicle movements");
        Label signalsLabel = mutedLabel("Signal changes");
        VBox left = new VBox(4, movesLabel, movementFeed);
        VBox right = new VBox(4, signalsLabel, signalFeed);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        HBox feeds = new HBox(10, left, right);
        card.getChildren().add(feeds);
        return card;
    }

    private static Label mutedLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + UiStyles.MUTED + "; -fx-font-size: 11;");
        return l;
    }

    private static TextArea readOnlyArea() {
        TextArea area = new TextArea();
        area.setEditable(false);
        area.setPrefRowCount(7);
        area.setWrapText(true);
        area.setStyle("-fx-control-inner-background: #0b1220; -fx-text-fill: #cbd5e1; -fx-font-size: 11;");
        return area;
    }
}
