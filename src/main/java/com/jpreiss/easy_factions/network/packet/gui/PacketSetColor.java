package com.jpreiss.easy_factions.network.packet.gui;

import com.jpreiss.easy_factions.network.NetworkHandler;
import com.jpreiss.easy_factions.server.alliance.AllianceStateManager;
import com.jpreiss.easy_factions.server.faction.FactionStateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSetColor {
    private final String color;
    private final boolean isAlliance;

    public PacketSetColor(String color, boolean isAlliance) {
        this.color = color;
        this.isAlliance = isAlliance;
    }

    public static void encode(PacketSetColor msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.color);
        buf.writeBoolean(msg.isAlliance);
    }

    public static PacketSetColor decode(FriendlyByteBuf buf) {
        return new PacketSetColor(buf.readUtf(), buf.readBoolean());
    }

    public static void handle(PacketSetColor msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            MinecraftServer server = player.getServer();
            if (server == null) return;

            try {
                if (msg.isAlliance) {
                    AllianceStateManager.get(server).setColor(msg.color, player, server);
                } else {
                    FactionStateManager.get(server).setColor(msg.color, player);
                }
                // Refresh GUI
                PacketOpenFactionGui.handle(new PacketOpenFactionGui(), ctx);
            } catch (Exception e) {
                NetworkHandler.sendToPlayer(new PacketOpenErrorPopup(e.getMessage()), player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}