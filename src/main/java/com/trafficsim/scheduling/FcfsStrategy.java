package com.trafficsim.scheduling;

import com.trafficsim.domain.ScenarioConfig;
import com.trafficsim.domain.Vehicle;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * First-Come First-Served — the non-preemptive baseline / control algorithm.
 *
 * <p>Vehicles cross strictly in arrival order. Once a vehicle is granted the intersection it keeps
 * it until it has fully crossed; nothing (not even an emergency vehicle) interrupts it. This purity
 * is intentional: FCFS is the control against which the other algorithms are measured, so emergency
 * handling lives only in {@link PriorityAgingStrategy}.</p>
 */
public final class FcfsStrategy implements SchedulingStrategy {

    private final Deque<Vehicle> ready = new ArrayDeque<>();

    @Override
    public String name() {
        return "First-Come First-Served";
    }

    @Override
    public String shortName() {
        return "FCFS";
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
        ready.addLast(vehicle);
    }

    @Override
    public Vehicle selectRunning(Vehicle running, int quantumElapsed, int tick) {
        if (running != null) {
            return running;          // never preempt
        }
        return ready.pollFirst();    // earliest arrival (FIFO), or null if empty
    }

    @Override
    public void onComplete(Vehicle vehicle, int tick) {
        // no bookkeeping: the vehicle left the queue when it was first selected
    }
}
