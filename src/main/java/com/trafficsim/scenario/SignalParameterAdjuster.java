package com.trafficsim.scenario;

import com.trafficsim.domain.ScenarioConfig;

/**
 * Adjusts the signal-timing parameters of a scenario (testable signal for FR-01).
 *
 * <p>In CPU-scheduling terms these are the algorithm knobs that change how long the "green light" is
 * granted and how aggressively starving vehicles are promoted:</p>
 * <ul>
 *   <li><b>quantum</b> — Round Robin green-light slice, in ticks.</li>
 *   <li><b>agingInterval</b> — ticks of waiting before a vehicle's priority improves by one.</li>
 *   <li><b>emergencyRate</b> — probability that a generated vehicle is an emergency.</li>
 * </ul>
 */
public final class SignalParameterAdjuster {

    private int quantum;
    private int agingInterval;
    private double emergencyRate;

    public SignalParameterAdjuster(ScenarioConfig seed) {
        this.quantum = seed.quantum();
        this.agingInterval = seed.agingInterval();
        this.emergencyRate = seed.emergencyRate();
    }

    public SignalParameterAdjuster() {
        this(ScenarioConfig.defaults());
    }

    public int quantum() { return quantum; }
    public int agingInterval() { return agingInterval; }
    public double emergencyRate() { return emergencyRate; }

    public void setQuantum(int quantum) {
        this.quantum = quantum;
    }

    public void setAgingInterval(int agingInterval) {
        this.agingInterval = agingInterval;
    }

    public void setEmergencyRate(double emergencyRate) {
        this.emergencyRate = emergencyRate;
    }

    /** Writes the current signal parameters onto a scenario builder. Builder validation enforces bounds. */
    public ScenarioConfig.Builder applyTo(ScenarioConfig.Builder builder) {
        return builder.quantum(quantum)
                .agingInterval(agingInterval)
                .emergencyRate(emergencyRate);
    }
}
