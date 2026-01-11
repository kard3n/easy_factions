package com.jpreiss.easy_factions.client.data_store;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientFactionData {
    private static final Map<UUID, String> playerFactions = new HashMap<>();
    // Faction, alliance
    private static final Map<String, String> factionAbbreviations = new HashMap<>();

    /**
     * Updates the data with the provided values
     */
    public static void update(Map<UUID, String> newPlayerFactions, Map<String, String> factionAbbreviations) {
        playerFactions.putAll(newPlayerFactions);
        ClientFactionData.factionAbbreviations.putAll(factionAbbreviations);
    }

    /**
     * Updates the data with the provided values
     */
    public static void update(String factionName, String abbreviation) {
        factionAbbreviations.put(factionName, abbreviation);
    }


    /**
     * Removes all information for the player. Abbreviations are only kept if another member is online
     * @param playerUUID UUID of the player
     * @return The faction the player was in
     */
    public static String removePlayer(UUID playerUUID) {
        String faction = playerFactions.remove(playerUUID);

        if (faction != null && !playerFactions.containsValue(faction)) {
            factionAbbreviations.remove(faction);
        }
        return faction;
    }

    /**
     * Removes all information for the faction
     */
    public static void removeFaction(String factionName) {
        factionAbbreviations.remove(factionName);
        for(Map.Entry<UUID, String> entry : playerFactions.entrySet()){
            if(entry.getValue().equals(factionName)){
                playerFactions.remove(entry.getKey());
            }
        }
    }

    /**
     * Returns how many members of the faction are online
     */
    public static long getFactionMemberCount(String factionName){
        return playerFactions.values().stream().filter(faction -> faction.equals(factionName)).count();
    }

    public static String getFaction(UUID playerUUID) {
        return playerFactions.get(playerUUID);
    }

    public static String getAbbreviation(String factionName) {
        return factionAbbreviations.get(factionName);
    }
}
