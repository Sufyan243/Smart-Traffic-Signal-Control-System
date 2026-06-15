package com.trafficsim.scenario;

import com.trafficsim.domain.ScenarioConfig;
import com.trafficsim.domain.Vehicle;
import com.trafficsim.domain.VehicleType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Builds the reproducible vehicle dataset and replays it identically across every algorithm.
 *
 * <p>This is where determinism — the project's central non-functional requirement — is enforced.
 * Generation is driven entirely by {@link ScenarioConfig#seed()}: the same seed and config always
 * produce the same vehicles in the same order, so a side-by-side algorithm comparison measures the
 * algorithm and never a different random workload.</p>
 */
public final class ScenarioManager {

    /** Relative spawn weights for ordinary vehicle types (cars are the most common). */
    private static final int CAR_WEIGHT = 3;
    private static final int BUS_WEIGHT = 2;
    private static final int TRUCK_WEIGHT = 1;

    private final VehicleTypeSelector vehicleTypeSelector;
    private ScenarioConfig config;
    private List<Vehicle> dataset = List.of();

    public ScenarioManager(VehicleTypeSelector vehicleTypeSelector) {
        this.vehicleTypeSelector = vehicleTypeSelector;
        this.config = ScenarioConfig.defaults();
    }

    public ScenarioConfig config() {
        return config;
    }

    public List<Vehicle> dataset() {
        return dataset;
    }

    /**
     * (Re)generates the dataset for {@code config}. Returns the same vehicle instances each call for
     * a given config+selector, so callers can {@link Vehicle#resetRuntime() reset} and replay them.
     */
    public List<Vehicle> regenerate(ScenarioConfig config) {
        this.config = config;
        Random rng = new Random(config.seed());
        List<Vehicle> out = new ArrayList<>();
        int id = 1;

        List<VehicleType> normalTypes = vehicleTypeSelector.enabledNormalTypes();
        boolean emergencyEnabled = vehicleTypeSelector.emergencyEnabled();

        for (int tick = 0; tick < config.durationTicks(); tick++) {
            for (int lane = 0; lane < config.laneCount(); lane++) {
                if (rng.nextDouble() >= config.arrivalRate()) {
                    continue;
                }
                VehicleType type = pickType(rng, normalTypes, emergencyEnabled, config.emergencyRate());
                int burst = type.defaultBurstTime() + rng.nextInt(2); // small deterministic jitter
                out.add(new Vehicle(id++, lane, tick, burst, type.defaultPriority(), type));
            }
        }

        this.dataset = out;
        return out;
    }

    /** Resets every vehicle's runtime state so the dataset can be replayed by the next algorithm. */
    public void resetDatasetRuntime() {
        for (Vehicle v : dataset) {
            v.resetRuntime();
        }
    }

    /**
     * Creates an emergency vehicle for mid-run injection (FR-05). It is not part of the seeded
     * dataset, so injecting one does not perturb the deterministic comparison baseline. The caller
     * (the controller) supplies a unique id derived from the loaded dataset to avoid collisions.
     */
    public Vehicle createEmergencyVehicle(int id, int laneId, int arrivalTick) {
        VehicleType t = VehicleType.EMERGENCY;
        return new Vehicle(id, laneId, arrivalTick, t.defaultBurstTime(), t.defaultPriority(), t);
    }

    private VehicleType pickType(Random rng, List<VehicleType> normalTypes,
                                 boolean emergencyEnabled, double emergencyRate) {
        if (emergencyEnabled && rng.nextDouble() < emergencyRate) {
            return VehicleType.EMERGENCY;
        }
        if (normalTypes.isEmpty()) {
            return VehicleType.CAR;
        }
        int totalWeight = normalTypes.stream().mapToInt(ScenarioManager::weightOf).sum();
        int roll = rng.nextInt(totalWeight);
        for (VehicleType t : normalTypes) {
            roll -= weightOf(t);
            if (roll < 0) {
                return t;
            }
        }
        return normalTypes.get(0);
    }

    private static int weightOf(VehicleType type) {
        return switch (type) {
            case CAR -> CAR_WEIGHT;
            case BUS -> BUS_WEIGHT;
            case TRUCK -> TRUCK_WEIGHT;
            case EMERGENCY -> 0;
        };
    }
}
