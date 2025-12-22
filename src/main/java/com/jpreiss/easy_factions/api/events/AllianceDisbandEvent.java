package com.jpreiss.easy_factions.api.events;

import net.minecraftforge.eventbus.api.Event;

public class AllianceDisbandEvent extends Event {
    private final String allianceName;

    public AllianceDisbandEvent(String allianceName) {
        this.allianceName = allianceName;
    }

    public String getAllianceName() {
        return allianceName;
    }
}