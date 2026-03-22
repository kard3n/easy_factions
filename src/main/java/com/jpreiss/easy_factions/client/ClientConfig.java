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

    private static final ForgeConfigSpec.DoubleValue CHUNK_BORDER_WIDTH = BUILDER
            .comment("The thickness of the stroke around claimed chunks")
            .defineInRange("chunkBorderWidth", 1.5, 0.0, 2.0);

    private static final ForgeConfigSpec.DoubleValue CHUNK_BORDER_OPACITY = BUILDER
            .comment("The opacity of the stroke around claimed chunks")
            .defineInRange("chunkBorderOpacity", 0.0, 0.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue CHUNK_OVERLAY_OPACITY = BUILDER
            .comment("The opacity of the color above claimed chunks")
            .defineInRange("chunkOverlayOpacity", 0.25, 0.0, 1.0);

    private static final ForgeConfigSpec.IntValue CLAIM_MERGE_GRID_SIZE = BUILDER
            .comment("The grid size (in chunks) used to split massive claims on the map.",
                    "Lower values (e.g. 16) prevent large claims from disappearing on the minimap.",
                    "Higher values (e.g. 64) increase performance.")
            .defineInRange("claimMergeGridSize", 32, 1, 256);




    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean showFactionAbbreviation;
    public static boolean showAllianceAbbreviation;
    public static float chunkBorderWidth;
    public static float chunkBorderOpacity;
    public static float chunkOverlayOpacity;
    public static int claimMergeGridSize;


    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        showFactionAbbreviation = SHOW_FACTION_ABBREVIATION.get();
        showAllianceAbbreviation = SHOW_ALLIANCE_ABBREVIATION.get();
        chunkBorderWidth = CHUNK_BORDER_WIDTH.get().floatValue();
        chunkBorderOpacity = CHUNK_BORDER_OPACITY.get().floatValue();
        chunkOverlayOpacity = CHUNK_OVERLAY_OPACITY.get().floatValue();
        claimMergeGridSize = CLAIM_MERGE_GRID_SIZE.get();
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
