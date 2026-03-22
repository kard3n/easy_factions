package com.jpreiss.easy_factions.server.claims;

import com.jpreiss.easy_factions.server.ServerConfig;
import com.jpreiss.easy_factions.server.claims.model.ClaimData;
import com.jpreiss.easy_factions.server.claims.model.ClaimType;
import com.jpreiss.easy_factions.server.faction.Faction;
import com.jpreiss.easy_factions.server.faction.FactionStateManager;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.*;

public class ChunkClaimSelectionManager {
    private static final Map<UUID, ChunkPos> pos1 = new HashMap<>();
    private static final Map<UUID, ResourceKey<Level>> pos1Dim = new HashMap<>();
    private static final Map<UUID, ChunkPos> pos2 = new HashMap<>();
    private static final Map<UUID, ResourceKey<Level>> pos2Dim = new HashMap<>();

    public static ChunkPos setPos1(ServerPlayer player) {
        pos1.put(player.getUUID(), player.chunkPosition());
        pos1Dim.put(player.getUUID(), player.level().dimension());
        return player.chunkPosition();
    }

    public static ChunkPos setPos2(ServerPlayer player) {
        pos2.put(player.getUUID(), player.chunkPosition());
        pos2Dim.put(player.getUUID(), player.level().dimension());
        return player.chunkPosition();
    }

    /**
     * Attempt claiming chunks for a faction
     *
     * @return Success message
     */
    public static String claimChunksFaction(ServerPlayer player, MinecraftServer server) throws RuntimeException {
        ChunkPos p1 = pos1.get(player.getUUID());
        ChunkPos p2 = pos2.get(player.getUUID());
        ResourceKey<Level> dim = getSelectionDimension(player.getUUID());

        if (!ServerConfig.factionClaimDimensions.contains(dim.location().toString())) {
            throw new RuntimeException("Faction chunks cannot be claimed in this dimensions");
        }

        // Check that the player is an authorized faction member
        FactionStateManager factions = FactionStateManager.get(server);
        Faction faction = factions.getFactionByPlayer(player.getUUID());
        if (faction == null) throw new RuntimeException("You are not in a faction.");
        if (!factions.playerIsOwnerOrOfficer(player.getUUID()))
            throw new RuntimeException("Only leaders/officers can claim faction land.");

        if (p1 == null || p2 == null) {
            throw new RuntimeException("You must set both positions first");
        }

        ClaimManager claimManager = ClaimManager.get(server);
        List<ChunkPos> chunksToClaim = getUnclaimedChunks(p1, p2, claimManager, dim);

        if (chunksToClaim.isEmpty()) {
            throw new RuntimeException("No chunks in your selected area can be claimed");
        }


        int cost = chunksToClaim.size() * ServerConfig.chunkCost;
        if (claimManager.getPoints(faction.getName()) < cost) {
            throw new RuntimeException("Faction does not have enough points. Cost: " + cost);
        }

        claimManager.addPoints(faction.getName(), -cost);

        HashMap<ResourceLocation, List<ChunkPos>> claimedChunksMap = new HashMap<>();
        claimedChunksMap.put(dim.location(), chunksToClaim);
        claimManager.claimChunks(claimedChunksMap, ClaimType.FACTION, faction.getName(), faction.getColor(), server);

        for (ChunkPos pos : chunksToClaim) {
            claimManager.claimChunk(dim, pos, ClaimType.FACTION, faction.getName(), faction.getColor());
        }

        return chunksToClaim.size() + " chunks claimed for " + cost + " points. Remaining points: " + claimManager.getPoints(faction.getName()) + ".";
    }

