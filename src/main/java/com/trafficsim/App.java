package com.trafficsim;

import com.trafficsim.domain.ScenarioConfig;
import com.trafficsim.domain.Vehicle;
import com.trafficsim.engine.SimEvent;
import com.trafficsim.engine.SimulationController;
import com.trafficsim.metrics.AlgorithmComparator;
import com.trafficsim.metrics.ComparisonReportGenerator;
import com.trafficsim.metrics.MetricsCalculator;
import com.trafficsim.metrics.RunMetrics;
import com.trafficsim.scenario.ScenarioManager;
import com.trafficsim.scenario.TrafficScenarioConfigurator;
import com.trafficsim.scheduling.SchedulingAlgorithmSelector;
import com.trafficsim.scheduling.SchedulingStrategy;
import com.trafficsim.ui.ControlPanel;
import com.trafficsim.ui.MainView;
import com.trafficsim.ui.MetricsDashboard;
import com.trafficsim.ui.SimulationVisualizer;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * JavaFX application entry point and wiring. Owns the long-lived domain services and the single
 * animation loop that drives the {@link SimulationController} one tick at a time, then refreshes the
 * view and dashboards. Implements {@link ControlPanel.Actions} so every button maps to one method
 * here.
 */
public final class App extends Application implements ControlPanel.Actions {

    private final TrafficScenarioConfigurator configurator = new TrafficScenarioConfigurator();
    private final ScenarioManager scenarioManager = new ScenarioManager(configurator.vehicleTypeSelector());
    private final SchedulingAlgorithmSelector algorithmSelector = new SchedulingAlgorithmSelector();
    private final SimulationController controller = new SimulationController(scenarioManager);
    private final AlgorithmComparator algorithmComparator = new AlgorithmComparator(algorithmSelector, scenarioManager);
    private final ComparisonReportGenerator reportGenerator = new ComparisonReportGenerator();
    private final MetricsCalculator liveCalculator = new MetricsCalculator();

    private ControlPanel controlPanel;
    private SimulationVisualizer visualizer;
    private MetricsDashboard dashboard;
    private Stage stage;

    // animation-loop state
    private boolean loaded;
    private boolean playing;
    private boolean resultShown;
    private double ticksPerSecond = 5;
    private long lastNanos;
    private double accumulator;

    // last comparison artefacts, kept for CSV export
    private List<RunMetrics> lastResults;
    private List<Vehicle> lastDataset;
    private ScenarioConfig lastConfig;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        controlPanel = new ControlPanel(configurator, algorithmSelector, this);
        visualizer = new SimulationVisualizer();
        dashboard = new MetricsDashboard();

        MainView root = new MainView(controlPanel, visualizer, dashboard);
        Scene scene = new Scene(root, 1320, 780);

        primaryStage.setTitle("Smart Traffic Signal Control System — OS Scheduling Simulator");
        primaryStage.setScene(scene);
        primaryStage.show();

