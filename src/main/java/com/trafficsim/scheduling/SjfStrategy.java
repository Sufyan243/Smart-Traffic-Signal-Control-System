package com.trafficsim.scheduling;

import com.trafficsim.domain.ScenarioConfig;
import com.trafficsim.domain.Vehicle;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Shortest-Job-First (non-preemptive), where a vehicle's "job length" is its crossing time
 * (burst time).
 *
 * <p>Pedagogical note for the report: real traffic cannot actually reorder a physical queue of cars
 * by who crosses fastest. We model crossing time as burst time purely to demonstrate SJF's
 * behaviour and its known minimal-average-waiting-time property on a ready set — this is a
 * comparison device, not a claim about how signals should work.</p>
 */
public final class SjfStrategy implements SchedulingStrategy {

    private static final Comparator<Vehicle> SHORTEST_FIRST =
            Comparator.comparingInt(Vehicle::burstTime)
                    .thenComparingInt(Vehicle::arrivalTime)
                    .thenComparingInt(Vehicle::id);

    private final PriorityQueue<Vehicle> ready = new PriorityQueue<>(SHORTEST_FIRST);

    @Override
    public String name() {
        return "Shortest Job First";
    }

    @Override
    public String shortName() {
        return "SJF";
    }

    @Override
    public boolean isPreemptive() {
        return false;
    }

    @Override
    public void reset(ScenarioConfig config) {
        ready.clear();
    }

    @Override
    public void onArrive(Vehicle vehicle, int tick) {
        ready.add(vehicle);
    }

    @Override
    public Vehicle selectRunning(Vehicle running, int quantumElapsed, int tick) {
        if (running != null) {
            return running;       // non-preemptive: finish the current crossing first
        }
        return ready.poll();      // shortest remaining job among those that have arrived
    }

    @Override
    public void onComplete(Vehicle vehicle, int tick) {
        // no bookkeeping: removed from the heap when selected
    }
}
