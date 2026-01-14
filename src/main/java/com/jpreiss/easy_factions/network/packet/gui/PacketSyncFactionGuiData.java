package com.jpreiss.easy_factions.network.packet.gui;

import com.jpreiss.easy_factions.client.gui.FactionScreen;
import com.jpreiss.easy_factions.client.gui.NoFactionScreen;
import com.jpreiss.easy_factions.common.MemberRank;
import com.jpreiss.easy_factions.common.RelationshipStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

public class PacketSyncFactionGuiData {
    private final boolean inFaction;
    private final String factionName;
    private final Map<UUID, MemberRank> memberRanks; // UUID -> MemberRank
    private final Map<UUID, String> playerNames; // UUID -> Name for all currently online server players, those invited and those who are members
    private final List<UUID> factionInvites; // UUIDs of all players invited to the player's faction
    private final List<String> playerInvites; // Names of the factions the player is invited to
    private final Map<String, RelationshipStatus> outgoingRelationships; // The relationship this faction set for others
    private final List<String> factionNames; // All known faction names
    private final String allianceName; // All known alliance names
    private final List<String> allianceMembers;
    private final List<String> allianceInvites;
    private final List<String> allianceNames;
    private final Map<String, RelationshipStatus> outgoingAllianceRelations;
    private final Map<String, RelationshipStatus> incomingAllianceRelations;

    // Constructor for when player IS in a faction
    public PacketSyncFactionGuiData(
            String factionName, Map<UUID, MemberRank> memberRanks,
            Map<UUID, String> playerNames,
            List<UUID> factionInvites,
            Map<String, RelationshipStatus> outgoingRelationships,
            List<String> factionNames,
            String allianceName,
            List<String> allianceMembers,
            List<String> allianceInvites,
            List<String> allianceNames,
            Map<String, RelationshipStatus> outgoingAllianceRelations,
            Map<String, RelationshipStatus> incomingAllianceRelations) {
        this.inFaction = true;
        this.factionName = factionName;
        this.memberRanks = memberRanks;
        this.playerNames = playerNames;
        this.factionInvites = factionInvites;
        this.playerInvites = new ArrayList<>();
        this.outgoingRelationships = outgoingRelationships;
        this.factionNames = factionNames;
        this.allianceMembers = allianceMembers;
        this.allianceName = allianceName;
        this.allianceInvites = allianceInvites;
        this.allianceNames = allianceNames;
        this.outgoingAllianceRelations = outgoingAllianceRelations;
        this.incomingAllianceRelations = incomingAllianceRelations;
    }

    // Constructor for when player is NOT in a faction
    public PacketSyncFactionGuiData(List<String> playerInvites) {
        this.inFaction = false;
        this.factionName = "";
        this.memberRanks = new HashMap<>();
        this.playerNames = new HashMap<>();
        this.factionInvites = new ArrayList<>();
        this.playerInvites = playerInvites;
        this.outgoingRelationships = new HashMap<>();
        this.factionNames = new ArrayList<>();
        this.allianceName = null;
        this.allianceMembers = new ArrayList<>();
        this.allianceInvites = new ArrayList<>();
        this.allianceNames = new ArrayList<>();
        this.outgoingAllianceRelations = new HashMap<>();
        this.incomingAllianceRelations = new HashMap<>();
    }

    // Internal constructor for decoding
    public PacketSyncFactionGuiData(
            boolean inFaction,
            String factionName,
            Map<UUID, MemberRank> memberRanks,
            Map<UUID, String> playerNames,
            List<String> playerInvites,
            List<UUID> factionInvites,
            Map<String, RelationshipStatus> outgoingRelationships,
            List<String> factionNames,
            String allianceName,
            List<String> allianceMembers,
            List<String> allianceInvites,
            List<String> allianceNames,
            Map<String, RelationshipStatus> outgoingAllianceRelations,
            Map<String, RelationshipStatus> incomingAllianceRelations
    ) {
        this.inFaction = inFaction;
        this.factionName = factionName;
        this.memberRanks = memberRanks;
        this.playerNames = playerNames;
        this.playerInvites = playerInvites;
        this.factionInvites = factionInvites;
        this.outgoingRelationships = outgoingRelationships;
        this.factionNames = factionNames;
        this.allianceName = allianceName;
        this.allianceMembers = allianceMembers;
        this.allianceInvites = allianceInvites;
        this.allianceNames = allianceNames;
        this.outgoingAllianceRelations = outgoingAllianceRelations;
        this.incomingAllianceRelations = incomingAllianceRelations;
    }

