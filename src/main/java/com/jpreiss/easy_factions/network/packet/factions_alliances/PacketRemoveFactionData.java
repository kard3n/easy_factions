package com.jpreiss.easy_factions.network.packet.factions_alliances;

import com.jpreiss.easy_factions.client.data_store.ClientAllianceData;
import com.jpreiss.easy_factions.client.data_store.ClientFactionData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketRemoveFactionData(String factionName) implements CustomPacketPayload {
    public static final Type<PacketRemoveFactionData> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("easy_factions", "packet_remove_faction_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketRemoveFactionData> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, PacketRemoveFactionData::factionName, PacketRemoveFactionData::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketRemoveFactionData msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientFactionData.removeFaction(msg.factionName());
            ClientAllianceData.removeFactionInformation(msg.factionName());
        });
    }
}
