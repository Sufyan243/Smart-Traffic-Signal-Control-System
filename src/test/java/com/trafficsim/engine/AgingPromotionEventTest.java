package com.trafficsim.engine;

import com.trafficsim.domain.ScenarioConfig;
import com.trafficsim.domain.Vehicle;
import com.trafficsim.scenario.ScenarioManager;
import com.trafficsim.scenario.VehicleTypeSelector;
import com.trafficsim.scheduling.PriorityAgingStrategy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.trafficsim.support.EngineTestSupport.config;
import static com.trafficsim.support.EngineTestSupport.normal;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FR-07: when aging promotes a starving vehicle the engine must fire an AGING_PROMOTION event (so the
 * UI can flag it) and count it. Uses the same dataset as the aging scheduler unit test.
 */
class AgingPromotionEventTest {

    @Test
    void agingPromotionIsEmittedAndCounted() {
        List<Vehicle> dataset = List.of(
                normal(1, 0, 2, 3),
                normal(2, 0, 2, 1),
                normal(3, 2, 2, 1));
        ScenarioConfig cfg = config(2, 2);

        SimulationController c = new SimulationController(new ScenarioManager(new VehicleTypeSelector()));
        c.load(new PriorityAgingStrategy(), dataset, cfg);

        List<SimEvent> events = new ArrayList<>();
        int guard = 0;
        while (!c.isFinished() && guard++ < 1000) {
            c.step();
            events.addAll(c.drainEvents());
        }

        boolean promotionEvent = events.stream()
                .anyMatch(e -> e.type() == SimEvent.Type.AGING_PROMOTION && e.vehicleId() == 1);
        assertTrue(promotionEvent, "an AGING_PROMOTION event should fire for the starved vehicle");
        assertTrue(c.agingPromotionCount() >= 1, "the promotion should be counted");
    }
}
