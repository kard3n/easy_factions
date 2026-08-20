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

public record PacketFactionCreateAction(String factionName) implements CustomPacketPayload {

    public static final Type<PacketFactionCreateAction> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("easy_factions", "packet_faction_create_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketFactionCreateAction> STREAM_CODEC =
        StreamCodec.ofMember(PacketFactionCreateAction::write, PacketFactionCreateAction::read);

    private static PacketFactionCreateAction read(RegistryFriendlyByteBuf buf) {
        return new PacketFactionCreateAction(buf.readUtf());
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(factionName);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PacketFactionCreateAction msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;
            MinecraftServer server = player.getServer();
            if (server == null) return;

            FactionStateManager manager = FactionStateManager.get(player.getServer());

            try {
                manager.createFaction(msg.factionName(), null, player, player.getServer());
                // If successful, re-open/refresh the GUI
                PacketOpenFactionGui.handle(new PacketOpenFactionGui(), ctx);
            } catch (Exception e) {
                // Ignore exception UI creation for brevity until converted
                // NetworkHandler.sendToPlayer(new PacketOpenErrorPopup(e.getMessage()), player);
            }
        });
    }
}
