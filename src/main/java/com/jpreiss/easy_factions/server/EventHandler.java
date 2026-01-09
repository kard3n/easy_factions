package com.jpreiss.easy_factions.server;

import com.jpreiss.easy_factions.server.alliance.Alliance;
import com.jpreiss.easy_factions.server.alliance.AllianceStateManager;
import com.jpreiss.easy_factions.server.faction.Faction;
import com.jpreiss.easy_factions.server.faction.FactionStateManager;
import com.jpreiss.easy_factions.network.NetworkManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber
public class EventHandler {

    /**
     * Prevents damage when friendly fire is enabled
     *
     * @param event The damage event
     */
    @SubscribeEvent
    public static void preventFriendlyFire(LivingAttackEvent event) {
        if (ServerConfig.forceFriendlyFire) return; // Ignore if friendly fire is forced by config
        UUID attackerUUID = getPlayerOrOwnerUUID(event.getSource().getEntity());
        UUID victimUUID = getPlayerOrOwnerUUID(event.getEntity());


        if (event.getEntity().getServer() == null) return;
        FactionStateManager factionStateManager = FactionStateManager.get(event.getEntity().getServer());
        Faction victimFac = factionStateManager.getFactionByPlayer(victimUUID);
        Faction attackerFac = factionStateManager.getFactionByPlayer(attackerUUID);

        if (victimFac == null || attackerFac == null) return;

        if (victimFac == attackerFac) {
            if (!victimFac.getFriendlyFire()) {
                event.setCanceled(true);
            }
            return;
        }

        if (event.getEntity().getServer() == null) return;

        AllianceStateManager allianceStateManager = AllianceStateManager.get(event.getEntity().getServer());
        Alliance attackerAlliance = allianceStateManager.getAllianceByFaction(attackerFac.getName());
        Alliance victimAlliance = allianceStateManager.getAllianceByFaction(victimFac.getName());

        if (attackerAlliance == null || victimAlliance == null) return;

        if (attackerAlliance == victimAlliance && (!attackerFac.getFriendlyFire() || !victimFac.getFriendlyFire())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
            NetworkManager.updatePlayerAboutOthers(player, player.getServer());
            NetworkManager.broadcastPlayerInfo(player, player.getServer());
            Faction playerFaction = FactionStateManager.get(player.getServer()).getFactionByPlayer(player.getUUID());
            if (playerFaction != null) {
                NetworkManager.broadcastFactionUpdate(playerFaction, player.getServer());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NetworkManager.removeSinglePlayerInfo(player.getUUID(), player.getServer());
        }
    }

    /**
     * Method to get the UUID of the player, or if it's a pet of its owner
     *
     * @param entity The entity to check
     * @return If player its UUID, if a pet the UUID of its owner
     */
    private static UUID getPlayerOrOwnerUUID(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            return player.getUUID();
        }

        if (entity instanceof OwnableEntity pet) {
            return pet.getOwnerUUID();
        }
        return null;
    }
}
