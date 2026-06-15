package com.trafficsim.scheduling;

import com.trafficsim.domain.ScenarioConfig;
import com.trafficsim.domain.Vehicle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Preemptive Priority Scheduling with aging — the project's strongest, clearest demo and the only
 * algorithm that handles emergency vehicles.
 *
 * <p><b>Priority convention:</b> lower number = higher priority. Emergency vehicles carry priority
 * {@code 0}; ordinary vehicles start at their type's base priority (&ge; 1).</p>
 *
 * <p><b>Preemption:</b> if a waiting vehicle outranks the one currently crossing, it takes the
 * intersection immediately. That is exactly what lets an emergency vehicle injected mid-run suspend
 * a normal vehicle and get a green path.</p>
 *
 * <p><b>Aging (starvation prevention):</b> every {@code agingInterval} ticks a vehicle spends
 * waiting, its effective priority improves by one (never crossing below an emergency's 0 — ordinary
 * vehicles are floored at 1). A vehicle whose effective priority has improved past its base is
 * flagged via {@link Vehicle#wasPromotedByAging()} so the UI can highlight the moment starvation
 * prevention fires.</p>
 */
public final class PriorityAgingStrategy implements SchedulingStrategy {

    private static final Comparator<Vehicle> BY_EFFECTIVE_PRIORITY =
            Comparator.comparingInt(Vehicle::effectivePriority)
                    .thenComparingInt(Vehicle::arrivalTime)
                    .thenComparingInt(Vehicle::id);

    private final List<Vehicle> waiting = new ArrayList<>();
    private int agingInterval = 5;

    @Override
    public String name() {
        return "Priority + Aging";
    }

    @Override
    public String shortName() {
        return "PRIO";
    }

    @Override
    public boolean isPreemptive() {
        return true;
    }

    @Override
    public void reset(ScenarioConfig config) {
        waiting.clear();
        this.agingInterval = config.agingInterval();
    }

    @Override
    public void onArrive(Vehicle vehicle, int tick) {
        waiting.add(vehicle);
    }

    @Override
    public Vehicle selectRunning(Vehicle running, int quantumElapsed, int tick) {
        applyAging();

        Vehicle best = bestWaiting();

        if (running == null) {
            if (best != null) {
                waiting.remove(best);
            }
            return best;
        }

        // running has not finished — preempt only if a waiting vehicle strictly outranks it
        if (best != null && best.effectivePriority() < running.effectivePriority()) {
            waiting.remove(best);
            waiting.add(running);   // demoted back to the waiting set
            return best;
        }
        return running;
    }

    @Override
    public void onComplete(Vehicle vehicle, int tick) {
        // finished vehicle was removed from the waiting set when it became running
    }

    /** Improves the effective priority of long-waiting vehicles (starvation prevention). */
    private void applyAging() {
        for (Vehicle v : waiting) {
            int promotions = v.waitingTicks() / agingInterval;
            int floor = v.isEmergency() ? 0 : 1;
            int effective = Math.max(floor, v.basePriority() - promotions);
            v.setEffectivePriority(effective);
        }
    }

    private Vehicle bestWaiting() {
        Vehicle best = null;
        for (Vehicle v : waiting) {
            if (best == null || BY_EFFECTIVE_PRIORITY.compare(v, best) < 0) {
                best = v;
            }
        }
        return best;
    }
}
