package com.jpreiss.easy_factions.network.packet.gui;

import com.jpreiss.easy_factions.network.NetworkHandler;
import com.jpreiss.easy_factions.server.faction.FactionStateManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketFactionJoinAction(String factionName) implements CustomPacketPayload {

    public static final Type<PacketFactionJoinAction> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("easy_factions", "packet_faction_join_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketFactionJoinAction> STREAM_CODEC =
        StreamCodec.ofMember(PacketFactionJoinAction::write, PacketFactionJoinAction::read);

    private static PacketFactionJoinAction read(RegistryFriendlyByteBuf buf) {
        return new PacketFactionJoinAction(buf.readUtf());
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(factionName);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PacketFactionJoinAction msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;
            MinecraftServer server = player.getServer();
            if (server == null) return;

            FactionStateManager manager = FactionStateManager.get(player.getServer());

            try {
                manager.joinFaction(player, msg.factionName(), player.getServer());
                // If successful, re-open/refresh the GUI
                PacketOpenFactionGui.handle(new PacketOpenFactionGui(), ctx);
            } catch (Exception e) {
                // Ignore parsing Error Popup here as it will be converted
                // NetworkHandler.sendToPlayer(new PacketOpenErrorPopup(e.getMessage()), player);
            }
        });
    }
}
