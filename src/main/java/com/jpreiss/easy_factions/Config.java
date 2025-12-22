package com.jpreiss.easy_factions;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
@Mod.EventBusSubscriber(modid = EasyFactions.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();


    private static final ForgeConfigSpec.IntValue MAX_ALLIANCE_SIZE = BUILDER
            .comment("The maximum amount of factions an alliances can contain.")
            .defineInRange("maxAllianceSize", 3, 1, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue MAX_FACTION_SIZE = BUILDER
            .comment("The maximum amount of members a faction can have.")
            .defineInRange("maxAllianceSize", 10, 1, Integer.MAX_VALUE);


    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int maxAllianceSize;
    public static int maxFactionSize;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        maxAllianceSize = MAX_ALLIANCE_SIZE.get();
        maxFactionSize = MAX_FACTION_SIZE.get();
    }
}
