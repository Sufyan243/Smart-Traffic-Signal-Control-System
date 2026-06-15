package com.trafficsim.scheduling;

import com.trafficsim.domain.ScenarioConfig;
import com.trafficsim.domain.Vehicle;

/**
 * A pluggable scheduling algorithm. The simulation engine never changes when an algorithm is added
 * or removed — it only talks to this interface (open/closed principle), which is the
 * maintainability NFR for the project.
 *
 * <p><b>Contract with the engine.</b> Each strategy owns its own ready structure. The engine drives
 * it once per tick:</p>
 * <ol>
 *   <li>{@link #reset(ScenarioConfig)} once before a run.</li>
 *   <li>{@link #onArrive(Vehicle, int)} for every vehicle whose arrival tick has come.</li>
 *   <li>{@link #selectRunning(Vehicle, int, int)} once per tick to decide which vehicle holds the
 *       intersection during that tick. Returning a vehicle other than {@code running} preempts the
 *       current one; the strategy is responsible for re-admitting a preempted vehicle to its own
 *       structure before returning.</li>
 *   <li>{@link #onComplete(Vehicle, int)} when a vehicle finishes crossing.</li>
 * </ol>
 */
public interface SchedulingStrategy {

    /** Human-readable name, e.g. {@code "First-Come First-Served"}. */
    String name();

    /** Compact label for charts/CSV, e.g. {@code "FCFS"}. */
    String shortName();

    /** Whether this algorithm can take the intersection away from a vehicle that has not finished. */
    boolean isPreemptive();

    /** Clears all internal state so the same dataset can be replayed from scratch. */
    void reset(ScenarioConfig config);

    /** Notifies the strategy that {@code vehicle} has arrived and is now ready at tick {@code tick}. */
    void onArrive(Vehicle vehicle, int tick);

    /**
     * Decides which vehicle should hold the intersection during {@code tick}.
     *
     * @param running        the vehicle that ran the previous tick (never finished — finished
     *                       vehicles are reported via {@link #onComplete} and passed here as null)
     * @param quantumElapsed number of consecutive ticks {@code running} has already crossed
     * @param tick           the current simulation tick
     * @return the vehicle to run this tick, or {@code null} if none are ready
     */
    Vehicle selectRunning(Vehicle running, int quantumElapsed, int tick);

    /** Notifies the strategy that {@code vehicle} has finished crossing at {@code tick}. */
    void onComplete(Vehicle vehicle, int tick);
}
