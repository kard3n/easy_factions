package com.jpreiss.easy_factions.server.claims.model;

public class ClaimData {
    public ClaimType type;
    public String owner; // Faction Name or Player UUID string
    public int color; // Cached color for minimap

    public ClaimData(ClaimType type, String owner, int color) {
        this.type = type;
        this.owner = owner;
        this.color = color;
    }
}
