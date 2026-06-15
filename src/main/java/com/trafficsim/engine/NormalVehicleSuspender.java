package com.trafficsim.engine;

import com.trafficsim.domain.Vehicle;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Tracks which ordinary vehicles have been temporarily suspended because an emergency vehicle
 * preempted them (testable signal for FR-05). The set is purely informational — it lets the UI grey
 * out suspended vehicles — while their actual rescheduling is handled by the priority strategy.
 */
public final class NormalVehicleSuspender {

    private final Set<Integer> suspendedVehicleIds = new LinkedHashSet<>();

    /** Records that an ordinary vehicle was suspended (emergencies are never suspended). */
    public void suspend(Vehicle vehicle) {
        if (!vehicle.isEmergency()) {
            suspendedVehicleIds.add(vehicle.id());
        }
    }

    public boolean isSuspended(int vehicleId) {
        return suspendedVehicleIds.contains(vehicleId);
    }

    public int suspendedCount() {
        return suspendedVehicleIds.size();
    }

    /** Clears all suspensions, e.g. once the emergency vehicle has cleared the intersection. */
    public void resumeAll() {
        suspendedVehicleIds.clear();
    }

    public void reset() {
        suspendedVehicleIds.clear();
    }
}
