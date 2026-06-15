package com.trafficsim.scheduling;

import com.trafficsim.domain.ScenarioConfig;
import com.trafficsim.domain.Vehicle;
import com.trafficsim.engine.SimulationController;
import com.trafficsim.metrics.GanttSegment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.trafficsim.support.EngineTestSupport.byId;
import static com.trafficsim.support.EngineTestSupport.config;
import static com.trafficsim.support.EngineTestSupport.normal;
import static com.trafficsim.support.EngineTestSupport.runToCompletion;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Round Robin (quantum = 2) hand-computed Gantt chart. V1 (burst 5) and V2 (burst 3) both arrive at
 * tick 0 and share the intersection in 2-tick slices:
 *
 * <pre>
 *   | V1 0-2 | V2 2-4 | V1 4-6 | V2 6-7 | V1 7-8 |
 *   completion: V2=7, V1=8 ; makespan=8
 *   waiting:    V1 = 8-5 = 3 , V2 = 7-3 = 4
 * </pre>
 */
class RoundRobinStrategyTest {

    @Test
    void rotatesEveryQuantum() {
        List<Vehicle> dataset = List.of(
                normal(1, 0, 5, 5),
                normal(2, 0, 3, 5));
        ScenarioConfig config = config(2, 100);

        SimulationController c = runToCompletion(new RoundRobinStrategy(), dataset, config);

        assertEquals(7, byId(dataset, 2).completionTime());
        assertEquals(8, byId(dataset, 1).completionTime());
        assertEquals(8, c.metricsCollector().makespan());

        // turnaround - burst
        assertEquals(3, byId(dataset, 1).completionTime() - byId(dataset, 1).arrivalTime() - byId(dataset, 1).burstTime());
        assertEquals(4, byId(dataset, 2).completionTime() - byId(dataset, 2).arrivalTime() - byId(dataset, 2).burstTime());

        List<GanttSegment> gantt = c.metricsCollector().segments();
        assertEquals(5, gantt.size());
        assertEquals(1, gantt.get(0).vehicleId());
        assertEquals(2, gantt.get(0).endTick());
        assertEquals(2, gantt.get(1).vehicleId());
        assertEquals(1, gantt.get(2).vehicleId());
        assertEquals(2, gantt.get(3).vehicleId());
        assertEquals(1, gantt.get(4).vehicleId());
    }

    @Test
    void loneVehicleIsNotPreempted() {
        // With nobody else waiting, RR must let a single vehicle cross in one continuous block.
        List<Vehicle> dataset = List.of(normal(1, 0, 5, 5));
        SimulationController c = runToCompletion(new RoundRobinStrategy(), dataset, config(2, 100));

        assertEquals(5, byId(dataset, 1).completionTime());
        assertEquals(1, c.metricsCollector().segments().size());
    }
}
