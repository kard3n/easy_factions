package com.jpreiss.easy_factions.server;

import com.jpreiss.easy_factions.common.RelationshipStatus;
import com.jpreiss.easy_factions.server.alliance.Alliance;
import com.jpreiss.easy_factions.server.alliance.AllianceStateManager;
import com.jpreiss.easy_factions.server.faction.Faction;
import com.jpreiss.easy_factions.server.faction.FactionStateManager;
import net.minecraft.server.MinecraftServer;

import java.util.HashMap;
import java.util.Map;

public class RelationshipCalculator {
    private static final Map<RelationshipStatus, Integer> STATUS_PRIORITIES = Map.of(
            RelationshipStatus.FRIENDLY, 0,
            RelationshipStatus.NEUTRAL, 1,
            RelationshipStatus.HOSTILE, 2
    );

    /**
     * Calculates the relationships of a faction with other factions.
     */
    public static HashMap<String, RelationshipStatus> calculateRelationships(Faction faction, MinecraftServer server) {
        HashMap<String, RelationshipStatus> relationships = new HashMap<>();
        FactionStateManager factionStateManager = FactionStateManager.get(server);
        AllianceStateManager allianceStateManager = AllianceStateManager.get(server);
        Alliance alliance = allianceStateManager.getAllianceByFaction(faction.getName());

        Faction otherFaction;
        Alliance otherAlliance;

        for (String otherFactionName : factionStateManager.getAllFactionNames()) {
            if (otherFactionName.equals(faction.getName())) continue;
            if (alliance != null && alliance.getMembers().contains(otherFactionName)) {
                relationships.put(otherFactionName, RelationshipStatus.FRIENDLY);
                continue;
            }

            RelationshipStatus maxRelation = RelationshipStatus.FRIENDLY;
            otherFaction = factionStateManager.getFactionByName(otherFactionName);
            otherAlliance = allianceStateManager.getAllianceByFaction(otherFactionName);

            // Check Alliance-to-Alliance Relations
            if (alliance != null && otherAlliance != null) {
                RelationshipStatus incoming = alliance.getOutgoingRelations().getOrDefault(otherAlliance.getName(), RelationshipStatus.NEUTRAL);
                RelationshipStatus outgoing = alliance.getIncomingRelations().getOrDefault(otherAlliance.getName(), RelationshipStatus.NEUTRAL);

                maxRelation = getHigherRelation(maxRelation, getHigherRelation(incoming, outgoing));
            }

            // Check Faction-to-Faction Relations
            if (alliance != null) {
                for (String member : alliance.getMembers()) {
                    Faction memberFaction = factionStateManager.getFactionByName(member);

                    RelationshipStatus incoming = memberFaction.getOutgoingRelations().getOrDefault(otherFactionName, RelationshipStatus.NEUTRAL);
                    RelationshipStatus outgoing = memberFaction.getIncomingRelations().getOrDefault(otherFactionName, RelationshipStatus.NEUTRAL);

                    maxRelation = getHigherRelation(maxRelation, getHigherRelation(incoming, outgoing));

                    if (maxRelation == RelationshipStatus.HOSTILE) break;
                }
            } else {
                // Solo Faction Logic
                RelationshipStatus incoming = faction.getOutgoingRelations().getOrDefault(otherFactionName, RelationshipStatus.NEUTRAL);
                RelationshipStatus outgoing = faction.getIncomingRelations().getOrDefault(otherFactionName, RelationshipStatus.NEUTRAL);
                maxRelation = getHigherRelation(maxRelation, getHigherRelation(incoming, outgoing));
            }

            if (maxRelation != RelationshipStatus.NEUTRAL) {
                relationships.put(otherFactionName, maxRelation);
            }
        }
        return relationships;
    }

    private static RelationshipStatus getHigherRelation(RelationshipStatus status1, RelationshipStatus status2) {
        return STATUS_PRIORITIES.get(status1) > STATUS_PRIORITIES.get(status2) ? status1 : status2;
    }
}
