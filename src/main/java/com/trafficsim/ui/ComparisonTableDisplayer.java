package com.trafficsim.ui;

import com.trafficsim.metrics.RunMetrics;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Side-by-side comparison table of every algorithm's metrics on the identical dataset (testable
 * signal for FR-04). One row per algorithm, columns for the four averages plus makespan and
 * completion count.
 */
public final class ComparisonTableDisplayer extends VBox {

    private final TableView<RunMetrics> table = new TableView<>();

    public ComparisonTableDisplayer() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new javafx.scene.control.Label("Run a comparison to populate this table."));
        table.setPrefHeight(180);

        addTextColumn("Algorithm", RunMetrics::algorithmKey);
        addNumberColumn("Avg Wait", RunMetrics::avgWaitingTime);
        addNumberColumn("Avg Turnaround", RunMetrics::avgTurnaroundTime);
        addNumberColumn("Avg Response", RunMetrics::avgResponseTime);
        addNumberColumn("Throughput/100", RunMetrics::throughputPer100Ticks);
        addTextColumn("Makespan", r -> String.valueOf(r.makespan()));
        addTextColumn("Done", r -> r.completedVehicles() + "/" + r.totalVehicles());

        VBox card = UiStyles.card("Comparison Table");
        card.getChildren().add(table);
        getChildren().add(card);
    }

    /** Replaces the table contents with a new comparison result set. */
    public void show(List<RunMetrics> results) {
        table.setItems(FXCollections.observableArrayList(results));
    }

    public void clear() {
        table.getItems().clear();
    }

    private void addTextColumn(String title, Function<RunMetrics, String> extractor) {
        TableColumn<RunMetrics, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cellFactory(extractor));
        table.getColumns().add(col);
    }

    private void addNumberColumn(String title, Function<RunMetrics, Double> extractor) {
        addTextColumn(title, r -> String.format(Locale.ROOT, "%.2f", extractor.apply(r)));
    }

    private static Callback<TableColumn.CellDataFeatures<RunMetrics, String>, javafx.beans.value.ObservableValue<String>>
    cellFactory(Function<RunMetrics, String> extractor) {
        return data -> new ReadOnlyStringWrapper(extractor.apply(data.getValue()));
    }
}
