package com.trafficsim.scenario;

import com.trafficsim.domain.ScenarioConfig;

/**
 * Front door for FR-01 "Configure traffic scenario" (testable signal). It holds the editable working
 * copy of a {@link ScenarioConfig} and delegates the two sub-concerns to {@link VehicleTypeSelector}
 * (which vehicle types appear) and {@link SignalParameterAdjuster} (signal-timing knobs).
 *
 * <p>The control panel mutates one field at a time, then calls {@link #build()} to obtain a fresh
 * immutable config for the engine. Bounds are enforced by {@link ScenarioConfig.Builder}; invalid
 * input surfaces as an {@link IllegalArgumentException} the UI turns into an inline message.</p>
 */
public final class TrafficScenarioConfigurator {

    private final VehicleTypeSelector vehicleTypeSelector;
    private final SignalParameterAdjuster signalParameterAdjuster;

    private int laneCount;
    private int durationTicks;
    private double arrivalRate;
    private long seed;

    public TrafficScenarioConfigurator() {
        this(ScenarioConfig.defaults());
    }

    public TrafficScenarioConfigurator(ScenarioConfig seedConfig) {
        this.laneCount = seedConfig.laneCount();
        this.durationTicks = seedConfig.durationTicks();
        this.arrivalRate = seedConfig.arrivalRate();
        this.seed = seedConfig.seed();
        this.vehicleTypeSelector = new VehicleTypeSelector();
        this.signalParameterAdjuster = new SignalParameterAdjuster(seedConfig);
    }

    public VehicleTypeSelector vehicleTypeSelector() {
        return vehicleTypeSelector;
    }

    public SignalParameterAdjuster signalParameterAdjuster() {
        return signalParameterAdjuster;
    }

    public TrafficScenarioConfigurator setLaneCount(int laneCount) {
        this.laneCount = laneCount;
        return this;
    }

    public TrafficScenarioConfigurator setDurationTicks(int durationTicks) {
        this.durationTicks = durationTicks;
        return this;
    }

    public TrafficScenarioConfigurator setArrivalRate(double arrivalRate) {
        this.arrivalRate = arrivalRate;
        return this;
    }

    public TrafficScenarioConfigurator setSeed(long seed) {
        this.seed = seed;
        return this;
    }

    /** Builds an immutable, validated config from the current working values. */
    public ScenarioConfig build() {
        ScenarioConfig.Builder builder = ScenarioConfig.builder()
                .laneCount(laneCount)
                .durationTicks(durationTicks)
                .arrivalRate(arrivalRate)
                .seed(seed);
        return signalParameterAdjuster.applyTo(builder).build();
    }
}
