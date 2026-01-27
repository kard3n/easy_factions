package com.jpreiss.easy_factions.client;

import com.jpreiss.easy_factions.client.gui.ErrorPopupScreen;
import com.jpreiss.easy_factions.client.gui.FactionScreen;
import com.jpreiss.easy_factions.client.gui.NoFactionScreen;
import com.jpreiss.easy_factions.network.packet.gui.PacketOpenErrorPopup;
import com.jpreiss.easy_factions.network.packet.gui.PacketSyncFactionGuiData;
import net.minecraft.client.Minecraft;

public class ClientPacketHandler {
    // Handles opening PacketSyncFactionGuiData on the clientside. Prevents import of screen class on serverside
    public static void handleSyncFactionGuiData(PacketSyncFactionGuiData msg) {
        if (msg.isInFaction()) {
            Minecraft.getInstance().setScreen(new FactionScreen(msg));
        } else {
            Minecraft.getInstance().setScreen(new NoFactionScreen(msg.getPlayerInvites()));
        }
    }

    public static void handleOpenErrorPopup(PacketOpenErrorPopup msg) {
        Minecraft.getInstance().setScreen(new ErrorPopupScreen(Minecraft.getInstance().screen, msg.getErrorMessage()));
    }
}
