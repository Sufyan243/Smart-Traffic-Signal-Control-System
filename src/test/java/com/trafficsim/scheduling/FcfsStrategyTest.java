package com.trafficsim.scheduling;

import com.trafficsim.domain.ScenarioConfig;
import com.trafficsim.domain.Vehicle;
import com.trafficsim.engine.SimulationController;
import com.trafficsim.metrics.GanttSegment;
import com.trafficsim.metrics.MetricsCalculator;
import com.trafficsim.metrics.RunMetrics;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.trafficsim.support.EngineTestSupport.byId;
import static com.trafficsim.support.EngineTestSupport.config;
import static com.trafficsim.support.EngineTestSupport.normal;
import static com.trafficsim.support.EngineTestSupport.runToCompletion;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * FCFS hand-computed Gantt chart. Three vehicles all arrive at tick 0 with bursts 5, 3, 2 and must
 * run strictly in arrival/id order:
 *
 * <pre>
 *   | V1 (0-5) | V2 (5-8) | V3 (8-10) |
 *   completion: V1=5, V2=8, V3=10
 *   waiting:    V1=0, V2=5, V3=8   -> avg 13/3
 * </pre>
 */
class FcfsStrategyTest {

    @Test
    void runsInArrivalOrder() {
        List<Vehicle> dataset = List.of(
                normal(1, 0, 5, 5),
                normal(2, 0, 3, 5),
                normal(3, 0, 2, 5));
        ScenarioConfig config = config(2, 100);

        SimulationController c = runToCompletion(new FcfsStrategy(), dataset, config);

        assertEquals(5, byId(dataset, 1).completionTime());
        assertEquals(8, byId(dataset, 2).completionTime());
        assertEquals(10, byId(dataset, 3).completionTime());
        assertEquals(10, c.metricsCollector().makespan());

        List<GanttSegment> gantt = c.metricsCollector().segments();
        assertEquals(3, gantt.size());
        assertEquals(1, gantt.get(0).vehicleId());
        assertEquals(0, gantt.get(0).startTick());
        assertEquals(5, gantt.get(0).endTick());
        assertEquals(2, gantt.get(1).vehicleId());
        assertEquals(3, gantt.get(2).vehicleId());
    }

    @Test
    void averageWaitingMatchesHandComputation() {
        List<Vehicle> dataset = List.of(
                normal(1, 0, 5, 5),
                normal(2, 0, 3, 5),
                normal(3, 0, 2, 5));
        ScenarioConfig config = config(2, 100);

        SimulationController c = runToCompletion(new FcfsStrategy(), dataset, config);
        RunMetrics m = new MetricsCalculator().calculate("FCFS", "FCFS",
                c.allVehicles(), c.metricsCollector().segments(), c.metricsCollector().makespan());

        assertEquals(13.0 / 3.0, m.avgWaitingTime(), 1e-9);
    }
}
