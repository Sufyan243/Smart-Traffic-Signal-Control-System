package com.trafficsim.scheduling;

import com.trafficsim.domain.ScenarioConfig;
import com.trafficsim.domain.Vehicle;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Round Robin with a fixed time quantum — the most physically realistic mapping in this project,
 * since real signals genuinely cycle right-of-way on fixed timers.
 *
 * <p>Each vehicle gets the intersection for at most {@code quantum} consecutive ticks; if it has not
 * finished crossing it goes to the back of the queue and the next vehicle gets a turn. A lone
 * vehicle with no one else waiting simply keeps crossing — Round Robin only rotates when there is
 * somebody to rotate to.</p>
 */
public final class RoundRobinStrategy implements SchedulingStrategy {

    private final Deque<Vehicle> ready = new ArrayDeque<>();
    private int quantum = 3;

    @Override
    public String name() {
        return "Round Robin (q=" + quantum + ")";
    }

    @Override
    public String shortName() {
        return "RR";
    }

    @Override
    public boolean isPreemptive() {
        return true;
    }

    @Override
    public void reset(ScenarioConfig config) {
        ready.clear();
        this.quantum = config.quantum();
    }

    @Override
    public void onArrive(Vehicle vehicle, int tick) {
        ready.addLast(vehicle);
    }

    @Override
    public Vehicle selectRunning(Vehicle running, int quantumElapsed, int tick) {
        if (running == null) {
            return ready.pollFirst();          // start the next vehicle in line
        }
        if (quantumElapsed < quantum) {
            return running;                    // still inside its time slice
        }
        if (ready.isEmpty()) {
            return running;                    // quantum used up but nobody else is waiting
        }
        ready.addLast(running);                // time slice expired: rotate to the back
        return ready.pollFirst();
    }

    @Override
    public void onComplete(Vehicle vehicle, int tick) {
        // finished vehicle was already removed from the queue when it became running
    }

    public int quantum() {
        return quantum;
    }
}
