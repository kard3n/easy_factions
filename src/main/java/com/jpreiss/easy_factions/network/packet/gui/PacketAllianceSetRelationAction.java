package com.jpreiss.easy_factions.network.packet.gui;

import com.jpreiss.easy_factions.common.RelationshipStatus;
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

public record PacketAllianceSetRelationAction(String allianceName, RelationshipStatus relationshipStatus) implements CustomPacketPayload {

    public static final Type<PacketAllianceSetRelationAction> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("easy_factions", "packet_alliance_set_relation_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketAllianceSetRelationAction> STREAM_CODEC =
        StreamCodec.ofMember(PacketAllianceSetRelationAction::write, PacketAllianceSetRelationAction::read);

    private static PacketAllianceSetRelationAction read(RegistryFriendlyByteBuf buf) {
        return new PacketAllianceSetRelationAction(buf.readUtf(), buf.readEnum(RelationshipStatus.class));
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(allianceName);
        buf.writeEnum(relationshipStatus);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PacketAllianceSetRelationAction msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;
            MinecraftServer server = player.getServer();
            if (server == null) return;

            AllianceStateManager allianceManager = AllianceStateManager.get(server);
            FactionStateManager factionStateManager = FactionStateManager.get(server);

            try {
                if (!factionStateManager.playerOwnsFaction(player.getUUID())){
                    throw new RuntimeException("Player is not owner of a faction");
                }
                allianceManager.setRelation(msg.allianceName(), player, msg.relationshipStatus());
                // If successful, re-open/refresh the GUI
                PacketOpenFactionGui.handle(new PacketOpenFactionGui(), ctx);
            } catch (Exception e) {
                NetworkHandler.sendToPlayer(new PacketOpenErrorPopup(e.getMessage()), player);
            }
        });
    }
}
