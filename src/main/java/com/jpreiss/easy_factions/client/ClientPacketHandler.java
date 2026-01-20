package com.jpreiss.easy_factions.client;

import com.jpreiss.easy_factions.client.gui.FactionScreen;
import com.jpreiss.easy_factions.client.gui.NoFactionScreen;
import com.jpreiss.easy_factions.network.packet.gui.PacketSyncFactionGuiData;
import net.minecraft.client.Minecraft;

public class ClientPacketHandler {
    // Handles opening PacketSyncFactionGuiData on the clientside. Prevents import of screen class on serverside
    public static void handleSyncFactionGuiData(PacketSyncFactionGuiData msg) {
        if (msg.isInFaction()) {
            Minecraft.getInstance().setScreen(new FactionScreen(
                    msg.getFactionName(),
                    msg.getMemberRanks(),
                    msg.getPlayerNames(),
                    msg.getFactionInvites(),
                    msg.getOutgoingRelationships(),
                    msg.getFactionNames(),
                    msg.getAllianceName(),
                    msg.getAllianceMembers(),
                    msg.getAllianceInvites(),
                    msg.getAllianceNames(),
                    msg.getOutgoingAllianceRelations(),
                    msg.getIncomingAllianceRelations(),
                    msg.isFriendlyFire()
            ));
        } else {
            Minecraft.getInstance().setScreen(new NoFactionScreen(msg.getPlayerInvites()));
        }
    }
}
