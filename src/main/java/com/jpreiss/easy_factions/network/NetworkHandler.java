package com.jpreiss.easy_factions.network;

import com.jpreiss.easy_factions.EasyFactions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EasyFactions.MODID, "main"),
            () -> PROTOCOL_VERSION,
            version -> version.equals(PROTOCOL_VERSION) || version.contains("ABSENT") || version.contains("ACCEPTVANILLA"),
            version -> version.equals(PROTOCOL_VERSION) || version.contains("ABSENT") || version.contains("ACCEPTVANILLA")
    );

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, PacketSyncFaction.class, PacketSyncFaction::encode, PacketSyncFaction::decode, PacketSyncFaction::handle);
    }

    /**
     * Safely sends a packet to a player.
     * IMPORTANT: Prevents crashing when sending to vanilla clients who don't have the mod.
     */
    public static void sendToPlayer(Object message, ServerPlayer player) {
        if (CHANNEL.isRemotePresent(player.connection.connection)) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
        }
    }
}