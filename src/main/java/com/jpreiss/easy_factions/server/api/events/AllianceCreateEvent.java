package com.jpreiss.easy_factions.server.api.events;

import net.minecraftforge.eventbus.api.Event;


public class AllianceCreateEvent extends Event {
    private final String allianceName;
    private final String factionName;

    public AllianceCreateEvent(String allianceName, String factionName) {
        this.allianceName = allianceName;
        this.factionName = factionName;
    }

    public String getAllianceName() {
        return allianceName;
    }

    /**
     * Returns the faction that created the alliance
     */
    public String getFactionName() {return factionName;}
}