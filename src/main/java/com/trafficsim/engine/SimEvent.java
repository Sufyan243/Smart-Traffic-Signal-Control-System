package com.trafficsim.engine;

/**
 * A discrete thing that happened during one simulation tick. The controller accumulates these and
 * the UI drains them each animation frame to feed the various "displayer" widgets — this keeps the
 * engine free of any JavaFX dependency.
 */
public record SimEvent(int tick, SimEvent.Type type, int vehicleId, int laneId, String message) {

    public enum Type {
        SIGNAL_CHANGE,
        VEHICLE_START,
        VEHICLE_COMPLETE,
        EMERGENCY_INJECTED,
        EMERGENCY_GRANTED,
        NORMAL_SUSPENDED,
        AGING_PROMOTION
    }

    public static SimEvent of(int tick, Type type, int vehicleId, int laneId, String message) {
        return new SimEvent(tick, type, vehicleId, laneId, message);
    }
}
