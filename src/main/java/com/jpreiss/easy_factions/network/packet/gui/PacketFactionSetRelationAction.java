package com.jpreiss.easy_factions.network.packet.gui;

import com.jpreiss.easy_factions.common.RelationshipStatus;
import com.jpreiss.easy_factions.network.NetworkHandler;
import com.jpreiss.easy_factions.server.faction.FactionStateManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketFactionSetRelationAction(String factionName, RelationshipStatus relationshipStatus) implements CustomPacketPayload {

    public static final Type<PacketFactionSetRelationAction> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("easy_factions", "packet_faction_set_relation_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketFactionSetRelationAction> STREAM_CODEC =
        StreamCodec.ofMember(PacketFactionSetRelationAction::write, PacketFactionSetRelationAction::read);

    private static PacketFactionSetRelationAction read(RegistryFriendlyByteBuf buf) {
        return new PacketFactionSetRelationAction(buf.readUtf(), buf.readEnum(RelationshipStatus.class));
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(factionName);
        buf.writeEnum(relationshipStatus);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PacketFactionSetRelationAction msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;
            MinecraftServer server = player.getServer();
            if (server == null) return;

            FactionStateManager manager = FactionStateManager.get(player.getServer());

            try {
                manager.setRelation(msg.factionName(), player, msg.relationshipStatus());
                // If successful, re-open/refresh the GUI
                PacketOpenFactionGui.handle(new PacketOpenFactionGui(), ctx);
            } catch (Exception e) {
            }
        });
    }
}
