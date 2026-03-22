package com.jpreiss.easy_factions.server.claims;


import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

@Mod.EventBusSubscriber
public class ChunkCleaner {

    private static boolean pendingWipe = false;
    // Saves the chunks that were claimed when the command was executed
    private static final Map<Path, Set<Long>> claimSnapshot = new HashMap<>();

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Schedules a chunk wipe for the next restart
     */
    public static void scheduleWipe(MinecraftServer server) {
        pendingWipe = true;
        claimSnapshot.clear();

        ClaimManager claimManager = ClaimManager.get(server);
        Path worldDir = server.getWorldPath(LevelResource.ROOT);

        // Take a snapshot of all dimensions and their claimed chunks
        for (ServerLevel level : server.getAllLevels()) {
            ResourceKey<Level> dimKey = level.dimension();
            Path dimPath = getDimensionPath(worldDir, dimKey.location());

            // Get claims for this dimension, or an empty set if there are none
            Map<Long, ?> dimClaims = claimManager.getClaimMap().get(dimKey);
            Set<Long> claimedLongs = dimClaims != null ? new HashSet<>(dimClaims.keySet()) : Collections.emptySet();

            claimSnapshot.put(dimPath, claimedLongs);
        }
    }

    /**
     * Stops a previously scheduled wipe
     */
    public static void stopWipe(){
        pendingWipe = false;
        claimSnapshot.clear();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        if (!pendingWipe) return;
        pendingWipe = false;

        LOGGER.atInfo().log("Beginning wipe of unclaimed chunks. This may take a moment...");

        for (Map.Entry<Path, Set<Long>> entry : claimSnapshot.entrySet()) {
            Path dimensionDir = entry.getKey();
            Set<Long> claimedChunks = entry.getValue();

            if (!Files.exists(dimensionDir)) continue;

            String[] subFolders = {"region", "entities", "poi"};
            for (String folder : subFolders) {
                Path folderPath = dimensionDir.resolve(folder);
                if (!Files.exists(folderPath)) continue;

                // Find all .mca files in this folder
                try (Stream<Path> files = Files.list(folderPath)) {
                    files.filter(p -> p.toString().endsWith(".mca")).forEach(mcaFile -> {
                        wipeUnclaimedInRegion(mcaFile, claimedChunks);
                    });
                } catch (IOException e) {
                    LOGGER.atError().log("Failed to read region folder: " + folderPath);
                }
            }
        }

        claimSnapshot.clear();
        LOGGER.atInfo().log("Unclaimed chunk wipe complete.");
    }

    private static void wipeUnclaimedInRegion(Path mcaFile, Set<Long> claimedChunks) {
        String fileName = mcaFile.getFileName().toString();
        String[] parts = fileName.split("\\.");

        // Ensure format is r.X.Z.mca
        if (parts.length != 4) return;

        try {
            int regionX = Integer.parseInt(parts[1]);
            int regionZ = Integer.parseInt(parts[2]);

            try (RandomAccessFile file = new RandomAccessFile(mcaFile.toFile(), "rw")) {
                if (file.length() < 4096) return; // Skip invalid or tiny files

                // Check all possible chunks in this region file
                for (int x = 0; x < 32; x++) {
                    for (int z = 0; z < 32; z++) {
                        // Convert pos to long
                        int absX = (regionX << 5) + x;
                        int absZ = (regionZ << 5) + z;
                        long chunkLong = ChunkPos.asLong(absX, absZ);

                        if (!claimedChunks.contains(chunkLong)) {
                            int headerOffset = 4 * (x + z * 32);

                            file.seek(headerOffset);
                            if (file.readInt() != 0) { // Don¡t write if there's nothing to write
                                file.seek(headerOffset);
                                file.writeInt(0);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.atError().log("Error wiping chunks in file: " + mcaFile);
        }
    }

    /**
     * Gets the path to a defined dimension
     * @param worldDir Path to the root of the world directory
     * @param dimLoc Dimension location
     * @return Path to the folder containing the dimension
     */
    private static Path getDimensionPath(Path worldDir, ResourceLocation dimLoc) {
        if (dimLoc.getNamespace().equals("minecraft")) {
            return switch (dimLoc.getPath()) {
                case "overworld" -> worldDir;
                case "the_nether" -> worldDir.resolve("DIM-1");
                case "the_end" -> worldDir.resolve("DIM1");
                default -> worldDir.resolve("dimensions").resolve(dimLoc.getNamespace()).resolve(dimLoc.getPath());
            };
        }
        return worldDir.resolve("dimensions").resolve(dimLoc.getNamespace()).resolve(dimLoc.getPath());
    }
}
