package com.jpreiss.easy_factions.server;

import com.jpreiss.easy_factions.EasyFactions;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
@Mod.EventBusSubscriber(modid = EasyFactions.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ServerConfig
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();


    private static final ForgeConfigSpec.IntValue MAX_FACTION_SIZE = BUILDER
            .comment("The maximum amount of members a faction can have.")
            .defineInRange("maxFactionSize", 10, 1, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue MAX_ALLIANCE_SIZE = BUILDER
            .comment("The maximum amount of factions an alliances can contain.")
            .defineInRange("maxAllianceSize", 3, 1, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.BooleanValue FORCE_FRIENDLY_FIRE = BUILDER
            .comment("Enables friendly fire, overwriting faction settings.")
            .define("forceFriendlyFire", false);


    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int maxAllianceSize;
    public static int maxFactionSize;
    public static boolean forceFriendlyFire;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        maxAllianceSize = MAX_ALLIANCE_SIZE.get();
        maxFactionSize = MAX_FACTION_SIZE.get();
        forceFriendlyFire = FORCE_FRIENDLY_FIRE.get();
    }
}
