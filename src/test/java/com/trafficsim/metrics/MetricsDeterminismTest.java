package com.trafficsim.metrics;

import com.trafficsim.domain.ScenarioConfig;
import com.trafficsim.scenario.ScenarioManager;
import com.trafficsim.scenario.VehicleTypeSelector;
import com.trafficsim.scheduling.SchedulingAlgorithmSelector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * NFR Determinism: identical dataset + algorithm must yield identical metrics on repeated runs. This
 * is the guarantee that makes the cross-algorithm comparison meaningful.
 */
class MetricsDeterminismTest {

    @Test
    void repeatedComparisonProducesIdenticalMetrics() {
        ScenarioConfig config = ScenarioConfig.builder().seed(2024L).durationTicks(40).build();

        List<RunMetrics> first = compare(config);
        List<RunMetrics> second = compare(config);

        assertEquals(first.size(), second.size());
        for (int i = 0; i < first.size(); i++) {
            RunMetrics a = first.get(i);
            RunMetrics b = second.get(i);
            assertEquals(a.algorithmKey(), b.algorithmKey());
            assertEquals(a.makespan(), b.makespan(), a.algorithmKey() + " makespan");
            assertEquals(a.avgWaitingTime(), b.avgWaitingTime(), 1e-12, a.algorithmKey() + " waiting");
            assertEquals(a.avgTurnaroundTime(), b.avgTurnaroundTime(), 1e-12, a.algorithmKey() + " turnaround");
            assertEquals(a.avgResponseTime(), b.avgResponseTime(), 1e-12, a.algorithmKey() + " response");
            assertEquals(a.throughputPer100Ticks(), b.throughputPer100Ticks(), 1e-12, a.algorithmKey() + " throughput");
        }
    }

    private List<RunMetrics> compare(ScenarioConfig config) {
        ScenarioManager manager = new ScenarioManager(new VehicleTypeSelector());
        return new AlgorithmComparator(new SchedulingAlgorithmSelector(), manager).compareAll(config);
    }
}
