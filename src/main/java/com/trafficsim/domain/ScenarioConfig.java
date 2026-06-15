package com.trafficsim.domain;

/**
 * Immutable bundle of all parameters that define a reproducible simulation scenario.
 *
 * <p>The {@code seed} is the linchpin of determinism: the same seed always produces the same vehicle
 * dataset, which every scheduling algorithm then replays identically so the comparison measures the
 * algorithm and not a different random workload.</p>
 *
 * <p>Instances are built through {@link Builder} so the UI's configurator can tweak one field at a
 * time while keeping the resulting config immutable.</p>
 */
public final class ScenarioConfig {

    private final int laneCount;
    private final int durationTicks;     // length of the arrival window
    private final double arrivalRate;    // expected new vehicles per lane per tick
    private final int quantum;           // Round Robin time quantum (ticks)
    private final int agingInterval;     // ticks of waiting before effective priority improves by 1
    private final double emergencyRate;  // probability a generated vehicle is an emergency
    private final long seed;

    private ScenarioConfig(Builder b) {
        this.laneCount = b.laneCount;
        this.durationTicks = b.durationTicks;
        this.arrivalRate = b.arrivalRate;
        this.quantum = b.quantum;
        this.agingInterval = b.agingInterval;
        this.emergencyRate = b.emergencyRate;
        this.seed = b.seed;
    }

    public int laneCount() { return laneCount; }
    public int durationTicks() { return durationTicks; }
    public double arrivalRate() { return arrivalRate; }
    public int quantum() { return quantum; }
    public int agingInterval() { return agingInterval; }
    public double emergencyRate() { return emergencyRate; }
    public long seed() { return seed; }

    public Builder toBuilder() {
        return new Builder()
                .laneCount(laneCount)
                .durationTicks(durationTicks)
                .arrivalRate(arrivalRate)
                .quantum(quantum)
                .agingInterval(agingInterval)
                .emergencyRate(emergencyRate)
                .seed(seed);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Sensible defaults for a 4-lane, ~50-vehicle demo. */
    public static ScenarioConfig defaults() {
        return builder().build();
    }

    public static final class Builder {
        private int laneCount = 4;
        private int durationTicks = 60;
        private double arrivalRate = 0.18;
        private int quantum = 3;
        private int agingInterval = 5;
        private double emergencyRate = 0.06;
        private long seed = 42L;

        public Builder laneCount(int v) { this.laneCount = require(v, 1, 8, "laneCount"); return this; }
        public Builder durationTicks(int v) { this.durationTicks = require(v, 1, 100_000, "durationTicks"); return this; }
        public Builder arrivalRate(double v) {
            if (v < 0 || v > 1) throw new IllegalArgumentException("arrivalRate must be in [0,1], got " + v);
            this.arrivalRate = v; return this;
        }
        public Builder quantum(int v) { this.quantum = require(v, 1, 1000, "quantum"); return this; }
        public Builder agingInterval(int v) { this.agingInterval = require(v, 1, 100_000, "agingInterval"); return this; }
        public Builder emergencyRate(double v) {
            if (v < 0 || v > 1) throw new IllegalArgumentException("emergencyRate must be in [0,1], got " + v);
            this.emergencyRate = v; return this;
        }
        public Builder seed(long v) { this.seed = v; return this; }

        public ScenarioConfig build() {
            return new ScenarioConfig(this);
        }

        private static int require(int v, int min, int max, String name) {
            if (v < min || v > max) {
                throw new IllegalArgumentException(name + " must be in [" + min + "," + max + "], got " + v);
            }
            return v;
        }
    }
}
