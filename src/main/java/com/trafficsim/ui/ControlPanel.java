package com.trafficsim.ui;

import com.trafficsim.domain.VehicleType;
import com.trafficsim.scenario.TrafficScenarioConfigurator;
import com.trafficsim.scheduling.SchedulingAlgorithmSelector;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.StringConverter;

import java.util.stream.IntStream;

/**
 * Left-hand control panel (testable signal for FR-01/FR-02). Lets the user pick the algorithm, edit
 * every scenario parameter, choose vehicle types, drive the simulation (start / pause / step /
 * reset), inject an emergency vehicle, run the cross-algorithm comparison and export CSV.
 *
 * <p>Parameter edits are written straight into the shared {@link TrafficScenarioConfigurator}; the
 * actual work is delegated to {@link Actions} so this class stays pure UI.</p>
 */
public final class ControlPanel extends ScrollPane {

    /** Callbacks the panel fires; implemented by the application wiring. */
    public interface Actions {
        void onStart();
        void onPauseToggle();
        void onStep();
        void onReset();
        void onInjectEmergency(int laneId);
        void onRunComparison();
        void onExportCsv();
        void onSpeedChanged(double ticksPerSecond);
    }

    private final TrafficScenarioConfigurator configurator;
    private final ComboBox<SchedulingAlgorithmSelector.Option> algorithmBox;
    private final ComboBox<Integer> emergencyLaneBox;
    private final Spinner<Integer> laneSpinner;
    private final Button pauseButton;
    private final Label statusLabel = new Label("Idle");

    public ControlPanel(TrafficScenarioConfigurator configurator,
                        SchedulingAlgorithmSelector algorithmSelector,
                        Actions actions) {
        this.configurator = configurator;

        VBox content = new VBox(14);
        content.setPadding(new Insets(14));
        content.setStyle("-fx-background-color: " + UiStyles.BG_PANEL + ";");
        content.setPrefWidth(300);

        content.getChildren().add(title("Smart Traffic Signal Control"));

        // --- algorithm ---
        algorithmBox = new ComboBox<>(FXCollections.observableArrayList(algorithmSelector.options()));
        algorithmBox.setConverter(new StringConverter<>() {
            @Override public String toString(SchedulingAlgorithmSelector.Option o) {
                return o == null ? "" : o.displayName();
            }
            @Override public SchedulingAlgorithmSelector.Option fromString(String s) {
                return null;
            }
        });
        algorithmBox.getSelectionModel().selectFirst();
        algorithmBox.setMaxWidth(Double.MAX_VALUE);
        content.getChildren().add(labelled("Scheduling algorithm", algorithmBox));

        // --- scenario parameters ---
        laneSpinner = intSpinner(1, 8, configurator.build().laneCount());
        Spinner<Integer> durationSpinner = intSpinner(10, 600, configurator.build().durationTicks());
        Spinner<Integer> quantumSpinner = intSpinner(1, 20, configurator.signalParameterAdjuster().quantum());
        Spinner<Integer> agingSpinner = intSpinner(1, 50, configurator.signalParameterAdjuster().agingInterval());
        Spinner<Integer> seedSpinner = intSpinner(0, 100000, (int) configurator.build().seed());
        Slider arrivalSlider = ratioSlider(configurator.build().arrivalRate());
        Slider emergencySlider = ratioSlider(configurator.signalParameterAdjuster().emergencyRate());

        laneSpinner.valueProperty().addListener((o, a, v) -> {
            configurator.setLaneCount(v);
            refreshEmergencyLanes(v);
        });
        durationSpinner.valueProperty().addListener((o, a, v) -> configurator.setDurationTicks(v));
        quantumSpinner.valueProperty().addListener((o, a, v) -> configurator.signalParameterAdjuster().setQuantum(v));
        agingSpinner.valueProperty().addListener((o, a, v) -> configurator.signalParameterAdjuster().setAgingInterval(v));
        seedSpinner.valueProperty().addListener((o, a, v) -> configurator.setSeed(v));
        arrivalSlider.valueProperty().addListener((o, a, v) -> configurator.setArrivalRate(round2(v.doubleValue())));
        emergencySlider.valueProperty().addListener((o, a, v) ->
                configurator.signalParameterAdjuster().setEmergencyRate(round2(v.doubleValue())));

        GridPane params = new GridPane();
        params.setHgap(10);
        params.setVgap(8);
        int r = 0;
        addParam(params, r++, "Lanes", laneSpinner);
        addParam(params, r++, "Duration (ticks)", durationSpinner);
        addParam(params, r++, "RR quantum", quantumSpinner);
        addParam(params, r++, "Aging interval", agingSpinner);
        addParam(params, r++, "Seed", seedSpinner);
        addParam(params, r++, "Arrival rate", arrivalSlider);
        addParam(params, r++, "Emergency rate", emergencySlider);
        content.getChildren().add(card("Scenario Parameters", params));

        // --- vehicle types ---
        content.getChildren().add(card("Vehicle Types", vehicleTypeBox()));

        // --- transport controls ---
        Button startButton = primaryButton("Start", actions::onStart);
        pauseButton = button("Pause", actions::onPauseToggle);
        Button stepButton = button("Step", actions::onStep);
        Button resetButton = button("Reset", actions::onReset);
        HBox transport = new HBox(8, startButton, pauseButton, stepButton, resetButton);
        content.getChildren().add(card("Simulation", transport));

        // --- speed ---
        Slider speed = new Slider(1, 20, 5);
        speed.setShowTickLabels(true);
        speed.setMajorTickUnit(5);
        speed.valueProperty().addListener((o, a, v) -> actions.onSpeedChanged(v.doubleValue()));
        content.getChildren().add(card("Speed (ticks/sec)", speed));

        // --- emergency injection ---
        emergencyLaneBox = new ComboBox<>();
        refreshEmergencyLanes(configurator.build().laneCount());
        emergencyLaneBox.getSelectionModel().selectFirst();
        Button injectButton = button("Inject Emergency", () -> {
            Integer lane = emergencyLaneBox.getValue();
            if (lane != null) {
                actions.onInjectEmergency(lane);
            }
        });
        HBox emergencyRow = new HBox(8, new Label("Lane:"), emergencyLaneBox, injectButton);
        emergencyRow.setStyle("-fx-alignment: center-left;");
        content.getChildren().add(card("Emergency Vehicle", emergencyRow));

        // --- comparison / export ---
        Button compareButton = primaryButton("Run Comparison", actions::onRunComparison);
        Button exportButton = button("Export CSV", actions::onExportCsv);
        compareButton.setMaxWidth(Double.MAX_VALUE);
        exportButton.setMaxWidth(Double.MAX_VALUE);
        content.getChildren().add(card("Analysis", new VBox(8, compareButton, exportButton)));

        // --- status ---
        statusLabel.setStyle("-fx-text-fill: " + UiStyles.ACCENT + ";");
        content.getChildren().add(statusLabel);

        setContent(content);
        setFitToWidth(true);
        setStyle("-fx-background: " + UiStyles.BG_PANEL + "; -fx-background-color: " + UiStyles.BG_PANEL + ";");
    }

