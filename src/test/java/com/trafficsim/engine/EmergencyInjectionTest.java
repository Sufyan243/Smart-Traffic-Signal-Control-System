package com.trafficsim.engine;

import com.trafficsim.domain.ScenarioConfig;
import com.trafficsim.domain.Vehicle;
import com.trafficsim.scenario.ScenarioManager;
import com.trafficsim.scenario.VehicleTypeSelector;
import com.trafficsim.scheduling.PriorityAgingStrategy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.trafficsim.support.EngineTestSupport.byId;
import static com.trafficsim.support.EngineTestSupport.config;
import static com.trafficsim.support.EngineTestSupport.normal;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FR-05: an emergency vehicle injected mid-run under Priority scheduling must preempt the vehicle
 * currently crossing, get a green path, and suspend the normal vehicle — and the engine must emit
 * the corresponding events with a non-colliding id.
 */
class EmergencyInjectionTest {

    @Test
    void injectedEmergencyPreemptsAndEmitsEvents() {
        List<Vehicle> dataset = List.of(normal(1, 0, 8, 3)); // one long, low-priority vehicle
        ScenarioConfig cfg = config(2, 100);

        SimulationController c = new SimulationController(new ScenarioManager(new VehicleTypeSelector()));
        c.load(new PriorityAgingStrategy(), dataset, cfg);

        List<SimEvent> events = new ArrayList<>();
        for (int i = 0; i < 3; i++) {           // let V1 start crossing
            c.step();
            events.addAll(c.drainEvents());
        }
        Vehicle emergency = c.injectEmergency(0);
        assertNotNull(emergency);
        assertNotEquals(1, emergency.id(), "injected id must not collide with the dataset vehicle");

        int guard = 0;
        while (!c.isFinished() && guard++ < 1000) {
            c.step();
            events.addAll(c.drainEvents());
        }

        // emergency must clear before the long normal vehicle it preempted
        assertTrue(byId(c.allVehicles(), emergency.id()).completionTime()
                        < byId(dataset, 1).completionTime(),
                "emergency should finish before the preempted normal vehicle");

        Set<SimEvent.Type> seen = EnumSet.noneOf(SimEvent.Type.class);
        events.forEach(e -> seen.add(e.type()));
        assertTrue(seen.contains(SimEvent.Type.EMERGENCY_INJECTED), "should emit EMERGENCY_INJECTED");
        assertTrue(seen.contains(SimEvent.Type.EMERGENCY_GRANTED), "should emit EMERGENCY_GRANTED");
        assertTrue(seen.contains(SimEvent.Type.NORMAL_SUSPENDED), "should emit NORMAL_SUSPENDED");
    }
}
