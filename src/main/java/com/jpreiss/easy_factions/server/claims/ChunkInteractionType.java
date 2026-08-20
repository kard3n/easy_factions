package com.jpreiss.easy_factions.server.claims;

/**
 * Holds the type of interactions inside the world this mod can restrict
 */
public enum ChunkInteractionType {
    BREAK_BLOCK,
    PLACE_BLOCK,
    RIGHT_CLICK_BLOCK,
    LEFT_CLICK_BLOCK,
    RIGHT_CLICK_ITEM,
    INTERACT_ENTITY,
    MOB_GRIEFING_DAMAGE,
    EXPLOSION_DAMAGE,
    PISTON_MOVE,
    PLAYER_ATTACK
}

