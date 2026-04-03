package com.jpreiss.easy_factions.server;

import com.jpreiss.easy_factions.EasyFactions;
import com.jpreiss.easy_factions.server.claims.ChunkInteractionType;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    private static final ForgeConfigSpec.IntValue COST_PER_CHUNK = BUILDER
            .comment("Cost in points per chunk")
            .defineInRange("chunkCost", 1, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue CORE_CHUNK_AMOUNT = BUILDER
            .comment("How many core chunks players should be allowed to own.")
            .defineInRange("coreChunks", 9, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.BooleanValue REFUND_COST_UNCLAIM = BUILDER
            .comment("If set to true, points are refunded when a chunk is unclaimed or set as an admin chunk")
            .define("refundCostUnclaim", false);

    private static final ForgeConfigSpec.IntValue POINTS_PER_KILL = BUILDER
            .comment("Points gained against a faction per kill.")
            .comment("To unclaim one chunk, chunkCost points are used and pointsPerStolenChunk are giving to the killing faction.")
            .defineInRange("pointsPerKill", 1, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue POINTS_PER_STOLEN_CHUNK = BUILDER
            .comment("How many claim points are given to a faction for taking a chunk from another faction")
            .defineInRange("pointsPerStolenChunk", 1, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue POINT_GENERATION_INTERVAL = BUILDER
            .comment("The interval in seconds in which factions are given points to be used for claiming chunks")
            .comment("After every interval, a point is given for every online member of the faction.")
            .defineInRange("pointGenerationInterval", 600, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue POINT_GENERATION_AMOUNT = BUILDER
            .comment("How many points are given per interval")
            .defineInRange("pointGenerationAmount", 1, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue ADMIN_CLAIM_COLOR = BUILDER
            .comment("The color of admin claims on the map")
            .defineInRange("adminColor", 0xFF00FF, 0, 0xFFFFFF);

    private static final ForgeConfigSpec.IntValue CORE_CLAIM_COLOR = BUILDER
            .comment("The color of core claims on the map")
            .defineInRange("coreColor", 0xFFFFFF, 0, 0xFFFFFF);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ADMIN_CLAIM_RESTRICTIONS = BUILDER
            .comment("The restrictions set for non-members in admin-claimed chunks")
            .comment("Possible values: BREAK_BLOCK, PLACE_BLOCK, INTERACT_BLOCK, RIGHT_CLICK_ITEM")
            .defineListAllowEmpty("adminClaimRestrictions", List.of("BREAK_BLOCK", "PLACE_BLOCK", "INTERACT_BLOCK"), ServerConfig::validateRestriction);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> CORE_CLAIM_RESTRICTIONS = BUILDER
            .comment("The restrictions set for non-members in core-claimed chunks")
            .comment("Possible values: BREAK_BLOCK, PLACE_BLOCK, INTERACT_BLOCK, RIGHT_CLICK_ITEM")
            .defineListAllowEmpty("coreClaimRestrictions", List.of("BREAK_BLOCK", "PLACE_BLOCK", "INTERACT_BLOCK"), ServerConfig::validateRestriction);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> FACTION_CLAIM_RESTRICTIONS = BUILDER
            .comment("The restrictions set for non-members in faction-claimed chunks")
            .comment("Possible values: BREAK_BLOCK, PLACE_BLOCK, INTERACT_BLOCK, RIGHT_CLICK_ITEM")
            .defineListAllowEmpty("factionClaimRestrictions", List.of("BREAK_BLOCK", "PLACE_BLOCK", "INTERACT_BLOCK"), ServerConfig::validateRestriction);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> CORE_CLAIM_DIMENSIONS = BUILDER
            .comment("The dimensions allowed for core (player) claims.")
            .defineListAllowEmpty("coreClaimDimensions", List.of("minecraft:overworld"), o -> o instanceof String);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> FACTION_CLAIM_DIMENSIONS = BUILDER
            .comment("The dimensions allowed for faction claims.")
            .defineListAllowEmpty("factionClaimDimensions", List.of("minecraft:overworld"), o -> o instanceof String);

    private static final ForgeConfigSpec.IntValue MAX_CHUNKS_PER_PACKET = BUILDER
            .comment("How many chunks should be sent per claim update packet.")
            .comment("Set to a higher amount if mods such as XLPackets are installed.")
            .defineInRange("maxChunksPerPacket", 1000, 10, Integer.MAX_VALUE);

    private static boolean validateRestriction(Object object) {
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
    public static int chunkCost;
    public static int coreChunkAmount;
    public static boolean refundCostUnclaim;
    public static int pointsPerKill;
    public static int pointsPerStolenChunk;
    public static int pointGenerationInterval;
    public static int pointGenerationAmount;
    public static int adminClaimColor;
    public static int coreClaimColor;
    public static Set<ChunkInteractionType> adminClaimRestrictions;
    public static Set<ChunkInteractionType> coreClaimRestrictions;
    public static Set<ChunkInteractionType> factionClaimRestrictions;
    public static Set<String> coreClaimDimensions;
    public static Set<String> factionClaimDimensions;
    public static int maxChunksPerPacket;


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
        chunkCost = COST_PER_CHUNK.get();
        coreChunkAmount = CORE_CHUNK_AMOUNT.get();
        refundCostUnclaim = REFUND_COST_UNCLAIM.get();
        pointsPerKill = POINTS_PER_KILL.get();
        pointsPerStolenChunk = POINTS_PER_STOLEN_CHUNK.get();
        pointGenerationInterval = POINT_GENERATION_INTERVAL.get();
        pointGenerationAmount = POINT_GENERATION_AMOUNT.get();
        adminClaimColor = ADMIN_CLAIM_COLOR.get();
        coreClaimColor = CORE_CLAIM_COLOR.get();
        adminClaimRestrictions = ADMIN_CLAIM_RESTRICTIONS.get().stream().map(ChunkInteractionType::valueOf).collect(Collectors.toSet());
        coreClaimRestrictions = CORE_CLAIM_RESTRICTIONS.get().stream().map(ChunkInteractionType::valueOf).collect(Collectors.toSet());
        factionClaimRestrictions = FACTION_CLAIM_RESTRICTIONS.get().stream().map(ChunkInteractionType::valueOf).collect(Collectors.toSet());
        coreClaimDimensions = new HashSet<>(CORE_CLAIM_DIMENSIONS.get());
        factionClaimDimensions = new HashSet<>(FACTION_CLAIM_DIMENSIONS.get());
        maxChunksPerPacket = MAX_CHUNKS_PER_PACKET.get();
    }
}
