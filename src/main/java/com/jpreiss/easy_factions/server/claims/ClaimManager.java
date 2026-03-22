package com.jpreiss.easy_factions.server.claims;

import com.jpreiss.easy_factions.network.NetworkManager;
import com.jpreiss.easy_factions.server.claims.model.ClaimData;
import com.jpreiss.easy_factions.server.claims.model.ClaimType;
import com.jpreiss.easy_factions.server.faction.Faction;
import com.jpreiss.easy_factions.server.faction.FactionStateManager;
import com.jpreiss.easy_factions.server.ServerConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.jpreiss.easy_factions.network.NetworkManager.sendClaimUpdate;


public class ClaimManager extends SavedData {
    private static final String DATA_NAME = "faction_claims";

    // Claim Storage: Dimension -> (ChunkLong -> ClaimData)
    private final Map<ResourceKey<Level>, Map<Long, ClaimData>> claimMap = new HashMap<>();

    // Fast Lookup: FactionName -> Dimension -> Set of Chunks
    private final Map<String, Map<ResourceKey<Level>, Set<Long>>> factionClaims = new HashMap<>();

    // Fast Lookup: PlayerUUID -> Dimension -> Set of Core ChunkLongs
    private final Map<UUID, Map<ResourceKey<Level>, Set<Long>>> playerCoreClaims = new HashMap<>();

    // Points storage: FactionName -> Points
    private final Map<String, Integer> factionPoints = new HashMap<>();

    // Kill points: AttackerFaction -> (VictimFaction -> Points)
    private final Map<String, Map<String, Integer>> killPoints = new HashMap<>();

