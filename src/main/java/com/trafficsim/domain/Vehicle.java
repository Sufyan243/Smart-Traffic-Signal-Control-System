package com.trafficsim.domain;

/**
 * A vehicle modelled as an operating-system process.
 *
 * <ul>
 *   <li><b>arrivalTime</b> &rarr; the tick at which the process becomes ready.</li>
 *   <li><b>burstTime</b> &rarr; total ticks the vehicle needs to cross the intersection (CPU burst).</li>
 *   <li><b>priority</b> &rarr; base scheduling priority, lower number = higher priority.</li>
 * </ul>
 *
 * <p>The immutable identity/parameters are set at construction. The mutable runtime fields
 * ({@link #remainingTime}, {@link #state}, the three timestamps and the aging counters) are reset
 * by {@link #resetRuntime()} before each algorithm replays the same dataset, which is what keeps
 * cross-algorithm comparison deterministic.</p>
 */
public final class Vehicle {

    public static final int UNSET = -1;

    // --- immutable identity / parameters ---
    private final int id;
    private final int laneId;
    private final int arrivalTime;
    private final int burstTime;
    private final int basePriority;
    private final VehicleType type;

    // --- mutable runtime state (reset between algorithm runs) ---
    private int remainingTime;
    private VehicleState state;
    private int firstStartTime;     // response time anchor
    private int completionTime;     // turnaround time anchor
    private int waitingTicks;       // ticks spent WAITING (feeds the aging mechanism)
    private int effectivePriority;  // basePriority improved by aging (lower = higher)
    private boolean promotedByAging;

    public Vehicle(int id, int laneId, int arrivalTime, int burstTime, int basePriority, VehicleType type) {
        if (burstTime <= 0) {
            throw new IllegalArgumentException("burstTime must be positive, got " + burstTime);
        }
        this.id = id;
        this.laneId = laneId;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.basePriority = basePriority;
        this.type = type;
        resetRuntime();
    }

    /** Restores the process to its pre-run state so the next algorithm replays an identical dataset. */
    public void resetRuntime() {
        this.remainingTime = burstTime;
        this.state = VehicleState.WAITING;
        this.firstStartTime = UNSET;
        this.completionTime = UNSET;
        this.waitingTicks = 0;
        this.effectivePriority = basePriority;
        this.promotedByAging = false;
    }

    // --- identity / parameters ---
    public int id() { return id; }
    public int laneId() { return laneId; }
    public int arrivalTime() { return arrivalTime; }
    public int burstTime() { return burstTime; }
    public int basePriority() { return basePriority; }
    public VehicleType type() { return type; }
    public boolean isEmergency() { return type.isEmergency(); }

    // --- runtime state ---
    public int remainingTime() { return remainingTime; }
    public boolean isFinished() { return remainingTime <= 0; }
    public VehicleState state() { return state; }
    public void setState(VehicleState state) { this.state = state; }

    public int firstStartTime() { return firstStartTime; }
    public int completionTime() { return completionTime; }
    public int waitingTicks() { return waitingTicks; }
    public int effectivePriority() { return effectivePriority; }
    public boolean wasPromotedByAging() { return promotedByAging; }

    /** Marks the first tick this vehicle was granted the intersection (anchors response time). */
    public void markStarted(int tick) {
        if (firstStartTime == UNSET) {
            firstStartTime = tick;
        }
    }

    /** Consumes one tick of crossing time. Returns true once the vehicle has fully crossed. */
    public boolean advanceOneTick() {
        if (remainingTime > 0) {
            remainingTime--;
        }
        return isFinished();
    }

    public void markCompleted(int tick) {
        this.completionTime = tick;
        this.state = VehicleState.DONE;
    }

    public void incrementWait() {
        waitingTicks++;
    }

    /** Sets the aging-adjusted priority and records whether aging actually promoted the vehicle. */
    public void setEffectivePriority(int effectivePriority) {
        this.effectivePriority = effectivePriority;
        if (effectivePriority < basePriority) {
            promotedByAging = true;
        }
    }

    @Override
    public String toString() {
        return "V%d[lane=%d,arr=%d,burst=%d,prio=%d,%s]"
                .formatted(id, laneId, arrivalTime, burstTime, basePriority, type.label());
    }
}
