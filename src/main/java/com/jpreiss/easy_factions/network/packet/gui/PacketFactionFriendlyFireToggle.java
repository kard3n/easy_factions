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

public record PacketFactionFriendlyFireToggle(boolean friendlyFire) implements CustomPacketPayload {

    public static final Type<PacketFactionFriendlyFireToggle> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("easy_factions", "packet_faction_friendly_fire_toggle"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketFactionFriendlyFireToggle> STREAM_CODEC =
        StreamCodec.ofMember(PacketFactionFriendlyFireToggle::write, PacketFactionFriendlyFireToggle::read);

    private static PacketFactionFriendlyFireToggle read(RegistryFriendlyByteBuf buf) {
        return new PacketFactionFriendlyFireToggle(buf.readBoolean());
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(friendlyFire);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PacketFactionFriendlyFireToggle msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;
            MinecraftServer server = player.getServer();
            if (server == null) return;

            FactionStateManager manager = FactionStateManager.get(player.getServer());

            try {
                manager.setFriendlyFire(player, msg.friendlyFire());
                // If successful, re-open/refresh the GUI
                PacketOpenFactionGui.handle(new PacketOpenFactionGui(), ctx);
            } catch (Exception e) {
                NetworkHandler.sendToPlayer(new PacketOpenErrorPopup(e.getMessage()), player);
            }
        });
    }
}