    /**
     * Unclaim faction chunks in the area
     *
     * @return The amount of chunks unclaimed
     */
    public static int unclaimChunksFaction(ServerPlayer player, MinecraftServer server) throws RuntimeException {
        ChunkPos p1 = pos1.get(player.getUUID());
        ChunkPos p2 = pos2.get(player.getUUID());
        ResourceKey<Level> dim = getSelectionDimension(player.getUUID());

        // Check that the player is an authorized faction member
        FactionStateManager factions = FactionStateManager.get(server);
        Faction faction = factions.getFactionByPlayer(player.getUUID());
        if (faction == null) throw new RuntimeException("You are not in a faction.");
        if (!factions.playerIsOwnerOrOfficer(player.getUUID()))
            throw new RuntimeException("Only leaders/officers can claim faction land.");

        if (p1 == null || p2 == null) {
            throw new RuntimeException("You must set both positions first");
        }

        ClaimManager claimManager = ClaimManager.get(server);

        List<ChunkPos> chunksToUnclaim = getClaimedChunks(p1, p2, claimManager, dim);
        List<ChunkPos> unclaimedChunks = new ArrayList<>();

        ClaimData currentClaim;
        for (ChunkPos pos : chunksToUnclaim) {
            currentClaim = claimManager.getClaim(dim, pos);
            if (currentClaim.type == ClaimType.FACTION && currentClaim.owner.equals(faction.getName())) {
                unclaimedChunks.add(pos);
                if (ServerConfig.refundCostUnclaim) claimManager.addPoints(currentClaim.owner, ServerConfig.chunkCost);
            }

        }

        HashMap<ResourceLocation, List<Long>> unclaimUpdate = new HashMap<>();
        unclaimUpdate.put(dim.location(), unclaimedChunks.stream().map(ChunkPos::toLong).toList());
        claimManager.unclaimChunks(unclaimUpdate, server);

        return unclaimedChunks.size();
    }

    /**
     * Claim core chunks
     *
     * @return Message to show to the user
     */
    public static String claimChunksCore(ServerPlayer player, MinecraftServer server) throws RuntimeException {
        ChunkPos p1 = pos1.get(player.getUUID());
        ChunkPos p2 = pos2.get(player.getUUID());
        ResourceKey<Level> dim = getSelectionDimension(player.getUUID());

        if (!ServerConfig.coreClaimDimensions.contains(dim.location().toString())) {
            throw new RuntimeException("Personal chunks cannot be claimed in this dimensions");
        }

        if (p1 == null || p2 == null) {
            throw new RuntimeException("You must set both positions first");
        }

        ClaimManager claimManager = ClaimManager.get(server);
        List<ChunkPos> chunksToClaim = getUnclaimedChunks(p1, p2, claimManager, dim);

        if (chunksToClaim.isEmpty()) {
            throw new RuntimeException("No chunks in your selected area can be claimed");
        }

        if (chunksToClaim.size() > ServerConfig.coreChunkAmount) {
            throw new RuntimeException("You cannot claim more than than " + ServerConfig.coreChunkAmount + " core chunks.");
        }

        // Calculate how many core chunks the player has already claimed
        int claimedCoreChunks = claimManager.getCoreChunkCount(player.getUUID());

        if (claimedCoreChunks + chunksToClaim.size() > ServerConfig.coreChunkAmount) {
            throw new RuntimeException("You cannot claim more than " + (ServerConfig.coreChunkAmount - claimedCoreChunks) + " more core chunks.");
        }

        HashMap<ResourceLocation, List<ChunkPos>> claimedChunksMap = new HashMap<>();
        claimedChunksMap.put(dim.location(), chunksToClaim);
        claimManager.claimChunks(claimedChunksMap, ClaimType.CORE, player.getUUID().toString(), 0xFFFFFF, server);

        return "You claimed " + chunksToClaim.size() + " core chunks. Remaining core chunks: " + (ServerConfig.coreChunkAmount - claimedCoreChunks - chunksToClaim.size()) + ".";
    }


