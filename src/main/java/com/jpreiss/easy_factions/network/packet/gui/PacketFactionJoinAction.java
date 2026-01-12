package com.jpreiss.easy_factions.network.packet.gui;

import com.jpreiss.easy_factions.server.faction.FactionStateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> Server packet for joining a faction
 */
public class PacketFactionJoinAction {

    private final String factionName;

    public PacketFactionJoinAction(String factionName) {
        this.factionName = factionName;
    }

    public static void encode(PacketFactionJoinAction msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.factionName);
    }

    public static PacketFactionJoinAction decode(FriendlyByteBuf buf) {
        return new PacketFactionJoinAction(buf.readUtf());
    }

    public static void handle(PacketFactionJoinAction msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            MinecraftServer server = player.getServer();
            if (server == null) return;

            FactionStateManager manager = FactionStateManager.get(player.getServer());

            try {
                manager.joinFaction(player, msg.factionName, player.getServer());
                // If successful, re-open/refresh the GUI
                PacketOpenFactionGui.handle(new PacketOpenFactionGui(), ctx);
            } catch (Exception e) {
                // Ideally send an error message packet back to display in GUI
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
