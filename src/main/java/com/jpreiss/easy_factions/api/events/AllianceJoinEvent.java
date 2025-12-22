package com.jpreiss.easy_factions.api.events;

import net.minecraftforge.eventbus.api.Event;


public class AllianceJoinEvent extends Event {
    private final String allianceName;
    private final String factionName;

    public AllianceJoinEvent(String allianceName, String factionName) {
        this.allianceName = allianceName;
        this.factionName = factionName;
    }

    public String getAllianceName() {
        return allianceName;
    }

    /**
     * Returns the faction that joined the alliance
     */
    public String getFactionName() {return factionName;}
}