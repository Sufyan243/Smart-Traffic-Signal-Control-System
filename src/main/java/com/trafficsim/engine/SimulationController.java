package com.trafficsim.engine;

import com.trafficsim.domain.Intersection;
import com.trafficsim.domain.Lane;
import com.trafficsim.domain.ScenarioConfig;
import com.trafficsim.domain.SimClock;
import com.trafficsim.domain.Vehicle;
import com.trafficsim.domain.VehicleState;
import com.trafficsim.metrics.MetricsCollector;
import com.trafficsim.scenario.ScenarioManager;
import com.trafficsim.scheduling.SchedulingStrategy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The single-threaded, tick-based discrete-event engine — the heart of the simulator.
 *
 * <p>Each call to {@link #step()} advances the virtual clock by exactly one tick and is fully
 * deterministic: the same dataset and algorithm always produce the same sequence of events. The UI
 * drives this one tick per animation frame for a live run; {@link SimulationRunner} drives it in a
 * tight loop for headless comparison. Both share this exact logic, so what you see animated is what
 * the metrics measured.</p>
 *
 * <p>Per tick the engine: admits arrivals, asks the {@link SchedulingStrategy} which vehicle holds
 * the intersection, applies preemption/suspension, switches signals, services one tick of crossing,
 * and ages waiting vehicles.</p>
 */
public final class SimulationController {

    private final SimClock clock = new SimClock();
    private final Intersection intersection = new Intersection();
    private final MetricsCollector metricsCollector = new MetricsCollector();
    private final SignalSwitcher signalSwitcher = new SignalSwitcher();
    private final NormalVehicleSuspender normalVehicleSuspender = new NormalVehicleSuspender();
    private final EmergencyVehicleHandler emergencyVehicleHandler;
    private final List<SimEvent> pendingEvents = new ArrayList<>();

    private List<Lane> lanes = List.of();
    private SchedulingStrategy strategy;
    private ScenarioConfig config;

    private List<Vehicle> seeded = new ArrayList<>();   // arrival-sorted seeded dataset
    private final List<Vehicle> injected = new ArrayList<>(); // emergencies added at runtime
    private int arrivalIndex;

    private Vehicle running;
    private int quantumElapsed;
    private boolean finished;
    private int safetyLimit;

    private final java.util.Set<Integer> agingFlagged = new java.util.HashSet<>();
    private int agingPromotionCount;
    private int nextInjectionId = 1;

    public SimulationController(ScenarioManager scenarioManager) {
        this.emergencyVehicleHandler = new EmergencyVehicleHandler(scenarioManager);
    }

    /** Loads an algorithm + dataset and resets all state so a fresh run can begin. */
    public void load(SchedulingStrategy strategy, List<Vehicle> dataset, ScenarioConfig config) {
        this.strategy = strategy;
        this.config = config;

        this.seeded = new ArrayList<>(dataset);
        this.seeded.sort(Comparator.comparingInt(Vehicle::arrivalTime).thenComparingInt(Vehicle::id));
        for (Vehicle v : seeded) {
            v.resetRuntime();
        }
        this.injected.clear();

        this.lanes = Lane.createLanes(config.laneCount());
        this.strategy.reset(config);
        this.intersection.reset();
        this.clock.reset();
        this.metricsCollector.reset();
        this.normalVehicleSuspender.reset();
        this.emergencyVehicleHandler.reset();
        this.signalSwitcher.reset(lanes);
        this.pendingEvents.clear();

        this.arrivalIndex = 0;
        this.running = null;
        this.quantumElapsed = 0;
        this.finished = false;
        this.agingFlagged.clear();
        this.agingPromotionCount = 0;
        this.nextInjectionId = seeded.stream().mapToInt(Vehicle::id).max().orElse(0) + 1;

        int burstSum = seeded.stream().mapToInt(Vehicle::burstTime).sum();
        this.safetyLimit = config.durationTicks() + burstSum + 100;
    }

    /** Advances the simulation by exactly one tick. No-op once {@link #isFinished()}. */
    public void step() {
        if (finished || strategy == null) {
            return;
        }
        int t = clock.currentTick();

        admitArrivals(t);

        Vehicle previous = running;
        Vehicle selected = strategy.selectRunning(running, quantumElapsed, t);
        if (selected != previous) {
            handleSwitch(previous, selected, t);
        }

        detectAgingPromotions(t);

        if (signalSwitcher.switchTo(lanes, running == null ? -1 : running.laneId())) {
            int green = signalSwitcher.greenLaneId();
            emit(t, SimEvent.Type.SIGNAL_CHANGE, running == null ? -1 : running.id(), green,
                    green < 0 ? "All lanes red (idle)" : "Green light → " + laneName(green));
        }

        metricsCollector.onTick(t, running);
        serviceOneTick(t);
        ageWaitingVehicles();

        clock.advance();
        evaluateFinished();
    }

    private void admitArrivals(int t) {
        while (arrivalIndex < seeded.size() && seeded.get(arrivalIndex).arrivalTime() == t) {
            Vehicle v = seeded.get(arrivalIndex++);
            v.setState(VehicleState.WAITING);
            laneOf(v).enqueue(v);
            strategy.onArrive(v, t);
        }
    }

    private void handleSwitch(Vehicle previous, Vehicle selected, int t) {
        if (previous != null && !previous.isFinished()) {
            // preempted: back to its lane queue and (for emergencies) flag the suspension
            previous.setState(VehicleState.WAITING);
            laneOf(previous).enqueue(previous);
            if (selected != null && selected.isEmergency() && !previous.isEmergency()) {
                normalVehicleSuspender.suspend(previous);
                emit(t, SimEvent.Type.NORMAL_SUSPENDED, previous.id(), previous.laneId(),
                        "Suspended V" + previous.id() + " for emergency");
            }
        }
        if (previous != null) {
            intersection.release();
        }

        running = selected;
        quantumElapsed = 0;

        if (running != null) {
            running.markStarted(t);
            running.setState(VehicleState.CROSSING);
            laneOf(running).remove(running);
            intersection.acquire(running);

            emit(t, SimEvent.Type.VEHICLE_START, running.id(), running.laneId(),
                    "V" + running.id() + " crossing (" + running.type().label() + ")");
            if (running.isEmergency()) {
                emit(t, SimEvent.Type.EMERGENCY_GRANTED, running.id(), running.laneId(),
                        "Emergency V" + running.id() + " granted green path");
            }
        }
    }

    /**
     * Emits an {@link SimEvent.Type#AGING_PROMOTION} the moment a waiting (or running) vehicle's
     * effective priority is first improved past its base by aging, so the UI can flag starvation
     * prevention as it fires rather than only when the vehicle eventually crosses. Each vehicle is
     * flagged at most once per run.
     */
    private void detectAgingPromotions(int t) {
        checkPromotion(running, t);
        for (Lane lane : lanes) {
            for (Vehicle v : lane.snapshot()) {
                checkPromotion(v, t);
            }
        }
    }

    private void checkPromotion(Vehicle v, int t) {
        if (v != null && v.wasPromotedByAging() && agingFlagged.add(v.id())) {
            agingPromotionCount++;
            emit(t, SimEvent.Type.AGING_PROMOTION, v.id(), v.laneId(),
                    "Aging promoted V" + v.id() + " (anti-starvation)");
        }
    }

    private void serviceOneTick(int t) {
        if (running == null) {
            return;
        }
        boolean done = running.advanceOneTick();
        quantumElapsed++;
        if (done) {
            running.markCompleted(t + 1);
            strategy.onComplete(running, t);
            intersection.release();
            emit(t, SimEvent.Type.VEHICLE_COMPLETE, running.id(), running.laneId(),
                    "V" + running.id() + " cleared the intersection");
            if (running.isEmergency()) {
                emergencyVehicleHandler.markCleared(running);
                if (!emergencyVehicleHandler.hasActiveEmergency()) {
                    normalVehicleSuspender.resumeAll();
                }
            }
            running = null;
            quantumElapsed = 0;
        }
    }

    /** Every vehicle still waiting in a lane queue has waited one more tick (feeds aging). */
    private void ageWaitingVehicles() {
        for (Lane lane : lanes) {
            for (Vehicle v : lane.snapshot()) {
                v.incrementWait();
            }
        }
    }

    private void evaluateFinished() {
        boolean allArrived = arrivalIndex >= seeded.size();
        boolean queuesEmpty = lanes.stream().allMatch(Lane::isEmpty);
        boolean overrun = clock.currentTick() > safetyLimit;
        if ((allArrived && running == null && queuesEmpty) || overrun) {
            finished = true;
            metricsCollector.finish(clock.currentTick());
        }
    }

    /**
     * Injects an emergency vehicle into {@code laneId} at the current tick (FR-05). With the priority
     * algorithm active it will preempt the current vehicle on the next tick; with the others it
     * simply joins the queue (which is itself a useful contrast to show at the defense).
     */
    public Vehicle injectEmergency(int laneId) {
        if (finished || strategy == null) {
            return null;
        }
        int t = clock.currentTick();
        Vehicle e = emergencyVehicleHandler.create(nextInjectionId++, laneId, t);
        injected.add(e);
        e.setState(VehicleState.WAITING);
        laneOf(e).enqueue(e);
        strategy.onArrive(e, t);
        emit(t, SimEvent.Type.EMERGENCY_INJECTED, e.id(), laneId,
                "Emergency V" + e.id() + " injected in " + laneName(laneId));
        return e;
    }

    /** Returns and clears the events accumulated since the last drain (for the UI). */
    public List<SimEvent> drainEvents() {
        List<SimEvent> drained = new ArrayList<>(pendingEvents);
        pendingEvents.clear();
        return drained;
    }

    /** Every vehicle in the run (seeded + injected), used for final metric calculation. */
    public List<Vehicle> allVehicles() {
        List<Vehicle> all = new ArrayList<>(seeded);
        all.addAll(injected);
        return all;
    }

    private void emit(int tick, SimEvent.Type type, int vehicleId, int laneId, String message) {
        pendingEvents.add(SimEvent.of(tick, type, vehicleId, laneId, message));
    }

    private Lane laneOf(Vehicle v) {
        return lanes.get(v.laneId());
    }

    private String laneName(int laneId) {
        return laneId >= 0 && laneId < lanes.size() ? lanes.get(laneId).name() : "Lane " + laneId;
    }

    // --- accessors for the UI / runner ---
    public List<Lane> lanes() { return lanes; }
    public Intersection intersection() { return intersection; }
    public SimClock clock() { return clock; }
    public Vehicle running() { return running; }
    public SchedulingStrategy strategy() { return strategy; }
    public ScenarioConfig config() { return config; }
    public MetricsCollector metricsCollector() { return metricsCollector; }
    public NormalVehicleSuspender normalVehicleSuspender() { return normalVehicleSuspender; }
    public EmergencyVehicleHandler emergencyVehicleHandler() { return emergencyVehicleHandler; }
    public int agingPromotionCount() { return agingPromotionCount; }
    public boolean isFinished() { return finished; }

    public int completedCount() {
        return (int) allVehicles().stream().filter(v -> v.completionTime() != Vehicle.UNSET).count();
    }

    public int totalVehicles() {
        return seeded.size() + injected.size();
    }
}
