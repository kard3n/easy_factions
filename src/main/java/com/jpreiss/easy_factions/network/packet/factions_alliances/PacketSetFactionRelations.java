package com.jpreiss.easy_factions.network.packet.factions_alliances;

import com.jpreiss.easy_factions.client.data_store.ClientRelationshipData;
import com.jpreiss.easy_factions.common.RelationshipStatus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

public record PacketSetFactionRelations(Map<String, RelationshipStatus> relationships) implements CustomPacketPayload {
    public static final Type<PacketSetFactionRelations> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("easy_factions", "packet_set_faction_relations"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketSetFactionRelations> STREAM_CODEC =
        StreamCodec.ofMember(PacketSetFactionRelations::write, PacketSetFactionRelations::read);

    private static PacketSetFactionRelations read(RegistryFriendlyByteBuf buf) {
        int size = buf.readInt();
        Map<String, RelationshipStatus> relationships = new HashMap<>();
        for (int i = 0; i < size; i++) {
           relationships.put(buf.readUtf(), RelationshipStatus.valueOf(buf.readUtf()));
        }

        return new PacketSetFactionRelations(relationships);
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeInt(relationships.size());
        relationships.forEach((faction, relationship) -> {
            buf.writeUtf(faction);
            buf.writeUtf(relationship.name());
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PacketSetFactionRelations msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientRelationshipData.setRelationships(msg.relationships());
        });
    }
}
