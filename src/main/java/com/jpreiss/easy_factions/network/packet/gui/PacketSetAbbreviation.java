package com.jpreiss.easy_factions.network.packet.gui;

import com.jpreiss.easy_factions.common.RelationshipStatus;
import com.jpreiss.easy_factions.network.NetworkHandler;
import com.jpreiss.easy_factions.server.ServerConfig;
import com.jpreiss.easy_factions.server.alliance.Alliance;
import com.jpreiss.easy_factions.server.alliance.AllianceStateManager;
import com.jpreiss.easy_factions.server.faction.Faction;
import com.jpreiss.easy_factions.server.faction.FactionStateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> Server packet for setting the abbreviation of a faction or alliance
 */
public class PacketSetAbbreviation {
    private final String abbreviation;
    private final boolean isAlliance;

    public PacketSetAbbreviation(String abbreviation, boolean isAlliance) {
        this.abbreviation = abbreviation;
        this.isAlliance = isAlliance;
    }

    public static void encode(PacketSetAbbreviation msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.abbreviation);
        buf.writeBoolean(msg.isAlliance);
    }

    public static PacketSetAbbreviation decode(FriendlyByteBuf buf) {
        return new PacketSetAbbreviation(buf.readUtf(), buf.readBoolean());
    }

    public static void handle(PacketSetAbbreviation msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            MinecraftServer server = player.getServer();
            if (server == null) return;

            FactionStateManager factionManager = FactionStateManager.get(server);
            AllianceStateManager allianceManager = AllianceStateManager.get(server);

            try {
                factionManager.playerOwnsFaction(player.getUUID());
                if(!msg.isAlliance){
                    Faction faction = factionManager.getFactionByPlayer(player.getUUID());
                    boolean factionAllowAbbreviationChange = ServerConfig.enableAbbreviation && (ServerConfig.allowAbbreviationChange || faction.getAbbreviation() == null);
                    if(!factionAllowAbbreviationChange){
                        throw  new RuntimeException("FORBIDDEN");
                    }

                    factionManager.setAbbreviation(faction.getName(), msg.abbreviation, server);
                }
                else {
                    Faction playerFaction = factionManager.getFactionByPlayer(player.getUUID());
                    Alliance alliance = allianceManager.getAllianceByFaction(playerFaction.getName());

                    if (alliance == null){
                        throw new RuntimeException("You are not in an alliance.");
                    }
                    boolean allianceAllowAbbreviationChange = ServerConfig.enableAbbreviation && (ServerConfig.allowAbbreviationChange || alliance.getAbbreviation() == null);
                    if(!allianceAllowAbbreviationChange){
                        throw  new RuntimeException("FORBIDDEN");
                    }
                    allianceManager.setAbbreviation(alliance.getName(), msg.abbreviation, server);
                }
                // If successful, re-open/refresh the GUI
                PacketOpenFactionGui.handle(new PacketOpenFactionGui(), ctx);
            } catch (Exception e) {
                NetworkHandler.sendToPlayer(new PacketOpenErrorPopup(e.getMessage()), player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
