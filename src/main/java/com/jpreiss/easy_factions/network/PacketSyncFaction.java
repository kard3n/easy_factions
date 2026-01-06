package com.jpreiss.easy_factions.network;

import com.jpreiss.easy_factions.client.ClientFactionData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class PacketSyncFaction {
    private final Map<UUID, String> playerFactions;
    private final Map<String, String> factionAlliances;

    public PacketSyncFaction(Map<UUID, String> playerFactions, Map<String, String> factionAlliances) {
        this.playerFactions = playerFactions;
        this.factionAlliances = factionAlliances;
    }

    /**
     * Encodes the packet into the provided buffer
     *
     * @param msg The instance of this class to encode
     * @param buf The buffer to write to
     */
    public static void encode(PacketSyncFaction msg, FriendlyByteBuf buf) {
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
    }

    /**
     * Decodes the packet from the provided buffer
     * @param buf The buffer to read from
     * @return A new instance of this class from the decoded data
     */
    public static PacketSyncFaction decode(FriendlyByteBuf buf) {
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

        return new PacketSyncFaction(playerFactions, factionAlliances);
    }

    public static void handle(PacketSyncFaction msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientFactionData.update(msg.playerFactions, msg.factionAlliances)));
        ctx.get().setPacketHandled(true);
    }
}
