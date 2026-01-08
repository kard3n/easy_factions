package com.jpreiss.easy_factions.network.packet;

import com.jpreiss.easy_factions.client.ClientAllianceData;
import com.jpreiss.easy_factions.client.ClientFactionData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class PacketSyncFactionAlliance {
    private final Map<UUID, String> playerFactions;
    private final Map<String, String> factionAlliances;
    private final Map<String, String> factionAbbreviations;
    private final Map<String, String> allianceAbbreviations;


    public PacketSyncFactionAlliance(Map<UUID, String> playerFactions, Map<String, String> factionAlliances, Map<String, String> factionAbbreviations, Map<String, String> allianceAbbreviations) {
        this.playerFactions = playerFactions;
        this.factionAlliances = factionAlliances;
        this.factionAbbreviations = factionAbbreviations;
        this.allianceAbbreviations = allianceAbbreviations;

    }

    /**
     * Encodes the packet into the provided buffer
     *
     * @param msg The instance of this class to encode
     * @param buf The buffer to write to
     */
    public static void encode(PacketSyncFactionAlliance msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.playerFactions.size());
        msg.playerFactions.forEach((uuid, faction) -> {
            buf.writeUUID(uuid);
            buf.writeUtf(faction);
        });

        buf.writeInt(msg.factionAlliances.size());
        msg.factionAlliances.forEach((faction, alliance) -> {
            buf.writeUtf(faction);
            buf.writeUtf(alliance);
        });

        buf.writeInt(msg.factionAbbreviations.size());
        msg.factionAbbreviations.forEach((faction, abbreviation) -> {
            buf.writeUtf(faction);
            buf.writeUtf(abbreviation);
        });

        buf.writeInt(msg.allianceAbbreviations.size());
        msg.allianceAbbreviations.forEach((alliance, abbreviation) -> {
            buf.writeUtf(alliance);
            buf.writeUtf(abbreviation);
        });
    }

    /**
     * Decodes the packet from the provided buffer
     * @param buf The buffer to read from
     * @return A new instance of this class from the decoded data
     */
    public static PacketSyncFactionAlliance decode(FriendlyByteBuf buf) {
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

    public static void handle(PacketSyncFactionAlliance msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ClientFactionData.update(msg.playerFactions, msg.factionAbbreviations);
            ClientAllianceData.update(msg.factionAlliances, msg.allianceAbbreviations);
        }));
        ctx.get().setPacketHandled(true);
    }
}
