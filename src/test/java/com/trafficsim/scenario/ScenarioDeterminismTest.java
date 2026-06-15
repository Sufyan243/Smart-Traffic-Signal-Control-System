package com.trafficsim.scenario;

import com.trafficsim.domain.ScenarioConfig;
import com.trafficsim.domain.Vehicle;
import com.trafficsim.metrics.AlgorithmComparator;
import com.trafficsim.metrics.RunMetrics;
import com.trafficsim.scheduling.SchedulingAlgorithmSelector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the project's central non-functional requirement: the same seed always produces the same
 * workload, so cross-algorithm comparison is reproducible.
 */
class ScenarioDeterminismTest {

    private ScenarioManager managerWithAllTypes() {
        return new ScenarioManager(new VehicleTypeSelector());
    }

    @Test
    void sameSeedProducesIdenticalDataset() {
        ScenarioConfig config = ScenarioConfig.builder().seed(12345L).build();

        List<Vehicle> first = managerWithAllTypes().regenerate(config);
        List<Vehicle> second = managerWithAllTypes().regenerate(config);

        assertEquals(first.size(), second.size());
        for (int i = 0; i < first.size(); i++) {
            Vehicle a = first.get(i);
            Vehicle b = second.get(i);
            assertEquals(a.id(), b.id());
            assertEquals(a.arrivalTime(), b.arrivalTime());
            assertEquals(a.burstTime(), b.burstTime());
            assertEquals(a.laneId(), b.laneId());
            assertEquals(a.type(), b.type());
        }
    }

    @Test
    void comparisonRunsEveryAlgorithmOnOneDataset() {
        ScenarioConfig config = ScenarioConfig.builder().seed(7L).build();
        ScenarioManager manager = managerWithAllTypes();
        AlgorithmComparator comparator = new AlgorithmComparator(new SchedulingAlgorithmSelector(), manager);

        List<RunMetrics> results = comparator.compareAll(config);

        assertEquals(4, results.size());
        for (RunMetrics r : results) {
            assertTrue(r.completedVehicles() > 0, r.algorithmKey() + " should complete vehicles");
            assertEquals(r.totalVehicles(), r.completedVehicles(),
                    r.algorithmKey() + " should clear every vehicle by the end of the run");
        }
    }
}
