package com.jpreiss.easy_factions.network.packet.factions_alliances;

import com.jpreiss.easy_factions.client.data_store.ClientAllianceData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketUpdateAllianceAbbreviation(String allianceName, String abbreviation) implements CustomPacketPayload {
    public static final Type<PacketUpdateAllianceAbbreviation> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("easy_factions", "packet_update_alliance_abbreviation"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketUpdateAllianceAbbreviation> STREAM_CODEC =
        StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                PacketUpdateAllianceAbbreviation::allianceName,
                ByteBufCodecs.STRING_UTF8,
                PacketUpdateAllianceAbbreviation::abbreviation,
                PacketUpdateAllianceAbbreviation::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PacketUpdateAllianceAbbreviation msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientAllianceData.update(msg.allianceName(), msg.abbreviation());
        });
    }
}
