package com.trafficsim.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Small shared styling helpers so the UI widgets share one dark theme without a heavy CSS file.
 */
final class UiStyles {

    static final String BG_PANEL = "#0f172a";
    static final String BG_CARD = "#1e293b";
    static final String TEXT = "#e2e8f0";
    static final String MUTED = "#94a3b8";
    static final String ACCENT = "#4a9eff";

    private UiStyles() {
    }

    /** A titled card container with the shared dark style. */
    static VBox card(String title) {
        Label header = new Label(title);
        header.setFont(Font.font("System", FontWeight.BOLD, 13));
        header.setStyle("-fx-text-fill: " + TEXT + ";");

        VBox box = new VBox(8, header);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-background-color: " + BG_CARD + "; -fx-background-radius: 10;");
        return box;
    }

    /** Adds a "label : value" row to {@code grid} and returns the value label for later updates. */
    static Label addRow(GridPane grid, int row, String label) {
        Label key = new Label(label);
        key.setStyle("-fx-text-fill: " + MUTED + ";");
        key.setFont(Font.font(12));

        Label value = new Label("-");
        value.setStyle("-fx-text-fill: " + TEXT + ";");
        value.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));

        grid.add(key, 0, row);
        grid.add(value, 1, row);
        return value;
    }

    static GridPane metricGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(6);
        return grid;
    }

    static Region grow() {
        Region spacer = new Region();
        spacer.setMinWidth(8);
        return spacer;
    }
}
