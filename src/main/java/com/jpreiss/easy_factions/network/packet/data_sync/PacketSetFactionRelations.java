package com.jpreiss.easy_factions.network.packet.data_sync;

import com.jpreiss.easy_factions.client.data_store.ClientRelationshipData;
import com.jpreiss.easy_factions.common.RelationshipStatus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class PacketSetFactionRelations {
    private final Map<String, RelationshipStatus> relationships;

    public PacketSetFactionRelations(Map<String, RelationshipStatus> relationships) {
        this.relationships = relationships;

    }

    /**
     * Encodes the packet into the provided buffer
     *
     * @param msg The instance of this class to encode
     * @param buf The buffer to write to
     */
    public static void encode(PacketSetFactionRelations msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.relationships.size());
        msg.relationships.forEach((faction, relationship) -> {
            buf.writeUtf(faction);
            buf.writeUtf(relationship.name());
        });
    }


    /**
     * Decodes the packet from the provided buffer
     * @param buf The buffer to read from
     * @return A new instance of this class from the decoded data
     */
    public static PacketSetFactionRelations decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        Map<String, RelationshipStatus> relationships = new HashMap<>();
        for (int i = 0; i < size; i++) {
           relationships.put(buf.readUtf(), RelationshipStatus.valueOf(buf.readUtf()));
        }

        return new PacketSetFactionRelations(relationships);
    }

    public static void handle(PacketSetFactionRelations msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ClientRelationshipData.setRelationships(msg.relationships);
        }));
        ctx.get().setPacketHandled(true);
    }
}
