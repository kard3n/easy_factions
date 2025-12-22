package com.jpreiss.easy_factions;

import net.minecraft.server.level.ServerPlayer;

public class Utils {
    public static void refreshCommandTree(ServerPlayer player) {
        if (player != null) {
            player.server.getCommands().sendCommands(player);
        }
    }
}
