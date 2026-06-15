package com.trafficsim.support;

import com.trafficsim.domain.ScenarioConfig;
import com.trafficsim.domain.Vehicle;
import com.trafficsim.domain.VehicleType;
import com.trafficsim.engine.SimulationController;
import com.trafficsim.scenario.ScenarioManager;
import com.trafficsim.scenario.VehicleTypeSelector;
import com.trafficsim.scheduling.SchedulingStrategy;

import java.util.List;

/**
 * Shared helpers for the scheduler tests: build small hand-checkable datasets and drive the real
 * engine to completion so the assertions exercise the same code path the UI uses.
 */
public final class EngineTestSupport {

    private EngineTestSupport() {
    }

    /** A single-lane config with explicit quantum/aging and a generous duration for the safety cap. */
    public static ScenarioConfig config(int quantum, int agingInterval) {
        return ScenarioConfig.builder()
                .laneCount(1)
                .durationTicks(50)
                .quantum(quantum)
                .agingInterval(agingInterval)
                .arrivalRate(0.0)   // datasets are supplied explicitly in tests
                .emergencyRate(0.0)
                .seed(1L)
                .build();
    }

    public static Vehicle normal(int id, int arrival, int burst, int priority) {
        return new Vehicle(id, 0, arrival, burst, priority, VehicleType.CAR);
    }

    public static Vehicle emergency(int id, int arrival, int burst) {
        return new Vehicle(id, 0, arrival, burst, VehicleType.EMERGENCY.defaultPriority(), VehicleType.EMERGENCY);
    }

    /** Loads the strategy + dataset into a real controller and steps until the run finishes. */
    public static SimulationController runToCompletion(SchedulingStrategy strategy,
                                                       List<Vehicle> dataset, ScenarioConfig config) {
        SimulationController controller = new SimulationController(new ScenarioManager(new VehicleTypeSelector()));
        controller.load(strategy, dataset, config);
        int guard = 0;
        while (!controller.isFinished() && guard++ < 100_000) {
            controller.step();
        }
        return controller;
    }

    public static Vehicle byId(List<Vehicle> dataset, int id) {
        return dataset.stream().filter(v -> v.id() == id).findFirst().orElseThrow();
    }
}
