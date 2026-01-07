package com.jpreiss.easy_factions.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientFactionData {
    private static Map<UUID, String> playerFactions = new HashMap<>();
    // Faction, alliance
    private static Map<String, String> factionAlliances = new HashMap<>();

    /**
     * Updates the data with the provided values
     */
    public static void update(Map<UUID, String> newPlayerFactions, Map<String, String> newFactionAlliances) {
        playerFactions.putAll(newPlayerFactions);
        factionAlliances.putAll(newFactionAlliances);
    }

    /**
     * Removes all information for the player. Alliance is only kept if another member is online
     * @param playerUUID
     */
    public static void removePlayer(UUID playerUUID) {
        String faction = playerFactions.remove(playerUUID);

        if (faction != null && !playerFactions.containsValue(faction)) {
            factionAlliances.remove(faction);
        }
    }

    /**
     * Removes all information for the faction
     */
    public static void removeFaction(String factionName) {
        factionAlliances.remove(factionName);
        for(Map.Entry<UUID, String> entry : playerFactions.entrySet()){
            if(entry.getValue().equals(factionName)){
                playerFactions.remove(entry.getKey());
            }
        }
    }

    /**
     * Remove the faction from its alliance
     */
    public static void leaveAlliance(String factionName) {
        factionAlliances.remove(factionName);
    }


    public static String getFaction(UUID playerUUID) {
        return playerFactions.get(playerUUID);
    }

    public static String getAlliance(String factionName) {
        return factionAlliances.get(factionName);
    }
}
