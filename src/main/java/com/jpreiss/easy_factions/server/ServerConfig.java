package com.jpreiss.easy_factions.server;

import com.jpreiss.easy_factions.server.claims.ChunkInteractionType;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.List;

public class ServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();


    public static final ModConfigSpec.IntValue MAX_FACTION_SIZE = BUILDER
            .comment("The maximum amount of members a faction can have.")
            .defineInRange("maxFactionSize", 10, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue MAX_ALLIANCE_SIZE = BUILDER
            .comment("The maximum amount of factions an alliances can contain.")
            .defineInRange("maxAllianceSize", 3, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue FORCE_FRIENDLY_FIRE = BUILDER
            .comment("Enables friendly fire, overwriting faction settings.")
            .define("forceFriendlyFire", false);

    public static final ModConfigSpec.IntValue FACTION_ABBREVIATION_MIN_LENGTH = BUILDER
            .comment("Minimum length for faction abbreviations.")
            .defineInRange("factionAbbreviationMinLength", 3, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue FACTION_ABBREVIATION_MAX_LENGTH = BUILDER
            .comment("Maximum length for faction abbreviations.")
            .defineInRange("factionAbbreviationMaxLength", 3, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue ALLIANCE_ABBREVIATION_MIN_LENGTH = BUILDER
            .comment("Minimum length for alliance abbreviations.")
            .defineInRange("allianceAbbreviationMinLength", 3, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue ALLIANCE_ABBREVIATION_MAX_LENGTH = BUILDER
            .comment("Maximum length for alliance abbreviations.")
            .defineInRange("allianceAbbreviationMaxLength", 3, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue ENABLE_ABBREVIATION = BUILDER
            .comment("Allow factions to set an abbreviation")
            .define("enableAbbreviation", true);

    public static final ModConfigSpec.BooleanValue ALLOW_ABBREVIATION_CHANGE = BUILDER
            .comment("Allow factions to change their abbreviation")
            .define("allowAbbreviationChange", false);

    public static final ModConfigSpec.IntValue COST_PER_CHUNK = BUILDER
            .comment("Cost in points per chunk")
            .defineInRange("chunkCost", 1, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue CORE_CHUNK_AMOUNT = BUILDER
            .comment("How many core chunks players should be allowed to own.")
            .defineInRange("coreChunks", 9, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue REFUND_COST_UNCLAIM = BUILDER
            .comment("If set to true, points are refunded when a chunk is unclaimed or set as an admin chunk")
            .define("refundCostUnclaim", false);

    public static final ModConfigSpec.IntValue POINTS_PER_KILL = BUILDER
            .comment("Points gained against a faction per kill.")
            .comment("To unclaim one chunk, chunkCost points are used and pointsPerStolenChunk are giving to the killing faction.")
            .defineInRange("pointsPerKill", 1, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue POINTS_PER_STOLEN_CHUNK = BUILDER
            .comment("How many claim points are given to a faction for taking a chunk from another faction")
            .defineInRange("pointsPerStolenChunk", 1, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue POINT_GENERATION_INTERVAL = BUILDER
            .comment("The interval in seconds in which factions are given points to be used for claiming chunks")
            .comment("After every interval, a point is given for every online member of the faction.")
            .defineInRange("pointGenerationInterval", 600, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue POINT_GENERATION_AMOUNT = BUILDER
            .comment("How many points are given per interval")
            .defineInRange("pointGenerationAmount", 1, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue ADMIN_CLAIM_COLOR = BUILDER
            .comment("The color of admin claims on the map")
            .defineInRange("adminColor", 0xFF00FF, 0, 0xFFFFFF);

    public static final ModConfigSpec.IntValue CORE_CLAIM_COLOR = BUILDER
            .comment("The color of core claims on the map")
            .defineInRange("coreColor", 0xFFFFFF, 0, 0xFFFFFF);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> ADMIN_CLAIM_RESTRICTIONS = BUILDER
            .comment("The restrictions set for non-members in admin-claimed chunks")
            .comment("Possible values: BREAK_BLOCK, PLACE_BLOCK, RIGHT_CLICK_BLOCK, LEFT_CLICK_BLOCK, RIGHT_CLICK_ITEM, INTERACT_ENTITY, MOB_GRIEFING_DAMAGE, EXPLOSION_DAMAGE, PISTON_MOVE, PLAYER_ATTACK")
            .comment("'PLAYER_ATTACK' prevents players from attacking entities.")
            .defineListAllowEmpty("adminClaimRestrictions", List.of("BREAK_BLOCK", "PLACE_BLOCK", "RIGHT_CLICK_BLOCK", "LEFT_CLICK_BLOCK", "RIGHT_CLICK_ITEM", "INTERACT_ENTITY", "PLAYER_ATTACK"), ServerConfig::validateRestriction);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> CORE_CLAIM_RESTRICTIONS = BUILDER
            .comment("The restrictions set for non-members in core-claimed chunks")
            .comment("Possible values: BREAK_BLOCK, PLACE_BLOCK, RIGHT_CLICK_BLOCK, LEFT_CLICK_BLOCK, RIGHT_CLICK_ITEM, INTERACT_ENTITY, MOB_GRIEFING_DAMAGE, EXPLOSION_DAMAGE, PISTON_MOVE, PLAYER_ATTACK")
            .defineListAllowEmpty("coreClaimRestrictions", List.of("BREAK_BLOCK", "PLACE_BLOCK", "RIGHT_CLICK_BLOCK", "LEFT_CLICK_BLOCK", "RIGHT_CLICK_ITEM", "INTERACT_ENTITY"), ServerConfig::validateRestriction);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> FACTION_CLAIM_RESTRICTIONS = BUILDER
            .comment("The restrictions set for non-members in faction-claimed chunks")
            .comment("Possible values: BREAK_BLOCK, PLACE_BLOCK, RIGHT_CLICK_BLOCK, LEFT_CLICK_BLOCK, RIGHT_CLICK_ITEM, INTERACT_ENTITY, MOB_GRIEFING_DAMAGE, EXPLOSION_DAMAGE, PISTON_MOVE, PLAYER_ATTACK")
            .defineListAllowEmpty("factionClaimRestrictions", List.of("BREAK_BLOCK", "PLACE_BLOCK", "RIGHT_CLICK_BLOCK", "LEFT_CLICK_BLOCK", "RIGHT_CLICK_ITEM", "INTERACT_ENTITY"), ServerConfig::validateRestriction);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> CORE_CLAIM_DIMENSIONS = BUILDER
            .comment("The dimensions allowed for core (player) claims.")
            .defineListAllowEmpty("coreClaimDimensions", List.of("minecraft:overworld"), o -> o instanceof String);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> FACTION_CLAIM_DIMENSIONS = BUILDER
            .comment("The dimensions allowed for faction claims.")
            .defineListAllowEmpty("factionClaimDimensions", List.of("minecraft:overworld"), o -> o instanceof String);

    public static final ModConfigSpec.IntValue FACTION_BASE_CLAIM_LIMIT = BUILDER
            .comment("The base amount of chunks a faction can claim")
            .defineInRange("factionBaseClaimLimit", 100, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue FACTION_ADDITIONAL_CLAIM_LIMIT_PER_MEMBER = BUILDER
            .comment("The additional amount of chunks a faction can claim per member.")
            .comment("The final limit of claimable chunks per faction is (factionBaseClaimLimit + factionAdditionalClaimLimitPerMember * factionMembers)")
            .defineInRange("factionAdditionalClaimLimitPerMember", 100, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue MAX_CHUNKS_PER_PACKET = BUILDER
            .comment("How many chunks should be sent per claim update packet.")
            .comment("Set to a higher amount if mods such as XLPackets are installed.")
            .defineInRange("maxChunksPerPacket", 1000, 10, Integer.MAX_VALUE);

    public static boolean validateRestriction(Object object) {
        if (object instanceof String) {
            try {
                ChunkInteractionType.valueOf((String) object);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return false;
    }


    public static final ModConfigSpec SPEC = BUILDER.build();

}
