package com.jpreiss.easy_factions.client;

import net.neoforged.neoforge.common.ModConfigSpec;


public class ClientConfig
{
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();


    public static final ModConfigSpec.BooleanValue SHOW_FACTION_ABBREVIATION = BUILDER
            .comment("Show the faction abbreviation (if available) instead of the name in the tag above player heads.")
            .define("showFactionAbbreviation", false);

    public static final ModConfigSpec.BooleanValue SHOW_ALLIANCE_ABBREVIATION = BUILDER
            .comment("Show the alliance abbreviation (if available) instead of the name in the tag above player heads.")
            .define("showAllianceAbbreviation", true);

    public static final ModConfigSpec.DoubleValue CHUNK_BORDER_WIDTH = BUILDER
            .comment("The thickness of the stroke around claimed chunks")
            .defineInRange("chunkBorderWidth", 1.5, 0.0, 2.0);

    public static final ModConfigSpec.DoubleValue CHUNK_BORDER_OPACITY = BUILDER
            .comment("The opacity of the stroke around claimed chunks")
            .defineInRange("chunkBorderOpacity", 0.0, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue CHUNK_OVERLAY_OPACITY = BUILDER
            .comment("The opacity of the color above claimed chunks")
            .defineInRange("chunkOverlayOpacity", 0.25, 0.0, 1.0);

    public static final ModConfigSpec.IntValue CLAIM_MERGE_GRID_SIZE = BUILDER
            .comment("The grid size (in chunks) used to split massive claims on the map.",
                    "Lower values (e.g. 16) prevent large claims from disappearing on the minimap.",
                    "Higher values (e.g. 64) increase performance.")
            .defineInRange("claimMergeGridSize", 32, 1, 256);




    public static final ModConfigSpec SPEC = BUILDER.build();

    public static void setShowFactionAbbreviation(boolean value) {
        SHOW_FACTION_ABBREVIATION.set(value);
    }

    public static void setShowAllianceAbbreviation(boolean value) {
        SHOW_ALLIANCE_ABBREVIATION.set(value);
    }
}
