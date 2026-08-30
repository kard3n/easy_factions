package com.jpreiss.easy_factions.server.event_handlers;

import com.jpreiss.easy_factions.EasyFactions;
import com.jpreiss.easy_factions.Utils;
import com.jpreiss.easy_factions.network.NetworkManager;
import com.jpreiss.easy_factions.server.ServerConfig;
import com.jpreiss.easy_factions.server.alliance.Alliance;
import com.jpreiss.easy_factions.server.alliance.AllianceStateManager;
import com.jpreiss.easy_factions.server.api.events.FactionChangeColorEvent;
import com.jpreiss.easy_factions.server.api.events.FactionDisbandEvent;
import com.jpreiss.easy_factions.server.claims.ChunkInteractionType;
import com.jpreiss.easy_factions.server.claims.ClaimManager;
import com.jpreiss.easy_factions.server.claims.model.ClaimData;
import com.jpreiss.easy_factions.server.faction.Faction;
import com.jpreiss.easy_factions.server.faction.FactionStateManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

@Mod.EventBusSubscriber(modid = EasyFactions.MODID)
public class ClaimEventHandler {
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
            HashMap<ResourceLocation, HashMap<Long, Integer>> claims = new HashMap<>();

            ClaimManager claimManager = ClaimManager.get(player.getServer());
            for (Map.Entry<ResourceKey<Level>, Map<Long, ClaimData>> entry : claimManager.getClaimMap().entrySet()) {
                HashMap<Long, Integer> currentDimClaims = new HashMap<>();
                for (Map.Entry<Long, ClaimData> entry2 : entry.getValue().entrySet()) {
                    currentDimClaims.put(entry2.getKey(), entry2.getValue().color);
                }
                claims.put(entry.getKey().location(), currentDimClaims);
            }
            NetworkManager.sendClaimsToPlayer(claims, player);
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        UUID victimUUID;
        UUID killerUUID;

        // The amount of conquest points that should be rewarded for this kill
        int pointsForKill;

        if(event.getSource().getEntity() instanceof ServerPlayer serverPlayer) {
            killerUUID = serverPlayer.getUUID();
        }
        else if(event.getSource().getEntity() instanceof OwnableEntity ownableEntity) {
            if(!ServerConfig.countKillsByOwnableEntities) return; // Kill doesn't count
            killerUUID = ownableEntity.getOwnerUUID();
        }
        else return;


        if(event.getEntity() instanceof ServerPlayer serverPlayer) {
            victimUUID = serverPlayer.getUUID();
            pointsForKill = ServerConfig.pointsPerKill;
        }
        else if(event.getEntity() instanceof OwnableEntity ownableEntity) {
            victimUUID = ownableEntity.getOwnerUUID();
            if(!ServerConfig.CONSIDER_OFFLINE_OWNER_OWNABLE_ENTITY_KILLS.get() && !Utils.isPlayerOnline(event.getEntity().getServer(), victimUUID)) return;
            pointsForKill = ServerConfig.pointsPerKilledOwnableEntity.get(event.getEntity().getType());
        }
        else return;

        MinecraftServer server = event.getEntity().getServer();
        if (server == null) return;

        FactionStateManager factionManager = FactionStateManager.get(server);
        AllianceStateManager allianceManager = AllianceStateManager.get(server);
        ClaimManager claimManager = ClaimManager.get(server);

        Faction victimFaction = factionManager.getFactionByPlayer(victimUUID);
        Faction killerFaction = factionManager.getFactionByPlayer(killerUUID);

        // Check that both players are in factions (and not in the same)
        if (victimFaction == null || killerFaction == null) return;
        if (victimFaction.getName().equals(killerFaction.getName())) return;

        // Check that the players are not in the same alliance
        Alliance alliance = allianceManager.getAllianceByFaction(killerFaction.getName());
        if (alliance != null && alliance.getMembers().contains(victimFaction.getName())) return;

