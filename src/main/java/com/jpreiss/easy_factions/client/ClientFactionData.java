package com.jpreiss.easy_factions.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientFactionData {
    private static Map<UUID, String> playerFactions = new HashMap<>();
    private static Map<String, String> factionAlliances = new HashMap<>();

    public static void update(Map<UUID, String> newPlayerFactions, Map<String, String> newFactionAlliances) {
        playerFactions = newPlayerFactions;
        factionAlliances = newFactionAlliances;
    }

    public static String getFaction(UUID playerUUID) {
        return playerFactions.get(playerUUID);
    }

    public static String getAlliance(String factionName) {
        return factionAlliances.get(factionName);
    }
}
