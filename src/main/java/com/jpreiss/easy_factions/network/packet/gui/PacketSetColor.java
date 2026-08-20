package com.jpreiss.easy_factions.network.packet.gui;

import com.jpreiss.easy_factions.network.NetworkHandler;
import com.jpreiss.easy_factions.server.alliance.AllianceStateManager;
import com.jpreiss.easy_factions.server.faction.FactionStateManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketSetColor(String color, boolean isAlliance) implements CustomPacketPayload {
    public static final Type<PacketSetColor> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("easy_factions", "packet_set_color"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketSetColor> STREAM_CODEC =
        StreamCodec.ofMember(PacketSetColor::write, PacketSetColor::read);

    private static PacketSetColor read(RegistryFriendlyByteBuf buf) {
        return new PacketSetColor(buf.readUtf(), buf.readBoolean());
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(color);
        buf.writeBoolean(isAlliance);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PacketSetColor msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;
            MinecraftServer server = player.getServer();
            if (server == null) return;

            try {
                if (msg.isAlliance()) {
                    AllianceStateManager.get(server).setColor(msg.color(), player, server);
                } else {
                    FactionStateManager.get(server).setColor(msg.color(), player);
                }
                // Refresh GUI
                PacketOpenFactionGui.handle(new PacketOpenFactionGui(), ctx);
            } catch (Exception e) {
                NetworkHandler.sendToPlayer(new PacketOpenErrorPopup(e.getMessage()), player);
            }
        });
    }
}