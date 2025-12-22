package com.jpreiss.easy_factions;

import com.jpreiss.easy_factions.alliance.AllianceCommands;
import com.jpreiss.easy_factions.alliance.AllianceStateManager;
import com.jpreiss.easy_factions.faction.FactionCommands;
import com.jpreiss.easy_factions.faction.FactionStateManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
//import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(EasyFactions.MODID)
public class EasyFactions
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "easy_factions";
    // Directly reference a slf4j logger
    //private static final Logger LOGGER = LogUtils.getLogger();

    public EasyFactions(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);


        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);



        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        //LOGGER.info("");
    }


    // Register commands
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        FactionCommands.register(event.getDispatcher());
        AllianceCommands.register(event.getDispatcher());
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Load data
        FactionStateManager.get().load();
        AllianceStateManager.get().load();
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
        }
    }
}
