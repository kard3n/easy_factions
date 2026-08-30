package com.jpreiss.easy_factions.server;

import com.jpreiss.easy_factions.EasyFactions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;

/**
 * Event handler for building the server config cache
 */
@EventBusSubscriber(modid = EasyFactions.MODID)
public class ConfigEventHandler {

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getType() == net.neoforged.fml.config.ModConfig.Type.SERVER) {
            ServerConfig.buildServerConfigCache();
        }
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getType() == net.neoforged.fml.config.ModConfig.Type.SERVER) {
            ServerConfig.buildServerConfigCache();
        }
    }
}