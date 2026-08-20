package com.jpreiss.easy_factions.network.packet.gui;

import com.jpreiss.easy_factions.client.ClientPacketHandler;
import com.jpreiss.easy_factions.common.MemberRank;
import com.jpreiss.easy_factions.common.RelationshipStatus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.*;

public record PacketSyncFactionGuiData(
        boolean inFaction,
        String factionName,
        Map<UUID, MemberRank> memberRanks,
        Map<UUID, String> playerNames,
        List<String> playerInvites,
        List<UUID> factionInvites,
        Map<String, RelationshipStatus> outgoingFactionRelationships,
        Map<String, RelationshipStatus> incomingFactionRelationships,
        List<String> factionNames,
        String allianceName,
        List<String> allianceMembers,
        List<String> allianceInvites,
        List<String> allianceNames,
        Map<String, RelationshipStatus> outgoingAllianceRelations,
        Map<String, RelationshipStatus> incomingAllianceRelations,
        boolean friendlyFire,
        int factionColor,
        int allianceColor,
        int factionAbbreviationMaxLength,
        int allianceAbbreviationMaxLength,
        boolean factionAbbreviationChangeAllowed,
        boolean allianceAbbreviationChangeAllowed
) implements CustomPacketPayload {

    public static final Type<PacketSyncFactionGuiData> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("easy_factions", "packet_sync_faction_gui_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketSyncFactionGuiData> STREAM_CODEC =
            StreamCodec.ofMember(PacketSyncFactionGuiData::write, PacketSyncFactionGuiData::read);

    // Constructor for when player IS in a faction
    public PacketSyncFactionGuiData(
            String factionName, Map<UUID, MemberRank> memberRanks,
            Map<UUID, String> playerNames,
            List<UUID> factionInvites,
            Map<String, RelationshipStatus> outgoingFactionRelationships,
            Map<String, RelationshipStatus> incomingFactionRelationships,
            List<String> factionNames,
            String allianceName,
            List<String> allianceMembers,
            List<String> allianceInvites,
            List<String> allianceNames,
            Map<String, RelationshipStatus> outgoingAllianceRelations,
            Map<String, RelationshipStatus> incomingAllianceRelations,
            boolean friendlyFire,
            int factionColor,
            int allianceColor,
            int factionAbbreviationMaxLength,
            int allianceAbbreviationMaxLength,
            boolean factionAbbreviationChangeAllowed,
            boolean allianceAbbreviationChangeAllowed) {
        this(true, factionName, memberRanks, playerNames, new ArrayList<>(), factionInvites,
                outgoingFactionRelationships, incomingFactionRelationships, factionNames,
                allianceName, allianceMembers, allianceInvites, allianceNames,
                outgoingAllianceRelations, incomingAllianceRelations, friendlyFire,
                factionColor, allianceColor, factionAbbreviationMaxLength, allianceAbbreviationMaxLength,
                factionAbbreviationChangeAllowed, allianceAbbreviationChangeAllowed);
    }

    // Constructor for when player is NOT in a faction
    public PacketSyncFactionGuiData(List<String> playerInvites) {
        this(false, "", new HashMap<>(), new HashMap<>(), playerInvites, new ArrayList<>(),
                new HashMap<>(), new HashMap<>(), new ArrayList<>(), null, new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new HashMap<>(), new HashMap<>(), false,
                0xFFFFFF, 0xFFFFFF, 0, 0, false, false);
    }

    private static PacketSyncFactionGuiData read(RegistryFriendlyByteBuf buf) {
        boolean inFaction = buf.readBoolean();
        String factionName = buf.readUtf();
        Map<UUID, MemberRank> memberRanks = buf.readMap(b -> b.readUUID(), b -> b.readEnum(MemberRank.class));
        Map<UUID, String> playerNames = buf.readMap(b -> b.readUUID(), b -> b.readUtf());
        List<String> playerInvites = buf.readList(b -> b.readUtf());
        List<UUID> factionInvites = buf.readList(b -> b.readUUID());
        Map<String, RelationshipStatus> outgoingRelationships = buf.readMap(b -> b.readUtf(), b -> b.readEnum(RelationshipStatus.class));
        Map<String, RelationshipStatus> incomingRelationships = buf.readMap(b -> b.readUtf(), b -> b.readEnum(RelationshipStatus.class));
        List<String> factionNames = buf.readList(b -> b.readUtf());
        String allianceName = buf.readUtf();
        if (allianceName.isEmpty()) allianceName = null;
        List<String> allianceMembers = buf.readList(b -> b.readUtf());
        List<String> allianceInvites = buf.readList(b -> b.readUtf());
        List<String> allianceNames = buf.readList(b -> b.readUtf());
        Map<String, RelationshipStatus> outgoingAllianceRelations = buf.readMap(b -> b.readUtf(), b -> b.readEnum(RelationshipStatus.class));
        Map<String, RelationshipStatus> incomingAllianceRelations = buf.readMap(b -> b.readUtf(), b -> b.readEnum(RelationshipStatus.class));
        boolean friendlyFire = buf.readBoolean();
        int factionColor = buf.readInt();
        int allianceColor = buf.readInt();
        int factionAbbreviationMaxLength = buf.readInt();
        int allianceAbbreviationMaxLength = buf.readInt();
        boolean factionAbbreviationChangeAllowed = buf.readBoolean();
        boolean allianceAbbreviationChangeAllowed = buf.readBoolean();

        return new PacketSyncFactionGuiData(
                inFaction, factionName, memberRanks, playerNames, playerInvites, factionInvites,
                outgoingRelationships, incomingRelationships, factionNames, allianceName,
                allianceMembers, allianceInvites, allianceNames, outgoingAllianceRelations,
                incomingAllianceRelations, friendlyFire, factionColor, allianceColor,
                factionAbbreviationMaxLength, allianceAbbreviationMaxLength,
                factionAbbreviationChangeAllowed, allianceAbbreviationChangeAllowed
        );
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(inFaction);
        buf.writeUtf(factionName);
        buf.writeMap(memberRanks, (b, k) -> b.writeUUID(k), (b, v) -> b.writeEnum(v));
        buf.writeMap(playerNames, (b, k) -> b.writeUUID(k), (b, v) -> b.writeUtf(v));
        buf.writeCollection(playerInvites, (b, v) -> b.writeUtf(v));
        buf.writeCollection(factionInvites, (b, v) -> b.writeUUID(v));
        buf.writeMap(outgoingFactionRelationships, (b, k) -> b.writeUtf(k), (b, v) -> b.writeEnum(v));
        buf.writeMap(incomingFactionRelationships, (b, k) -> b.writeUtf(k), (b, v) -> b.writeEnum(v));
        buf.writeCollection(factionNames, (b, v) -> b.writeUtf(v));
        buf.writeUtf(allianceName == null ? "" : allianceName);
        buf.writeCollection(allianceMembers, (b, v) -> b.writeUtf(v));
        buf.writeCollection(allianceInvites, (b, v) -> b.writeUtf(v));
        buf.writeCollection(allianceNames, (b, v) -> b.writeUtf(v));
        buf.writeMap(outgoingAllianceRelations, (b, k) -> b.writeUtf(k), (b, v) -> b.writeEnum(v));
        buf.writeMap(incomingAllianceRelations, (b, k) -> b.writeUtf(k), (b, v) -> b.writeEnum(v));
        buf.writeBoolean(friendlyFire);
        buf.writeInt(factionColor);
        buf.writeInt(allianceColor);
        buf.writeInt(factionAbbreviationMaxLength);
        buf.writeInt(allianceAbbreviationMaxLength);
        buf.writeBoolean(factionAbbreviationChangeAllowed);
        buf.writeBoolean(allianceAbbreviationChangeAllowed);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PacketSyncFactionGuiData msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientPacketHandler.handleSyncFactionGuiData(msg);
        });
    }

    // Keep getter aliases to avoid breaking client code that expects JavaBeans names
    public boolean isInFaction() { return inFaction(); }
    public String getFactionName() { return factionName(); }
    public Map<UUID, MemberRank> getMemberRanks() { return memberRanks(); }
    public Map<UUID, String> getPlayerNames() { return playerNames(); }
    public List<UUID> getFactionInvites() { return factionInvites(); }
    public List<String> getPlayerInvites() { return playerInvites(); }
    public Map<String, RelationshipStatus> getOutgoingFactionRelationships() { return outgoingFactionRelationships(); }
    public List<String> getFactionNames() { return factionNames(); }
    public String getAllianceName() { return allianceName(); }
    public List<String> getAllianceMembers() { return allianceMembers(); }
    public List<String> getAllianceInvites() { return allianceInvites(); }
    public List<String> getAllianceNames() { return allianceNames(); }
    public Map<String, RelationshipStatus> getOutgoingAllianceRelations() { return outgoingAllianceRelations(); }
    public Map<String, RelationshipStatus> getIncomingAllianceRelations() { return incomingAllianceRelations(); }
    public boolean isFriendlyFire() { return friendlyFire(); }
    public int getFactionColor() { return factionColor(); }
    public int getAllianceColor() { return allianceColor(); }
    public int getFactionAbbreviationMaxLength() { return factionAbbreviationMaxLength(); }
    public int getAllianceAbbreviationMaxLength() { return allianceAbbreviationMaxLength(); }
    public boolean isFactionAbbreviationChangeAllowed() { return factionAbbreviationChangeAllowed(); }
    public boolean isAllianceAbbreviationChangeAllowed() { return allianceAbbreviationChangeAllowed(); }
}