package com.trafficsim.scenario;

import com.trafficsim.domain.VehicleType;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Lets the user choose which vehicle types appear in the generated traffic and which type is used
 * when a vehicle is injected by hand (testable signal for FR-01).
 *
 * <p>Disabling a type removes it from dataset generation; the {@link #selected()} type is the one
 * the control panel injects on demand. {@link VehicleType#CAR} can never be disabled so generation
 * always has at least one ordinary type to fall back on.</p>
 */
public final class VehicleTypeSelector {

    private final Set<VehicleType> enabled = EnumSet.allOf(VehicleType.class);
    private VehicleType selected = VehicleType.EMERGENCY;

    public void enable(VehicleType type) {
        enabled.add(type);
    }

    public void disable(VehicleType type) {
        if (type == VehicleType.CAR) {
            throw new IllegalArgumentException("CAR is the baseline type and cannot be disabled");
        }
        enabled.remove(type);
    }

    public void setEnabled(VehicleType type, boolean on) {
        if (on) {
            enable(type);
        } else {
            disable(type);
        }
    }

    public boolean isEnabled(VehicleType type) {
        return enabled.contains(type);
    }

    public Set<VehicleType> enabledTypes() {
        return Collections.unmodifiableSet(EnumSet.copyOf(enabled));
    }

    /** Enabled types that are not emergencies, used to weight ordinary-traffic generation. */
    public List<VehicleType> enabledNormalTypes() {
        return enabledTypes().stream().filter(t -> !t.isEmergency()).toList();
    }

    public boolean emergencyEnabled() {
        return enabled.contains(VehicleType.EMERGENCY);
    }

    public VehicleType selected() {
        return selected;
    }

    public void select(VehicleType type) {
        this.selected = type;
    }
}
