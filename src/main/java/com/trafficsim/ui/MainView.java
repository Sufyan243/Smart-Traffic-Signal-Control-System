package com.trafficsim.ui;

import javafx.geometry.Insets;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

/**
 * Top-level three-pane layout: the {@link ControlPanel} on the left, the animated
 * {@link SimulationVisualizer} in the centre, and the {@link MetricsDashboard} on the right.
 */
public final class MainView extends BorderPane {

    public MainView(ControlPanel controlPanel, SimulationVisualizer visualizer, MetricsDashboard dashboard) {
        StackPane centre = new StackPane(visualizer);
        centre.setPadding(new Insets(12));
        centre.setStyle("-fx-background-color: #0b1220;");

        setLeft(controlPanel);
        setCenter(centre);
        setRight(dashboard);
        setStyle("-fx-background-color: #0b1220;");
    }
}
