package com.trafficsim.engine;

import com.trafficsim.domain.ScenarioConfig;
import com.trafficsim.domain.Vehicle;
import com.trafficsim.metrics.MetricsCalculator;
import com.trafficsim.metrics.RunMetrics;
import com.trafficsim.scenario.ScenarioManager;
import com.trafficsim.scheduling.SchedulingStrategy;

import java.util.List;

/**
 * Runs one scheduling algorithm over a dataset to completion, headlessly (no animation), and returns
 * its {@link RunMetrics} (testable signal for FR-02). This is the same engine the UI uses, just
 * driven in a tight loop instead of one tick per frame — which is exactly why the animated run and
 * the measured numbers agree.
 */
public final class SimulationRunner {

    private final ScenarioManager scenarioManager;
    private final MetricsCalculator metricsCalculator = new MetricsCalculator();

    public SimulationRunner(ScenarioManager scenarioManager) {
        this.scenarioManager = scenarioManager;
    }

    /** Executes {@code strategy} over {@code dataset} under {@code config} and computes its metrics. */
    public RunMetrics run(SchedulingStrategy strategy, List<Vehicle> dataset, ScenarioConfig config) {
        SimulationController controller = new SimulationController(scenarioManager);
        controller.load(strategy, dataset, config);
        while (!controller.isFinished()) {
            controller.step();
        }
        return metricsCalculator.calculate(
                strategy.name(),
                strategy.shortName(),
                controller.allVehicles(),
                controller.metricsCollector().segments(),
                controller.metricsCollector().makespan());
    }
}
