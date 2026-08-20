package com.jpreiss.easy_factions.network.packet.factions_alliances;

import com.jpreiss.easy_factions.client.data_store.ClientFactionData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketUpdateFactionAbbreviation(String factionName, String abbreviation) implements CustomPacketPayload {
    public static final Type<PacketUpdateFactionAbbreviation> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("easy_factions", "packet_update_faction_abbreviation"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketUpdateFactionAbbreviation> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    PacketUpdateFactionAbbreviation::factionName,
                    ByteBufCodecs.STRING_UTF8,
                    PacketUpdateFactionAbbreviation::abbreviation,
                    PacketUpdateFactionAbbreviation::new
            );

    public static void handle(PacketUpdateFactionAbbreviation msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientFactionData.update(msg.factionName(), msg.abbreviation());
        });
    }


    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
