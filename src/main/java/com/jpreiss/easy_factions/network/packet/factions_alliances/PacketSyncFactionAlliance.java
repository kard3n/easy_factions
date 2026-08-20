package com.jpreiss.easy_factions.network.packet.factions_alliances;

import com.jpreiss.easy_factions.client.data_store.ClientAllianceData;
import com.jpreiss.easy_factions.client.data_store.ClientFactionData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record PacketSyncFactionAlliance(Map<UUID, String> playerFactions, Map<String, String> factionAlliances, Map<String, String> factionAbbreviations, Map<String, String> allianceAbbreviations) implements CustomPacketPayload {
    public static final Type<PacketSyncFactionAlliance> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("easy_factions", "packet_sync_faction_alliance"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketSyncFactionAlliance> STREAM_CODEC =
        StreamCodec.ofMember(PacketSyncFactionAlliance::write, PacketSyncFactionAlliance::read);

    private static PacketSyncFactionAlliance read(RegistryFriendlyByteBuf buf) {
        int playerFactionsSize = buf.readInt();
        Map<UUID, String> playerFactions = new HashMap<>();
        for (int i = 0; i < playerFactionsSize; i++) {
            playerFactions.put(buf.readUUID(), buf.readUtf());
        }

        int factionAlliancesSize = buf.readInt();
        Map<String, String> factionAlliances = new HashMap<>();
        for (int i = 0; i < factionAlliancesSize; i++) {
            factionAlliances.put(buf.readUtf(), buf.readUtf());
        }

        int factionAbbreviationsSize = buf.readInt();
        Map<String, String> factionAbbreviations = new HashMap<>();
        for (int i = 0; i < factionAbbreviationsSize; i++) {
            factionAbbreviations.put(buf.readUtf(), buf.readUtf());
        }

        int allianceAbbreviationsSize = buf.readInt();
        Map<String, String> allianceAbbreviations = new HashMap<>();
        for (int i = 0; i < allianceAbbreviationsSize; i++) {
            allianceAbbreviations.put(buf.readUtf(), buf.readUtf());
        }

        return new PacketSyncFactionAlliance(playerFactions, factionAlliances, factionAbbreviations, allianceAbbreviations);
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeInt(playerFactions.size());
        playerFactions.forEach((uuid, faction) -> {
            buf.writeUUID(uuid);
            buf.writeUtf(faction);
        });

        buf.writeInt(factionAlliances.size());
        factionAlliances.forEach((faction, alliance) -> {
            buf.writeUtf(faction);
            buf.writeUtf(alliance);
        });

        buf.writeInt(factionAbbreviations.size());
        factionAbbreviations.forEach((faction, abbreviation) -> {
            buf.writeUtf(faction);
            buf.writeUtf(abbreviation);
        });

        buf.writeInt(allianceAbbreviations.size());
        allianceAbbreviations.forEach((alliance, abbreviation) -> {
            buf.writeUtf(alliance);
            buf.writeUtf(abbreviation);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PacketSyncFactionAlliance msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientFactionData.update(msg.playerFactions(), msg.factionAbbreviations());
            ClientAllianceData.update(msg.factionAlliances(), msg.allianceAbbreviations());
        });
    }
}
