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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SJF hand-computed Gantt chart. Same three vehicles all arriving at tick 0 (bursts 5, 3, 2) must
 * run shortest-first:
 *
 * <pre>
 *   | V3 (0-2) | V2 (2-5) | V1 (5-10) |
 *   completion: V3=2, V2=5, V1=10
 *   waiting:    V3=0, V2=2, V1=5   -> avg 7/3  (lower than FCFS's 13/3)
 * </pre>
 */
class SjfStrategyTest {

    private static List<Vehicle> dataset() {
        return List.of(
                normal(1, 0, 5, 5),
                normal(2, 0, 3, 5),
                normal(3, 0, 2, 5));
    }

    @Test
    void runsShortestFirst() {
        List<Vehicle> dataset = dataset();
        SimulationController c = runToCompletion(new SjfStrategy(), dataset, config(2, 100));

        assertEquals(2, byId(dataset, 3).completionTime());
        assertEquals(5, byId(dataset, 2).completionTime());
        assertEquals(10, byId(dataset, 1).completionTime());

        List<GanttSegment> gantt = c.metricsCollector().segments();
        assertEquals(3, gantt.size());
        assertEquals(3, gantt.get(0).vehicleId());
        assertEquals(2, gantt.get(1).vehicleId());
        assertEquals(1, gantt.get(2).vehicleId());
    }

    @Test
    void beatsFcfsOnAverageWaiting() {
        List<Vehicle> sjf = dataset();
        SimulationController cSjf = runToCompletion(new SjfStrategy(), sjf, config(2, 100));
        RunMetrics mSjf = new MetricsCalculator().calculate("SJF", "SJF",
                cSjf.allVehicles(), cSjf.metricsCollector().segments(), cSjf.metricsCollector().makespan());
        assertEquals(7.0 / 3.0, mSjf.avgWaitingTime(), 1e-9);

        List<Vehicle> fcfs = dataset();
        SimulationController cFcfs = runToCompletion(new FcfsStrategy(), fcfs, config(2, 100));
        RunMetrics mFcfs = new MetricsCalculator().calculate("FCFS", "FCFS",
                cFcfs.allVehicles(), cFcfs.metricsCollector().segments(), cFcfs.metricsCollector().makespan());

        assertTrue(mSjf.avgWaitingTime() <= mFcfs.avgWaitingTime(),
                "SJF should minimise average waiting time for equal arrivals");
    }
}
