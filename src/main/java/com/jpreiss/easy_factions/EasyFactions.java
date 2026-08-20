package com.jpreiss.easy_factions;

import com.jpreiss.easy_factions.client.ClientConfig;
import com.jpreiss.easy_factions.server.ServerConfig;
import com.jpreiss.easy_factions.server.alliance.AllianceCommands;
import com.jpreiss.easy_factions.server.claims.ClaimCommands;
import com.jpreiss.easy_factions.server.faction.FactionCommands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.ModContainer;

import static com.jpreiss.easy_factions.client.Keybinds.OPEN_FACTION_GUI;
//import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(EasyFactions.MODID)
public class EasyFactions
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "easy_factions";
    // Directly reference a slf4j logger
    //private static final Logger LOGGER = LogUtils.getLogger();

    public EasyFactions(IEventBus modEventBus, ModContainer modContainer)
    {
        // Register ourselves for server and other game events we are interested in
        NeoForge.EVENT_BUS.register(this);

        // Register our mod's ForgeConfigSpec
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }

    // Register commands
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        FactionCommands.register(event.getDispatcher());
        AllianceCommands.register(event.getDispatcher());
        ClaimCommands.register(event.getDispatcher());
    }

    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
        }

        @SubscribeEvent
        public static void registerBindings(RegisterKeyMappingsEvent event) {
            event.register(OPEN_FACTION_GUI);
        }
    }
}
