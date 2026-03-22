package com.jpreiss.easy_factions.client;

import com.jpreiss.easy_factions.client.data_store.ClientClaimCache;
import com.jpreiss.easy_factions.client.gui.ErrorPopupScreen;
import com.jpreiss.easy_factions.client.gui.FactionScreen;
import com.jpreiss.easy_factions.client.gui.NoFactionScreen;
import com.jpreiss.easy_factions.client.integration.JourneyMapCompat;
import com.jpreiss.easy_factions.network.packet.claims.PacketChunkClaim;
import com.jpreiss.easy_factions.network.packet.claims.PacketChunkUnclaim;
import com.jpreiss.easy_factions.network.packet.gui.PacketOpenErrorPopup;
import com.jpreiss.easy_factions.network.packet.gui.PacketSyncFactionGuiData;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class ClientPacketHandler {
    // Handles opening PacketSyncFactionGuiData on the clientside. Prevents import of screen class on serverside
    public static void handleSyncFactionGuiData(PacketSyncFactionGuiData msg) {
        if (msg.isInFaction()) {
            Minecraft.getInstance().setScreen(new FactionScreen(msg));
        } else {
            Minecraft.getInstance().setScreen(new NoFactionScreen(msg.getPlayerInvites()));
        }
    }

    public static void handleOpenErrorPopup(PacketOpenErrorPopup msg) {
        Minecraft.getInstance().setScreen(new ErrorPopupScreen(Minecraft.getInstance().screen, msg.getErrorMessage()));
    }

    /**
     * Handles adding chunks to the cache
     */
    public static void handleClaimChunk(PacketChunkClaim msg) {
        ClientClaimCache.addClaims(msg.getChunks());
        // Tell JourneyMap to recalculate the map for the affected dimensions
        msg.getChunks().keySet().forEach(dimLoc -> {
            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, dimLoc);
            JourneyMapCompat.rebuildDimension(dim);
        });
    }

    public static void handleUnclaimChunk(PacketChunkUnclaim msg) {
        ClientClaimCache.removeClaims(msg.getChunks());
        // Tell JourneyMap to recalculate the map for the affected dimensions
        msg.getChunks().keySet().forEach(dimLoc -> {
            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, dimLoc);
            JourneyMapCompat.rebuildDimension(dim);
        });
    }
}
