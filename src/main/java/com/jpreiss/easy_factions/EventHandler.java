package com.jpreiss.easy_factions;

import com.jpreiss.easy_factions.alliance.Alliance;
import com.jpreiss.easy_factions.alliance.AllianceStateManager;
import com.jpreiss.easy_factions.faction.Faction;
import com.jpreiss.easy_factions.faction.FactionStateManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber
public class EventHandler {

    /**
     * Prevents damage when friendly fire is enabled
     * @param event The damage event
     */
    @SubscribeEvent
    public static void preventFriendlyFire(LivingAttackEvent event) {
        UUID attackerUUID = getPlayerOrOwnerUUID(event.getSource().getEntity());
        UUID victimUUID = getPlayerOrOwnerUUID(event.getEntity());

        FactionStateManager factionStateManager = FactionStateManager.get();
        Faction victimFac = factionStateManager.getFactionByPlayer(victimUUID);
        Faction attackerFac = factionStateManager.getFactionByPlayer(attackerUUID);

        if (victimFac == null || attackerFac == null) return;

        if (victimFac == attackerFac) {
            if (!victimFac.getFriendlyFire()) {
                event.setCanceled(true);
            }
            return;
        }

        AllianceStateManager allianceStateManager = AllianceStateManager.get();
        Alliance attackerAlliance = allianceStateManager.getAllianceByFaction(attackerFac.getName());
        Alliance victimAlliance = allianceStateManager.getAllianceByFaction(victimFac.getName());

        if (attackerAlliance == null || victimAlliance == null) return;

        if (attackerAlliance == victimAlliance && (!attackerFac.getFriendlyFire() || !victimFac.getFriendlyFire())) {
            event.setCanceled(true);
        }
    }

    /**
     * Method to get the UUID of the player, or if it's a pet of its owner
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