        startLoop();
    }

    private void startLoop() {
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastNanos == 0) {
                    lastNanos = now;
                    return;
                }
                double dt = (now - lastNanos) / 1_000_000_000.0;
                lastNanos = now;

                if (loaded && playing && !controller.isFinished()) {
                    accumulator += dt * ticksPerSecond;
                    int guard = 0;
                    while (accumulator >= 1 && !controller.isFinished() && guard++ < 2000) {
                        controller.step();
                        accumulator -= 1;
                    }
                }
                drainEvents();
                renderFrame();
            }
        }.start();
    }

    private void drainEvents() {
        for (SimEvent event : controller.drainEvents()) {
            visualizer.acceptEvent(event);
        }
    }

    private void renderFrame() {
        visualizer.render(controller);
        RunMetrics live = loaded ? liveMetrics() : null;
        dashboard.updateLive(controller, live);
        dashboard.updateActivity(
                visualizer.vehicleMovementDisplayer().recentMovements(),
                visualizer.signalChangeDisplayer().recentChanges());

        if (loaded && controller.isFinished() && !resultShown) {
            resultShown = true;
            playing = false;
            dashboard.showResult(live);
            controlPanel.setRunning(false);
            controlPanel.setStatus("Finished " + controller.strategy().shortName()
                    + " in " + controller.clock().currentTick() + " ticks");
        }
    }

    private RunMetrics liveMetrics() {
        SchedulingStrategy strategy = controller.strategy();
        if (strategy == null) {
            return null;
        }
        return liveCalculator.calculate(
                strategy.name(), strategy.shortName(),
                controller.allVehicles(),
                controller.metricsCollector().segments(),
                controller.clock().currentTick());
    }

    // --- ControlPanel.Actions ---

    @Override
    public void onStart() {
        if (loadFreshRun()) {
            playing = true;
            controlPanel.setRunning(true);
            controlPanel.setStatus("Running " + controller.strategy().shortName());
        }
    }

    @Override
    public void onPauseToggle() {
        if (!loaded) {
            return;
        }
        playing = !playing;
        controlPanel.setRunning(playing);
        controlPanel.setStatus(playing ? "Running" : "Paused");
    }

    @Override
    public void onStep() {
        if (!loaded && !loadFreshRun()) {
            return;
        }
        playing = false;
        controlPanel.setRunning(false);
        controller.step();
        controlPanel.setStatus("Stepped to tick " + controller.clock().currentTick());
    }

    @Override
    public void onReset() {
        if (loadFreshRun()) {
            playing = false;
            controlPanel.setRunning(false);
            controlPanel.setStatus("Reset — ready");
        }
    }

    @Override
    public void onInjectEmergency(int laneId) {
        if (!loaded) {
            controlPanel.setStatus("Start a run before injecting an emergency");
            return;
        }
        Vehicle e = controller.injectEmergency(laneId);
        controlPanel.setStatus(e == null ? "Run already finished"
                : "Injected emergency V" + e.id() + " in lane " + laneId);
    }

    @Override
    public void onRunComparison() {
        try {
            ScenarioConfig config = configurator.build();
            List<Vehicle> dataset = scenarioManager.regenerate(config);
            List<RunMetrics> results = algorithmComparator.compareAll(dataset, config);
            lastResults = results;
            lastDataset = dataset;
            lastConfig = config;
            dashboard.showComparison(results);
            controlPanel.setStatus("Compared " + results.size() + " algorithms on seed " + config.seed());
        } catch (IllegalArgumentException ex) {
            controlPanel.setStatus("Invalid config: " + ex.getMessage());
        }
    }

    @Override
    public void onExportCsv() {
        if (lastResults == null || lastDataset == null) {
            controlPanel.setStatus("Run a comparison before exporting");
            return;
        }
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose export folder");
        File dir = chooser.showDialog(stage);
        if (dir == null) {
            return;
        }
        try {
            Path out = reportGenerator.export(dir.toPath(), lastConfig, lastDataset, lastResults);
            controlPanel.setStatus("Exported CSV to " + out);
        } catch (IOException ex) {
            controlPanel.setStatus("Export failed: " + ex.getMessage());
        }
    }

    @Override
    public void onSpeedChanged(double ticksPerSecond) {
        this.ticksPerSecond = ticksPerSecond;
    }

    /** Builds config, regenerates the dataset and loads the selected algorithm. Returns success. */
    private boolean loadFreshRun() {
        try {
            ScenarioConfig config = configurator.build();
            List<Vehicle> dataset = scenarioManager.regenerate(config);
            SchedulingStrategy strategy = algorithmSelector.create(controlPanel.selectedAlgorithmKey());
            controller.load(strategy, dataset, config);
            lastDataset = dataset;
            lastConfig = config;
            visualizer.clearLogs();
            dashboard.reset();
            accumulator = 0;
            resultShown = false;
            loaded = true;
            return true;
        } catch (IllegalArgumentException ex) {
            controlPanel.setStatus("Invalid config: " + ex.getMessage());
            return false;
        }
    }
}