    public static ClaimManager get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(ClaimManager::load, ClaimManager::create, DATA_NAME);
    }

    public static ClaimManager create() {
        return new ClaimManager();
    }

    // Points Logic

    public int getPoints(String factionName) {
        return factionPoints.getOrDefault(factionName, 0);
    }

    public void addPoints(String factionName, int amount) {
        factionPoints.merge(factionName, amount, Integer::sum);
        this.setDirty();
    }

    public int getKillPoints(String attacker, String victim) {
        return killPoints.getOrDefault(attacker, Collections.emptyMap()).getOrDefault(victim, 0);
    }

    public void addKillPoints(String attacker, String victim, int amount) {
        killPoints.computeIfAbsent(attacker, k -> new HashMap<>())
                .merge(victim, amount, Integer::sum);
        this.setDirty();
    }

    public void reduceKillPoints(String attacker, String victim, int amount) {
        if (killPoints.containsKey(attacker)) {
            Map<String, Integer> victimMap = killPoints.get(attacker);
            int current = victimMap.getOrDefault(victim, 0);
            int newVal = Math.max(0, current - amount);
            if (newVal == 0) {
                victimMap.remove(victim);
                if (victimMap.isEmpty()) killPoints.remove(attacker);
            } else {
                victimMap.put(victim, newVal);
            }
            this.setDirty();
        }
    }

    // Claim Logic

    public boolean isClaimed(ResourceKey<Level> dim, ChunkPos pos) {
        return claimMap.getOrDefault(dim, Collections.emptyMap()).containsKey(pos.toLong());
    }

    public ClaimData getClaim(ResourceKey<Level> dim, ChunkPos pos) {
        return claimMap.getOrDefault(dim, Collections.emptyMap()).get(pos.toLong());
    }

    public Map<ResourceKey<Level>, Map<Long, ClaimData>> getClaimMap() {
        return claimMap;
    }

    public void claimChunks(HashMap<ResourceLocation, List<ChunkPos>> claims, ClaimType type, String owner, int color, MinecraftServer server){
        for (Map.Entry<ResourceLocation, List<ChunkPos>> entry : claims.entrySet()){

            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, entry.getKey());
            for (ChunkPos chunk : entry.getValue()){
                claimChunk(dim, chunk, type, owner, color);
            }
            sendClaimUpdate(dim, entry.getValue(), color, server);
        }
    }

    public void claimChunk(ResourceKey<Level> dim, ChunkPos pos, ClaimType type, String owner, int color) {
        long key = pos.toLong();
        ClaimData data = new ClaimData(type, owner, color);
        claimMap.computeIfAbsent(dim, k -> new HashMap<>()).put(key, data);

        if (type == ClaimType.FACTION) {
            factionClaims.computeIfAbsent(owner, k -> new HashMap<>())
                    .computeIfAbsent(dim, k -> new HashSet<>()).add(key);
        } else if (type == ClaimType.CORE) {
            playerCoreClaims.computeIfAbsent(UUID.fromString(owner), k -> new HashMap<>())
                    .computeIfAbsent(dim, k -> new HashSet<>()).add(key);
        }
        this.setDirty();
    }

    /**
     * Unclaim a chunk. This does not notify clients
     */
    public void unclaimChunk(ResourceKey<Level> dim, ChunkPos pos) {
        long key = pos.toLong();
        Map<Long, ClaimData> dimClaims = claimMap.get(dim);
        if (dimClaims == null) return;

        ClaimData data = dimClaims.remove(key);
        if (data != null) {
            if (data.type == ClaimType.FACTION) {
                Map<ResourceKey<Level>, Set<Long>> dims = factionClaims.get(data.owner);
                if (dims != null) {
                    Set<Long> claims = dims.get(dim);
                    if (claims != null) {
                        claims.remove(key);
                        if (claims.isEmpty()) dims.remove(dim);
                        if (dims.isEmpty()) factionClaims.remove(data.owner);
                    }
                }
            } else if (data.type == ClaimType.CORE) {
                Map<ResourceKey<Level>, Set<Long>> dims = playerCoreClaims.get(UUID.fromString(data.owner));
                if (dims != null) {
                    Set<Long> claims = dims.get(dim);
                    if (claims != null) {
                        claims.remove(key);
                        if (claims.isEmpty()) dims.remove(dim);
                        if (dims.isEmpty()) playerCoreClaims.remove(UUID.fromString(data.owner));
                    }
                }
            }
            this.setDirty();
        }
    }

    /**
     * Unclaim multiple chunks. Clients are notified
     */
    public void unclaimChunks(HashMap<ResourceLocation, List<Long>> unclaimUpdate, MinecraftServer server) {
        for (Map.Entry<ResourceLocation, List<Long>> entry : unclaimUpdate.entrySet()) {
            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, entry.getKey());
            for (Long chunk : entry.getValue()) {
                ChunkPos pos = new ChunkPos(chunk);
                unclaimChunk(dim, pos);
            }
        }

        NetworkManager.notifyChunkUnclaim(unclaimUpdate, server);
    }

    public int getCoreChunkCount(UUID playerUUID) {
        Map<ResourceKey<Level>, Set<Long>> dims = playerCoreClaims.get(playerUUID);
        if (dims == null) return 0;
        return dims.values().stream().mapToInt(Set::size).sum();
    }

    public void deleteFactionData(String factionName){
        factionPoints.remove(factionName);
        factionClaims.remove(factionName);

        killPoints.remove(factionName);
        for (Map<String, Integer> victimMap : killPoints.values()) {
            victimMap.remove(factionName);
        }
    }

    public void changeFactionColor(String name, int color, MinecraftServer server) {
        for(Map.Entry<ResourceKey<Level>, Map<Long, ClaimData>> entry : claimMap.entrySet()){
            LinkedList<ChunkPos> changedChunks = new LinkedList<>();
            for(Map.Entry<Long, ClaimData> claimEntry : entry.getValue().entrySet()){
                if(claimEntry.getValue().type == ClaimType.FACTION && claimEntry.getValue().owner.equals(name)){
                    claimEntry.getValue().color = color;
                    changedChunks.add(new ChunkPos(claimEntry.getKey()));
                }
            }
            if(!changedChunks.isEmpty()){
                sendClaimUpdate(entry.getKey(), changedChunks, color, server);
            }
        }
        this.setDirty();
    }

    public Map<ResourceKey<Level>, Set<Long>> getFactionChunks(String factionName) {
        return factionClaims.getOrDefault(factionName, Collections.emptyMap());
    }

    public Map<ResourceKey<Level>, Set<Long>> getPlayerCoreChunks(UUID playerUuid) {
        return playerCoreClaims.getOrDefault(playerUuid, Collections.emptyMap());
    }

    public static ClaimManager load(CompoundTag tag) {
        ClaimManager manager = new ClaimManager();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        // Points
        CompoundTag pointsTag = tag.getCompound("Points");
        for (String key : pointsTag.getAllKeys()) {
            manager.factionPoints.put(key, pointsTag.getInt(key));
        }

        // War Points
        if (tag.contains("WarPoints")) {
            CompoundTag warPointsTag = tag.getCompound("WarPoints");
            for (String attacker : warPointsTag.getAllKeys()) {
                CompoundTag victimTag = warPointsTag.getCompound(attacker);
                Map<String, Integer> map = new HashMap<>();
                for (String victim : victimTag.getAllKeys()) {
                    map.put(victim, victimTag.getInt(victim));
                }
                manager.killPoints.put(attacker, map);
            }
        }

        FactionStateManager factionManager = FactionStateManager.get(server);

        // Claims
        ListTag list = tag.getList("Claims", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            long posLong = c.getLong("Pos");
            ClaimType type = ClaimType.valueOf(c.getString("Type"));
            String owner = c.getString("Owner");
            int color = 0xFFFFFF;
            if(type == ClaimType.FACTION){
                Faction faction = factionManager.getFactionByName(owner);
                if(faction != null){
                    color = faction.getColor();
                }
            } else if (type == ClaimType.ADMIN) {
                color = ServerConfig.adminClaimColor;
            } else if (type == ClaimType.CORE) {
                color = ServerConfig.coreClaimColor;
            }

            ResourceKey<Level> dim;
            if (c.contains("Dim")) {
                dim = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(c.getString("Dim")));
            } else {
                dim = Level.OVERWORLD;
            }

            ChunkPos pos = new ChunkPos(posLong);
            manager.claimChunk(dim, pos, type, owner, color);
        }
        return manager;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        CompoundTag pointsTag = new CompoundTag();
        factionPoints.forEach(pointsTag::putInt);
        tag.put("Points", pointsTag);

        CompoundTag warPointsTag = new CompoundTag();
        killPoints.forEach((attacker, victimMap) -> {
            CompoundTag victimTag = new CompoundTag();
            victimMap.forEach(victimTag::putInt);
            warPointsTag.put(attacker, victimTag);
        });
        tag.put("WarPoints", warPointsTag);

        ListTag list = new ListTag();
        for (Map.Entry<ResourceKey<Level>, Map<Long, ClaimData>> dimEntry : claimMap.entrySet()) {
            String dimStr = dimEntry.getKey().location().toString();
            for (Map.Entry<Long, ClaimData> entry : dimEntry.getValue().entrySet()) {
                CompoundTag c = new CompoundTag();
                c.putString("Dim", dimStr);
                c.putLong("Pos", entry.getKey());
                c.putString("Type", entry.getValue().type.name());
                c.putString("Owner", entry.getValue().owner);
                list.add(c);
            }
        }
        tag.put("Claims", list);
        return tag;
    }
}