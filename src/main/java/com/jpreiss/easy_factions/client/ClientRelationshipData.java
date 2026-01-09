package com.jpreiss.easy_factions.client;

import com.jpreiss.easy_factions.common.RelationshipStatus;

import java.util.HashMap;
import java.util.Map;

public class ClientRelationshipData {
    // faction, relationship with that faction
    private static final Map<String, RelationshipStatus> relationships = new HashMap<>();

    public static void setRelationships(Map<String, RelationshipStatus> relationships) {
        ClientRelationshipData.relationships.clear();
        ClientRelationshipData.relationships.putAll(relationships);
    }

    public static RelationshipStatus getRelationship(String factionName) {
        return relationships.getOrDefault(factionName, RelationshipStatus.NEUTRAL);
    }

}
