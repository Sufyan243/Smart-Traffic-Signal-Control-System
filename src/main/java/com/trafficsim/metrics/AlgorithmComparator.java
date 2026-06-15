package com.trafficsim.metrics;

import com.trafficsim.domain.ScenarioConfig;
import com.trafficsim.domain.Vehicle;
import com.trafficsim.engine.SimulationRunner;
import com.trafficsim.scenario.ScenarioManager;
import com.trafficsim.scheduling.SchedulingAlgorithmSelector;
import com.trafficsim.scheduling.SchedulingStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs every scheduling algorithm over the <em>same</em> seeded dataset and collects their
 * {@link RunMetrics} for side-by-side comparison (testable signal for FR-04).
 *
 * <p>Each algorithm replays an identical workload (the dataset's runtime state is reset between
 * runs), so any difference in the resulting metrics is attributable to the algorithm alone — the
 * reproducibility guarantee that makes the comparison meaningful.</p>
 */
public final class AlgorithmComparator {

    private final SchedulingAlgorithmSelector selector;
    private final SimulationRunner runner;
    private final ScenarioManager scenarioManager;

    public AlgorithmComparator(SchedulingAlgorithmSelector selector, ScenarioManager scenarioManager) {
        this.selector = selector;
        this.scenarioManager = scenarioManager;
        this.runner = new SimulationRunner(scenarioManager);
    }

    /** Runs all algorithms over a freshly generated dataset for {@code config}. */
    public List<RunMetrics> compareAll(ScenarioConfig config) {
        List<Vehicle> dataset = scenarioManager.regenerate(config);
        return compareAll(dataset, config);
    }

    /** Runs all algorithms over the given dataset, resetting it between runs. */
    public List<RunMetrics> compareAll(List<Vehicle> dataset, ScenarioConfig config) {
        List<RunMetrics> results = new ArrayList<>();
        for (SchedulingStrategy strategy : selector.createAll()) {
            for (Vehicle v : dataset) {
                v.resetRuntime();   // identical starting point for every algorithm
            }
            results.add(runner.run(strategy, dataset, config));
        }
        return results;
    }
}
