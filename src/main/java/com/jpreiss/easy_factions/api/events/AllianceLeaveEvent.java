package com.jpreiss.easy_factions.api.events;

import net.minecraftforge.eventbus.api.Event;


public class AllianceLeaveEvent extends Event {
    private final String allianceName;
    private final String factionName;

    public AllianceLeaveEvent(String allianceName, String factionName) {
        this.allianceName = allianceName;
        this.factionName = factionName;
    }

    public String getAllianceName() {
        return allianceName;
    }

    /**
     * Returns the faction that left the alliance
     */
    public String getFactionName() {return factionName;}
}