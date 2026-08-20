package com.jpreiss.easy_factions.network.packet.factions_alliances;

import com.jpreiss.easy_factions.client.data_store.ClientAllianceData;
import com.jpreiss.easy_factions.client.data_store.ClientFactionData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record PacketRemovePlayerData(UUID playerUUID) implements CustomPacketPayload {
    public static final Type<PacketRemovePlayerData> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("easy_factions", "packet_remove_player_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketRemovePlayerData> STREAM_CODEC =
        StreamCodec.ofMember(PacketRemovePlayerData::write, PacketRemovePlayerData::read);

    private static PacketRemovePlayerData read(RegistryFriendlyByteBuf buf) {
        return new PacketRemovePlayerData(buf.readUUID());
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PacketRemovePlayerData msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            String playerFaction = ClientFactionData.removePlayer(msg.playerUUID());
            if (playerFaction != null && ClientFactionData.getFactionMemberCount(playerFaction) < 1) {
                ClientAllianceData.removeFactionInformation(playerFaction);
            }
        });
    }
}
