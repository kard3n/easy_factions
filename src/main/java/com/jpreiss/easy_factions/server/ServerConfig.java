package com.jpreiss.easy_factions.server;

import com.jpreiss.easy_factions.EasyFactions;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = EasyFactions.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ServerConfig {
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

    private static final ForgeConfigSpec.IntValue FACTION_ABBREVIATION_MIN_LENGTH = BUILDER
            .comment("Minimum length for faction abbreviations.")
            .defineInRange("factionAbbreviationMinLength", 3, 1, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue FACTION_ABBREVIATION_MAX_LENGTH = BUILDER
            .comment("Maximum length for faction abbreviations.")
            .defineInRange("factionAbbreviationMaxLength", 3, 1, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue ALLIANCE_ABBREVIATION_MIN_LENGTH = BUILDER
            .comment("Minimum length for alliance abbreviations.")
            .defineInRange("allianceAbbreviationMinLength", 3, 1, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue ALLIANCE_ABBREVIATION_MAX_LENGTH = BUILDER
            .comment("Maximum length for alliance abbreviations.")
            .defineInRange("allianceAbbreviationMaxLength", 3, 1, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.BooleanValue ENABLE_ABBREVIATION = BUILDER
            .comment("Allow factions to set an abbreviation")
            .define("enableAbbreviation", true);

    private static final ForgeConfigSpec.BooleanValue ALLOW_ABBREVIATION_CHANGE = BUILDER
            .comment("Allow factions to change their abbreviation")
            .define("allowAbbreviationChange", false);



    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int maxAllianceSize;
    public static int maxFactionSize;
    public static boolean forceFriendlyFire;
    public static int factionAbbreviationMinLength;
    public static int factionAbbreviationMaxLength;
    public static int allianceAbbreviationMinLength;
    public static int allianceAbbreviationMaxLength;
    public static boolean enableAbbreviation;
    public static boolean allowAbbreviationChange;


    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        maxAllianceSize = MAX_ALLIANCE_SIZE.get();
        maxFactionSize = MAX_FACTION_SIZE.get();
        forceFriendlyFire = FORCE_FRIENDLY_FIRE.get();
        factionAbbreviationMinLength = FACTION_ABBREVIATION_MIN_LENGTH.get();
        factionAbbreviationMaxLength = FACTION_ABBREVIATION_MAX_LENGTH.get();
        allianceAbbreviationMinLength = ALLIANCE_ABBREVIATION_MIN_LENGTH.get();
        allianceAbbreviationMaxLength = ALLIANCE_ABBREVIATION_MAX_LENGTH.get();
        enableAbbreviation = ENABLE_ABBREVIATION.get();
        allowAbbreviationChange = ALLOW_ABBREVIATION_CHANGE.get();
    }
}