        // Add points
        claimManager.addKillPoints(killerFaction.getName(), victimFaction.getName(), pointsForKill);
        claimManager.evaluateKillPoints(killerFaction, victimFaction, event.getEntity().level().dimension(), server);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!playerHasPermission(event.getPlayer(), event.getPos(), event.getPlayer().level().dimension(), ChunkInteractionType.BREAK_BLOCK, event.getLevel().getServer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (!playerHasPermission(player, event.getPos(), player.level().dimension(), ChunkInteractionType.PLACE_BLOCK, event.getLevel().getServer())) {
                event.setCanceled(true);
            } else if (event.getEntity() instanceof TamableAnimal animal && !animal.isOwnedBy(player)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        MinecraftServer server = event.getEntity().getServer();
        if (server == null) {
            return;
        }

        if (!playerHasPermission(player, event.getPos(), player.level().dimension(), ChunkInteractionType.RIGHT_CLICK_BLOCK, server))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBlockLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        MinecraftServer server = event.getEntity().getServer();
        if (server == null) {
            return;
        }

        if (!playerHasPermission(player, event.getPos(), player.level().dimension(), ChunkInteractionType.LEFT_CLICK_BLOCK, server)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onItemRightClick(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        MinecraftServer server = event.getEntity().getServer();
        if (server == null) return;

        if (!playerHasPermission(player, event.getPos(), player.level().dimension(), ChunkInteractionType.RIGHT_CLICK_ITEM, server))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        MinecraftServer server = event.getEntity().getServer();
        if (server == null) {
            return;
        }

        if (!playerHasPermission(player, event.getPos(), player.level().dimension(), ChunkInteractionType.INTERACT_ENTITY, server)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMobGriefing(EntityMobGriefingEvent event) {
        if (event.getEntity() == null) return;
        MinecraftServer server = event.getEntity().getServer();
        if (server == null) return;
        ClaimManager claimManager = ClaimManager.get(server);
        ChunkPos chunkPos = event.getEntity().chunkPosition();
        ResourceKey<Level> dimension = event.getEntity().level().dimension();

        if (!claimManager.isClaimed(dimension, chunkPos)) return;
        ClaimData claim = claimManager.getClaim(dimension, chunkPos);

        switch (claim.type) {
            case FACTION:
                if (ServerConfig.factionClaimRestrictions.contains(ChunkInteractionType.MOB_GRIEFING_DAMAGE)) {
                    event.setResult(Event.Result.DENY);
                }
                break;
            case CORE:
                if (ServerConfig.coreClaimRestrictions.contains(ChunkInteractionType.MOB_GRIEFING_DAMAGE)) {
                    event.setResult(Event.Result.DENY);
                }
                break;
            case ADMIN:
                if (ServerConfig.adminClaimRestrictions.contains(ChunkInteractionType.MOB_GRIEFING_DAMAGE)) {
                    event.setResult(Event.Result.DENY);
                }
                break;
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (event.getLevel().isClientSide()) return;
        MinecraftServer server = event.getLevel().getServer();
        if (server == null) return;
        ClaimManager claimManager = ClaimManager.get(server);

        event.getAffectedBlocks().removeIf(blockPos -> {
            ChunkPos chunkPos = new ChunkPos(blockPos);
            ResourceKey<Level> dimension = event.getLevel().dimension();
            if (!claimManager.isClaimed(dimension, chunkPos)) return false;
            ClaimData claim = claimManager.getClaim(dimension, chunkPos);

            return switch (claim.type) {
                case FACTION -> ServerConfig.factionClaimRestrictions.contains(ChunkInteractionType.EXPLOSION_DAMAGE);
                case CORE -> ServerConfig.coreClaimRestrictions.contains(ChunkInteractionType.EXPLOSION_DAMAGE);
                case ADMIN -> ServerConfig.adminClaimRestrictions.contains(ChunkInteractionType.EXPLOSION_DAMAGE);
            };
        });
    }

    @SubscribeEvent
    public static void onPistonMove(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof Level level)) return;
        if (event.getLevel().isClientSide()) return;
        MinecraftServer server = event.getLevel().getServer();
        if (server == null) return;
        ClaimManager claimManager = ClaimManager.get(server);
        ChunkPos pistonChunk = new ChunkPos(event.getPos());

        if (!claimManager.isClaimed(level.dimension(), pistonChunk)) return;

        ClaimData claim = claimManager.getClaim(level.dimension(), pistonChunk);

        boolean restricted = switch (claim.type) {
            case FACTION -> ServerConfig.factionClaimRestrictions.contains(ChunkInteractionType.PISTON_MOVE);
            case CORE -> ServerConfig.coreClaimRestrictions.contains(ChunkInteractionType.PISTON_MOVE);
            case ADMIN -> ServerConfig.adminClaimRestrictions.contains(ChunkInteractionType.PISTON_MOVE);
        };

        if (restricted) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBucketUse(FillBucketEvent event) {
        if (event.getLevel().isClientSide()) return;
        MinecraftServer server = event.getEntity().getServer();
        if (server == null) return;

        if (event.getTarget() instanceof BlockHitResult blockHit) {
            BlockPos targetPos = blockHit.getBlockPos();

            if (!playerHasPermission(event.getEntity(), targetPos, event.getLevel().dimension(), ChunkInteractionType.USE_BUCKET, server)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityAttacked(LivingAttackEvent event) {
        MinecraftServer server = event.getEntity().getServer();
        if (server == null) return;
        ClaimManager claimManager = ClaimManager.get(server);

        if(event.getSource().getEntity() instanceof Player player) {
            if (!claimManager.isClaimed(event.getEntity().level().dimension(), event.getEntity().chunkPosition())) return;

            ClaimData claim = claimManager.getClaim(event.getEntity().level().dimension(), event.getEntity().chunkPosition());

            boolean restricted = switch (claim.type) {
                case FACTION -> ServerConfig.factionClaimRestrictions.contains(ChunkInteractionType.PLAYER_ATTACK);
                case CORE -> ServerConfig.coreClaimRestrictions.contains(ChunkInteractionType.PLAYER_ATTACK);
                case ADMIN -> ServerConfig.adminClaimRestrictions.contains(ChunkInteractionType.PLAYER_ATTACK);
            };

            if (restricted) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onFactionColorChange(FactionChangeColorEvent event) {
        var server = ServerLifecycleHooks.getCurrentServer();
        ClaimManager.get(server).changeFactionColor(event.getFaction().getName(), event.getFaction().getColor(), server);
    }

    @SubscribeEvent
    public static void onFactionDisband(FactionDisbandEvent event) {
        var server = ServerLifecycleHooks.getCurrentServer();
        ClaimManager.get(server).deleteFactionData(event.getFaction().getName());
    }

    /**
     * Returns true if the player is a member of the region the block is in for the current block
     */
    public static boolean playerHasPermission(Player player, BlockPos pos, ResourceKey<Level> dimension, ChunkInteractionType type, MinecraftServer server) {
        return playerHasPermission(player, new ChunkPos(pos), dimension, type, server);
    }

    /**
     * Returns true if the player is a member of the region the block is in for the current block
     */
    public static boolean playerHasPermission(Player player, ChunkPos pos, ResourceKey<Level> dimension, ChunkInteractionType type, MinecraftServer server) {
        ClaimManager claimManager = ClaimManager.get(server);
        if (!claimManager.isClaimed(dimension, pos)) return true;

        ClaimData claim = claimManager.getClaim(dimension, pos);

        if (player.hasPermissions(2)) return true;

        switch (claim.type) {
            case FACTION:
                if (!ServerConfig.factionClaimRestrictions.contains(type)) return true;
                FactionStateManager factionManager = FactionStateManager.get(server);
                Faction faction = factionManager.getFactionByPlayer(player.getUUID());
                if (faction == null) break;
                return faction.getName().equals(claim.owner);
            case CORE:
                if (!ServerConfig.coreClaimRestrictions.contains(type)) return true;
                return claim.owner.equals(player.getUUID().toString());
            case ADMIN:
                return (!ServerConfig.adminClaimRestrictions.contains(type));
        }

        return false;
    }
}
