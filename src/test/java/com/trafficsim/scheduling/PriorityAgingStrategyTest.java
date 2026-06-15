package com.trafficsim.scheduling;

import com.trafficsim.domain.ScenarioConfig;
import com.trafficsim.domain.Vehicle;
import com.trafficsim.engine.SimulationController;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.trafficsim.support.EngineTestSupport.byId;
import static com.trafficsim.support.EngineTestSupport.config;
import static com.trafficsim.support.EngineTestSupport.emergency;
import static com.trafficsim.support.EngineTestSupport.normal;
import static com.trafficsim.support.EngineTestSupport.runToCompletion;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Priority + Aging behaviour: preemption by a higher-priority arrival, emergency precedence, and
 * starvation prevention via aging.
 */
class PriorityAgingStrategyTest {

    /**
     * V1 (burst 5, low priority 5) starts; at tick 2 the higher-priority V2 (burst 3, priority 1)
     * arrives and preempts it. V2 runs to completion, then V1 resumes:
     * <pre>
     *   | V1 0-2 | V2 2-5 | V1 5-8 |
     *   completion: V2=5, V1=8 ; V2 waiting 0, V1 waiting 3
     * </pre>
     */
    @Test
    void higherPriorityArrivalPreempts() {
        List<Vehicle> dataset = List.of(
                normal(1, 0, 5, 5),
                normal(2, 2, 3, 1));
        ScenarioConfig config = config(2, 100); // aging effectively disabled

        SimulationController c = runToCompletion(new PriorityAgingStrategy(), dataset, config);

        assertEquals(5, byId(dataset, 2).completionTime());
        assertEquals(8, byId(dataset, 1).completionTime());
        assertEquals(0, byId(dataset, 2).completionTime() - byId(dataset, 2).arrivalTime() - byId(dataset, 2).burstTime());
        assertEquals(3, byId(dataset, 1).completionTime() - byId(dataset, 1).arrivalTime() - byId(dataset, 1).burstTime());
    }

    /** An emergency vehicle (priority 0) preempts whatever is crossing as soon as it arrives. */
    @Test
    void emergencyTakesPrecedence() {
        List<Vehicle> dataset = List.of(
                normal(1, 0, 6, 3),
                emergency(2, 2, 2));
        SimulationController c = runToCompletion(new PriorityAgingStrategy(), dataset, config(2, 100));

        // emergency arrives at 2, preempts, runs 2 ticks -> completes at 4, before V1 finishes
        assertEquals(4, byId(dataset, 2).completionTime());
        assertTrue(byId(dataset, 2).completionTime() < byId(dataset, 1).completionTime());
    }

    /**
     * Aging prevents starvation. With agingInterval = 2, the low-priority V1 (priority 3) is promoted
     * past a stream of priority-1 vehicles and finishes:
     * <pre>
     *   | V2 0-2 | V3 2-4 | V1 4-6 |   completion: V2=2, V3=4, V1=6
     * </pre>
     */
    @Test
    void agingPreventsStarvation() {
        List<Vehicle> dataset = List.of(
                normal(1, 0, 2, 3),
                normal(2, 0, 2, 1),
                normal(3, 2, 2, 1));
        ScenarioConfig config = config(2, 2); // aging every 2 ticks of waiting

        SimulationController c = runToCompletion(new PriorityAgingStrategy(), dataset, config);

        assertEquals(2, byId(dataset, 2).completionTime());
        assertEquals(4, byId(dataset, 3).completionTime());
        assertEquals(6, byId(dataset, 1).completionTime());
        assertTrue(byId(dataset, 1).wasPromotedByAging(),
                "low-priority vehicle should have been promoted by aging");
    }
}
