package com.jpreiss.easy_factions;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class Utils {
    public static void refreshCommandTree(ServerPlayer player) {
        if (player != null) {
            player.server.getCommands().sendCommands(player);
        }
    }

    /**
     * Gets the name of a player given his UUID. Works with offline players
     * @param uuid UUID of the player
     * @param server MinecraftServer
     * @return The player's name
     * @throws RuntimeException If the UUID doesn't match any player
     */
    public static String getPlayerNameOffline(UUID uuid, MinecraftServer server) throws RuntimeException {
        Optional<GameProfile> profile = Objects.requireNonNull(server.getProfileCache()).get(uuid);

        if (profile.isPresent()) {
            return profile.get().getName();
        }
        else throw new RuntimeException("Player not found!");
    }

    /**
     * Gets the UUID of a player given his name. Works with offline players
     * @param name Name of the player
     * @param server MinecraftServer
     * @return The player's UUID
     * @throws RuntimeException If the UUID doesn't match any player
     */
    public static UUID getPlayerUUIDOffline(String name, MinecraftServer server) throws RuntimeException {
        Optional<GameProfile> profileOpt = Objects.requireNonNull(server.getProfileCache()).get(name);

        if (profileOpt.isEmpty()) {
            throw new RuntimeException("Player not found!");
        }

        return profileOpt.get().getId();
    }
}
