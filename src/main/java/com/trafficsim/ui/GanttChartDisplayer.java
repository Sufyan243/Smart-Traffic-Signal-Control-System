package com.trafficsim.ui;

import com.trafficsim.metrics.GanttSegment;
import com.trafficsim.metrics.RunMetrics;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.List;

/**
 * Renders the Gantt-chart-style crossing timeline of every algorithm on a shared time axis
 * (testable signal for FR-04). One track per algorithm; each block is a vehicle's crossing interval
 * coloured by vehicle type, so the difference in ordering (e.g. SJF front-loading short jobs, RR
 * interleaving, priority preemption) is visible at a glance.
 */
public final class GanttChartDisplayer extends VBox {

    private final Canvas canvas = new Canvas(640, 200);
    private List<RunMetrics> results = List.of();

    public GanttChartDisplayer() {
        Pane holder = new Pane(canvas);
        canvas.widthProperty().bind(holder.widthProperty());
        holder.setMinHeight(200);
        holder.heightProperty().addListener((o, a, b) -> redraw());
        canvas.widthProperty().addListener((o, a, b) -> redraw());

        VBox card = UiStyles.card("Gantt Timeline (per algorithm)");
        card.getChildren().add(holder);
        getChildren().add(card);
    }

    /** Supplies the per-algorithm runs to chart and redraws. */
    public void show(List<RunMetrics> results) {
        this.results = results == null ? List.of() : results;
        canvas.setHeight(Math.max(160, 28 + this.results.size() * 46));
        redraw();
    }

    public void clear() {
        show(List.of());
    }

    private void redraw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        gc.setFill(Color.web("#0b1220"));
        gc.fillRect(0, 0, w, h);

        if (results.isEmpty()) {
            gc.setFill(Color.web("#64748b"));
            gc.setFont(Font.font(13));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText("Run a comparison to see the Gantt timelines", w / 2, h / 2);
            return;
        }

        int maxMakespan = results.stream().mapToInt(RunMetrics::makespan).max().orElse(1);
        if (maxMakespan <= 0) {
            maxMakespan = 1;
        }

        double labelW = 56;
        double trackLeft = labelW + 6;
        double trackRight = w - 10;
        double trackW = Math.max(40, trackRight - trackLeft);
        double rowH = 40;
        double top = 18;

        // time axis ticks
        gc.setStroke(Color.web("#1e293b"));
        gc.setFill(Color.web("#475569"));
        gc.setFont(Font.font(9));
        gc.setTextAlign(TextAlignment.CENTER);
        int step = axisStep(maxMakespan);
        for (int t = 0; t <= maxMakespan; t += step) {
            double x = trackLeft + (t / (double) maxMakespan) * trackW;
            gc.strokeLine(x, top - 4, x, top + results.size() * rowH);
            gc.fillText(String.valueOf(t), x, top - 6);
        }

        for (int i = 0; i < results.size(); i++) {
            RunMetrics r = results.get(i);
            double y = top + i * rowH + 6;

            gc.setFill(Color.web("#e2e8f0"));
            gc.setFont(Font.font("System", FontWeight.BOLD, 11));
            gc.setTextAlign(TextAlignment.LEFT);
            gc.fillText(r.algorithmKey(), 6, y + rowH * 0.5);

            for (GanttSegment seg : r.gantt()) {
                double x = trackLeft + (seg.startTick() / (double) maxMakespan) * trackW;
                double segW = Math.max(1.5, (seg.duration() / (double) maxMakespan) * trackW);
                gc.setFill(seg.type().color());
                gc.fillRect(x, y, segW, rowH - 14);
                gc.setStroke(Color.web("#0b1220"));
                gc.setLineWidth(0.5);
                gc.strokeRect(x, y, segW, rowH - 14);

                if (segW > 16) {
                    gc.setFill(Color.web("#0f172a"));
                    gc.setFont(Font.font(9));
                    gc.setTextAlign(TextAlignment.CENTER);
                    gc.fillText("V" + seg.vehicleId(), x + segW / 2, y + (rowH - 14) / 2 + 3);
                }
            }
        }
    }

    private static int axisStep(int makespan) {
        if (makespan <= 20) return 2;
        if (makespan <= 60) return 5;
        if (makespan <= 150) return 10;
        return 25;
    }
}
