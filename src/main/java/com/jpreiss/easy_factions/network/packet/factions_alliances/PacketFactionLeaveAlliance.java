package com.jpreiss.easy_factions.network.packet.factions_alliances;

import com.jpreiss.easy_factions.client.data_store.ClientAllianceData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketFactionLeaveAlliance(String factionName) implements CustomPacketPayload {
    public static final Type<PacketFactionLeaveAlliance> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("easy_factions", "packet_faction_leave_alliance"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketFactionLeaveAlliance> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, PacketFactionLeaveAlliance::factionName, PacketFactionLeaveAlliance::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketFactionLeaveAlliance msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientAllianceData.removeFactionInformation(msg.factionName());
        });
    }
}
