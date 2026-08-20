package com.jpreiss.easy_factions.server.api.events;

import com.jpreiss.easy_factions.server.alliance.Alliance;
import net.neoforged.bus.api.Event;

public class AllianceDisbandEvent extends Event {
    private final Alliance alliance;

    public AllianceDisbandEvent(Alliance alliance) {
        this.alliance = alliance;
    }

    public Alliance getAlliance() {
        return alliance;
    }
}