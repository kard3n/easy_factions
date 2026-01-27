package com.jpreiss.easy_factions.network.packet.gui;

import com.jpreiss.easy_factions.network.NetworkHandler;
import com.jpreiss.easy_factions.server.alliance.AllianceStateManager;
import com.jpreiss.easy_factions.server.faction.FactionStateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * Client -> Server packet for joining a faction
 */
public class PacketAllianceLeaveAction {

    public PacketAllianceLeaveAction() {

    }

    public static void encode(PacketAllianceLeaveAction msg, FriendlyByteBuf buf) {

    }

    public static PacketAllianceLeaveAction decode(FriendlyByteBuf buf) {
        return new PacketAllianceLeaveAction();
    }

    public static void handle(PacketAllianceLeaveAction msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            MinecraftServer server = player.getServer();
            if (server == null) return;

            AllianceStateManager manager = AllianceStateManager.get(server);

            try {
                manager.leaveAlliance(player, player.getServer());
                // If successful, re-open/refresh the GUI
                PacketOpenFactionGui.handle(new PacketOpenFactionGui(), ctx);
            } catch (Exception e) {
                NetworkHandler.sendToPlayer(new PacketOpenErrorPopup(e.getMessage()), player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
