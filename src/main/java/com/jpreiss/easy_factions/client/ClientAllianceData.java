package com.jpreiss.easy_factions.client;

import java.util.HashMap;
import java.util.Map;

public class ClientAllianceData {
    private static final Map<String, String> factionToAlliance = new HashMap<>();
    // alliance, abbreviation
    private static final Map<String, String> allianceToAbbreviation = new HashMap<>();

    /**
     * Updates the data with the provided values
     */
    public static void update(Map<String, String> factionToAlliance, Map<String, String> allianceToAbbreviation) {
        ClientAllianceData.factionToAlliance.putAll(factionToAlliance);
        ClientAllianceData.allianceToAbbreviation.putAll(allianceToAbbreviation);
    }

    /**
     * Updates the data with the provided values
     */
    public static void update(String allianceName, String abbreviation) {
        allianceToAbbreviation.put(allianceName, abbreviation);
    }

    public static String getAlliance(String factionName) {
        return factionToAlliance.get(factionName);
    }

    public static String getAbbreviation(String factionName) {
        return allianceToAbbreviation.get(factionName);
    }

    /**
     * Removes alliance information of a faction. Cleans up abbreviations too
     */
    public static void removeFactionInformation(String factionName) {
        String previousAlliance = factionToAlliance.remove(factionName);
        if (previousAlliance != null && !factionToAlliance.containsValue(previousAlliance)) {
            allianceToAbbreviation.remove(previousAlliance);
        }
    }
}