    public static void encode(PacketSyncFactionGuiData msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.inFaction);
        buf.writeUtf(msg.factionName);
        buf.writeMap(msg.memberRanks, FriendlyByteBuf::writeUUID, FriendlyByteBuf::writeEnum);
        buf.writeMap(msg.playerNames, FriendlyByteBuf::writeUUID, FriendlyByteBuf::writeUtf);
        buf.writeCollection(msg.playerInvites, FriendlyByteBuf::writeUtf);
        buf.writeCollection(msg.factionInvites, FriendlyByteBuf::writeUUID);
        buf.writeMap(msg.outgoingRelationships, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeEnum);
        buf.writeCollection(msg.factionNames, FriendlyByteBuf::writeUtf);
        buf.writeUtf(msg.allianceName == null ? "" : msg.allianceName);
        buf.writeCollection(msg.allianceMembers, FriendlyByteBuf::writeUtf);
        buf.writeCollection(msg.allianceInvites, FriendlyByteBuf::writeUtf);
        buf.writeCollection(msg.allianceNames, FriendlyByteBuf::writeUtf);
        buf.writeMap(msg.outgoingAllianceRelations, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeEnum);
        buf.writeMap(msg.incomingAllianceRelations, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeEnum);
    }

    public static PacketSyncFactionGuiData decode(FriendlyByteBuf buf) {
        boolean inFaction = buf.readBoolean();
        String factionName = buf.readUtf();
        Map<UUID, MemberRank> memberRanks = buf.readMap(FriendlyByteBuf::readUUID, b -> b.readEnum(MemberRank.class));
        Map<UUID, String> playerNames = buf.readMap(FriendlyByteBuf::readUUID, FriendlyByteBuf::readUtf);
        List<String> playerInvites = buf.readList(FriendlyByteBuf::readUtf);
        List<UUID> factionInvites = buf.readList(FriendlyByteBuf::readUUID);
        Map<String, RelationshipStatus> outgoingRelationships = buf.readMap(FriendlyByteBuf::readUtf, b -> b.readEnum(RelationshipStatus.class));
        List<String> factionNames = buf.readList(FriendlyByteBuf::readUtf);
        String allianceName = buf.readUtf();
        if (allianceName.isEmpty()) allianceName = null;
        List<String> allianceMembers = buf.readList(FriendlyByteBuf::readUtf);
        List<String> allianceInvites = buf.readList(FriendlyByteBuf::readUtf);
        List<String> allianceNames = buf.readList(FriendlyByteBuf::readUtf);
        Map<String, RelationshipStatus> outgoingAllianceRelations = buf.readMap(FriendlyByteBuf::readUtf, b -> b.readEnum(RelationshipStatus.class));
        Map<String, RelationshipStatus> incomingAllianceRelations = buf.readMap(FriendlyByteBuf::readUtf, b -> b.readEnum(RelationshipStatus.class));

        return new PacketSyncFactionGuiData(
                inFaction,
                factionName,
                memberRanks,
                playerNames,
                playerInvites,
                factionInvites,
                outgoingRelationships,
                factionNames,
                allianceName,
                allianceMembers,
                allianceInvites,
                allianceNames,
                outgoingAllianceRelations,
                incomingAllianceRelations
        );
    }

    public static void handle(PacketSyncFactionGuiData msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (msg.inFaction) {
                Minecraft.getInstance().setScreen(new FactionScreen(msg.factionName, msg.memberRanks, msg.playerNames, msg.factionInvites, msg.outgoingRelationships, msg.factionNames, msg.allianceName, msg.allianceMembers, msg.allianceInvites, msg.allianceNames, msg.outgoingAllianceRelations, msg.incomingAllianceRelations));
            } else {
                Minecraft.getInstance().setScreen(new NoFactionScreen(msg.playerInvites));
            }
        }));
        ctx.get().setPacketHandled(true);
    }
}