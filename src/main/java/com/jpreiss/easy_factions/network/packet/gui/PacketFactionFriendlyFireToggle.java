package com.jpreiss.easy_factions.network.packet.gui;

import com.jpreiss.easy_factions.network.NetworkHandler;
import com.jpreiss.easy_factions.server.faction.FactionStateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> Server packet for creating a faction
 */
public class PacketFactionFriendlyFireToggle {

    private final boolean friendlyFire;

    public PacketFactionFriendlyFireToggle(boolean friendlyFire) {
        this.friendlyFire = friendlyFire;
    }

    public static void encode(PacketFactionFriendlyFireToggle msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.friendlyFire);
    }

    public static PacketFactionFriendlyFireToggle decode(FriendlyByteBuf buf) {
        return new PacketFactionFriendlyFireToggle(buf.readBoolean());
    }

    public static void handle(PacketFactionFriendlyFireToggle msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            MinecraftServer server = player.getServer();
            if (server == null) return;

            FactionStateManager manager = FactionStateManager.get(player.getServer());

            try {
                manager.setFriendlyFire(player, msg.friendlyFire);
                // If successful, re-open/refresh the GUI
                PacketOpenFactionGui.handle(new PacketOpenFactionGui(), ctx);
            } catch (Exception e) {
                NetworkHandler.sendToPlayer(new PacketOpenErrorPopup(e.getMessage()), player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