    public String selectedAlgorithmKey() {
        return algorithmBox.getValue().key();
    }

    public void setStatus(String text) {
        statusLabel.setText(text);
    }

    public void setRunning(boolean running) {
        pauseButton.setText(running ? "Pause" : "Resume");
    }

    private void refreshEmergencyLanes(int laneCount) {
        Integer previous = emergencyLaneBox.getValue();
        emergencyLaneBox.setItems(FXCollections.observableArrayList(
                IntStream.range(0, laneCount).boxed().toList()));
        if (previous != null && previous < laneCount) {
            emergencyLaneBox.setValue(previous);
        } else {
            emergencyLaneBox.getSelectionModel().selectFirst();
        }
    }

    private VBox vehicleTypeBox() {
        VBox box = new VBox(6);
        for (VehicleType type : VehicleType.values()) {
            CheckBox cb = new CheckBox(type.label() + " (burst " + type.defaultBurstTime()
                    + ", prio " + type.defaultPriority() + ")");
            cb.setSelected(true);
            cb.setStyle("-fx-text-fill: " + UiStyles.TEXT + ";");
            if (type == VehicleType.CAR) {
                cb.setDisable(true); // baseline type, always on
            } else {
                cb.selectedProperty().addListener((o, a, v) ->
                        configurator.vehicleTypeSelector().setEnabled(type, v));
            }
            box.getChildren().add(cb);
        }
        return box;
    }

    // --- small builders ---

    private static Label title(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("System", FontWeight.BOLD, 15));
        l.setWrapText(true);
        l.setStyle("-fx-text-fill: " + UiStyles.TEXT + ";");
        return l;
    }

    private static VBox card(String titleText, javafx.scene.Node body) {
        VBox c = UiStyles.card(titleText);
        c.getChildren().add(body);
        return c;
    }

    private static VBox labelled(String text, javafx.scene.Node node) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + UiStyles.MUTED + "; -fx-font-size: 12;");
        return new VBox(4, l, node);
    }

    private static void addParam(GridPane grid, int row, String label, javafx.scene.Node control) {
        Label l = new Label(label);
        l.setStyle("-fx-text-fill: " + UiStyles.MUTED + "; -fx-font-size: 12;");
        grid.add(l, 0, row);
        grid.add(control, 1, row);
    }

    private static Spinner<Integer> intSpinner(int min, int max, int initial) {
        Spinner<Integer> spinner = new Spinner<>();
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                min, max, Math.max(min, Math.min(max, initial))));
        spinner.setEditable(true);
        spinner.setPrefWidth(110);
        return spinner;
    }

    private static Slider ratioSlider(double initial) {
        Slider s = new Slider(0, 1, initial);
        s.setShowTickLabels(true);
        s.setMajorTickUnit(0.25);
        s.setPrefWidth(130);
        return s;
    }

    private Button button(String text, Runnable action) {
        Button b = new Button(text);
        b.setOnAction(e -> action.run());
        b.setStyle("-fx-background-color: #334155; -fx-text-fill: " + UiStyles.TEXT + ";");
        return b;
    }

    private Button primaryButton(String text, Runnable action) {
        Button b = button(text, action);
        b.setStyle("-fx-background-color: " + UiStyles.ACCENT + "; -fx-text-fill: white; -fx-font-weight: bold;");
        return b;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
