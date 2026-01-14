package com.jpreiss.easy_factions.client;

import com.jpreiss.easy_factions.EasyFactions;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = EasyFactions.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientConfig
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();


    private static final ForgeConfigSpec.BooleanValue SHOW_FACTION_ABBREVIATION = BUILDER
            .comment("Show the faction abbreviation (if available) instead of the name in the tag above player heads.")
            .define("showFactionAbbreviation", false);

    private static final ForgeConfigSpec.BooleanValue SHOW_ALLIANCE_ABBREVIATION = BUILDER
            .comment("Show the alliance abbreviation (if available) instead of the name in the tag above player heads.")
            .define("showAllianceAbbreviation", true);




    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean showFactionAbbreviation;
    public static boolean showAllianceAbbreviation;


    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        showFactionAbbreviation = SHOW_FACTION_ABBREVIATION.get();
        showAllianceAbbreviation = SHOW_ALLIANCE_ABBREVIATION.get();
    }

    public static void setShowFactionAbbreviation(boolean value) {
        SHOW_FACTION_ABBREVIATION.set(value);
        showFactionAbbreviation = value;
    }

    public static void setShowAllianceAbbreviation(boolean value) {
        SHOW_ALLIANCE_ABBREVIATION.set(value);
        showAllianceAbbreviation = value;
    }

    public static boolean getShowFactionAbbreviation() {
        return SHOW_FACTION_ABBREVIATION.get();
    }

    public static boolean getShowAllianceAbbreviation() {
        return SHOW_ALLIANCE_ABBREVIATION.get();
    }
}