    /**
     * Attempt claiming chunks for admins. Previous claims are removed
     *
     * @return The amount of chunks claimed
     */
    public static int claimChunksAdmin(ServerPlayer player, MinecraftServer server) throws RuntimeException {
        ChunkPos p1 = pos1.get(player.getUUID());
        ChunkPos p2 = pos2.get(player.getUUID());
        ResourceKey<Level> dim = getSelectionDimension(player.getUUID());

        if (p1 == null || p2 == null) {
            throw new RuntimeException("You must set both positions first");
        }

        ClaimManager claimManager = ClaimManager.get(server);
        int minX = Math.min(p1.x, p2.x);
        int maxX = Math.max(p1.x, p2.x);
        int minZ = Math.min(p1.z, p2.z);
        int maxZ = Math.max(p1.z, p2.z);

        List<ChunkPos> claimedChunks = new ArrayList<>();
        ChunkPos currentPos;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                currentPos = new ChunkPos(x, z);
                if (ServerConfig.refundCostUnclaim && claimManager.isClaimed(dim, currentPos)) {
                    ClaimData claimData = claimManager.getClaim(dim, currentPos);

                    // Refund points for faction
                    if (claimData.type == ClaimType.FACTION) {
                        claimManager.addPoints(claimData.owner, ServerConfig.chunkCost);
                    }
                }

                claimedChunks.add(currentPos);
            }
        }

        HashMap<ResourceLocation, List<ChunkPos>> claimedChunksMap = new HashMap<>();
        claimedChunksMap.put(dim.location(), claimedChunks);

        claimManager.claimChunks(claimedChunksMap, ClaimType.ADMIN, player.getUUID().toString(), 0xFF00FF, server);

        return claimedChunks.size();
    }

    /**
     * Unclaims all chunks in the area, ignoring of what type the claim is
     *
     * @return The amount of chunks unclaimed
     * @throws RuntimeException If not both positions are set
     */
    public static int unclaimChunksAdmin(ServerPlayer player, MinecraftServer server) throws RuntimeException {
        ChunkPos p1 = pos1.get(player.getUUID());
        ChunkPos p2 = pos2.get(player.getUUID());
        ResourceKey<Level> dim = getSelectionDimension(player.getUUID());

        if (p1 == null || p2 == null) {
            throw new RuntimeException("You must set both positions first");
        }

        ClaimManager claimManager = ClaimManager.get(server);

        List<ChunkPos> chunksToUnclaim = getClaimedChunks(p1, p2, claimManager, dim);

        List<ChunkPos> unclaimedChunks = new ArrayList<>();

        ClaimData currentClaim;
        for (ChunkPos pos : chunksToUnclaim) {
            currentClaim = claimManager.getClaim(dim, pos);
            if (ServerConfig.refundCostUnclaim && currentClaim.type == ClaimType.FACTION) {
                claimManager.addPoints(currentClaim.owner, ServerConfig.chunkCost);
            }
            unclaimedChunks.add(pos);
        }

        HashMap<ResourceLocation, List<Long>> unclaimUpdate = new HashMap<>();
        unclaimUpdate.put(dim.location(), unclaimedChunks.stream().map(ChunkPos::toLong).toList());
        claimManager.unclaimChunks(unclaimUpdate, server);

        return unclaimedChunks.size();
    }

    /**
     * Gets all unclaimed chunks inside the area
     */
    private static List<ChunkPos> getUnclaimedChunks(ChunkPos p1, ChunkPos p2, ClaimManager claimManager, ResourceKey<Level> dim) {
        // Calculate chunk rectangle
        int minX = Math.min(p1.x, p2.x);
        int maxX = Math.max(p1.x, p2.x);
        int minZ = Math.min(p1.z, p2.z);
        int maxZ = Math.max(p1.z, p2.z);


        // Get all claimable chunks in the marked area
        LinkedList<ChunkPos> chunksToClaim = new LinkedList<>();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                ChunkPos pos = new ChunkPos(x, z);
                if (!claimManager.isClaimed(dim, pos)) {
                    chunksToClaim.add(pos);
                }
            }
        }

        return chunksToClaim;
    }

    /**
     * Get all claimed chunks in the area
     */
    private static List<ChunkPos> getClaimedChunks(ChunkPos p1, ChunkPos p2, ClaimManager claimManager, ResourceKey<Level> dim) {
        // Calculate chunk rectangle
        int minX = Math.min(p1.x, p2.x);
        int maxX = Math.max(p1.x, p2.x);
        int minZ = Math.min(p1.z, p2.z);
        int maxZ = Math.max(p1.z, p2.z);


        // Get all claimed chunks in the marked area
        LinkedList<ChunkPos> chunksToClaim = new LinkedList<>();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                ChunkPos pos = new ChunkPos(x, z);
                if (claimManager.isClaimed(dim, pos)) {
                    chunksToClaim.add(pos);
                }
            }
        }

        return chunksToClaim;
    }

    private static ResourceKey<Level> getSelectionDimension(UUID uuid) {
        ResourceKey<Level> dim1 = pos1Dim.get(uuid);
        ResourceKey<Level> dim2 = pos2Dim.get(uuid);
        if (dim1 == null || dim2 == null) throw new RuntimeException("You must set both positions first");
        if (!dim1.equals(dim2)) throw new RuntimeException("Positions are in different dimensions");
        return dim1;
    }
}