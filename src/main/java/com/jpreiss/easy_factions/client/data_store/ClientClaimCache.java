package com.jpreiss.easy_factions.client.data_store;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientClaimCache {
    // Dim -> (Chunk, Color)
    private static final Map<ResourceLocation, Map<Long, Integer>> claims = new HashMap<>();

    public static void addClaims(Map<ResourceLocation, HashMap<Long, Integer>> newClaims) {
        newClaims.forEach((dim, map) -> {
            claims.computeIfAbsent(dim, k -> new HashMap<>()).putAll(map);
        });
    }

    public static void removeClaims(Map<ResourceLocation, List<Long>> chunks){
        chunks.forEach((dim, list) -> {
            Map<Long, Integer> dimClaims = claims.get(dim);
            if (dimClaims != null) {
                list.forEach(dimClaims::remove);
            }
        });
    }

    public static Map<ResourceLocation, Map<Long, Integer>> getClaims() {
        return claims;
    }
}