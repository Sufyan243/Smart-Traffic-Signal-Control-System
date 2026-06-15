package com.trafficsim.engine;

import com.trafficsim.domain.Vehicle;
import com.trafficsim.scenario.ScenarioManager;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Creates emergency vehicles for mid-run injection and tracks which ones are currently active
 * (testable signal for FR-05).
 *
 * <p>An emergency vehicle carries priority 0, so the moment the {@code PriorityAgingStrategy} sees
 * it waiting it preempts whatever is crossing — that is the "promote to the highest-priority queue"
 * behaviour. This handler only manufactures and accounts for emergencies; the actual preemption and
 * signal switch happen in the controller and {@link SignalSwitcher}.</p>
 */
public final class EmergencyVehicleHandler {

    private final ScenarioManager scenarioManager;
    private final Set<Integer> activeEmergencyIds = new LinkedHashSet<>();

    public EmergencyVehicleHandler(ScenarioManager scenarioManager) {
        this.scenarioManager = scenarioManager;
    }

    /** Manufactures an emergency vehicle with id {@code id} arriving in {@code laneId} at {@code arrivalTick}. */
    public Vehicle create(int id, int laneId, int arrivalTick) {
        Vehicle v = scenarioManager.createEmergencyVehicle(id, laneId, arrivalTick);
        activeEmergencyIds.add(v.id());
        return v;
    }

    public void markCleared(Vehicle vehicle) {
        activeEmergencyIds.remove(vehicle.id());
    }

    public boolean hasActiveEmergency() {
        return !activeEmergencyIds.isEmpty();
    }

    public int activeCount() {
        return activeEmergencyIds.size();
    }

    public void reset() {
        activeEmergencyIds.clear();
    }
}
