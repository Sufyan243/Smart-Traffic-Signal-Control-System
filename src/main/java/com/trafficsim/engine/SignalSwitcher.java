package com.trafficsim.engine;

import com.trafficsim.domain.Lane;
import com.trafficsim.domain.SignalState;

import java.util.List;

/**
 * Owns the traffic-light state across all lanes (testable signal for FR-05). Exactly one lane — the
 * one whose vehicle currently holds the intersection — is {@link SignalState#GREEN}; every other
 * lane is {@link SignalState#RED}. When an emergency vehicle is granted the intersection this is what
 * "switches to provide a green path".
 */
public final class SignalSwitcher {

    private int greenLaneId = -1;

    /**
     * Drives the lane signals so that {@code greenLaneId} is GREEN and all others RED.
     *
     * @return true if the green light actually moved to a different lane
     */
    public boolean switchTo(List<Lane> lanes, int greenLaneId) {
        boolean changed = this.greenLaneId != greenLaneId;
        for (Lane lane : lanes) {
            lane.setSignal(lane.id() == greenLaneId ? SignalState.GREEN : SignalState.RED);
        }
        this.greenLaneId = greenLaneId;
        return changed;
    }

    public int greenLaneId() {
        return greenLaneId;
    }

    public void reset(List<Lane> lanes) {
        greenLaneId = -1;
        for (Lane lane : lanes) {
            lane.setSignal(SignalState.RED);
        }
    }
}
