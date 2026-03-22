package com.jpreiss.easy_factions.server.claims;

import com.jpreiss.easy_factions.Utils;
import com.jpreiss.easy_factions.server.ServerConfig;
import com.jpreiss.easy_factions.server.claims.model.ClaimData;
import com.jpreiss.easy_factions.server.faction.Faction;
import com.jpreiss.easy_factions.server.faction.FactionStateManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.UUID;

public class ClaimCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("claim")
                // Create
                .then(Commands.literal("pos1")
                        .executes(ctx -> {
                            try {
                                ChunkPos pos = ChunkClaimSelectionManager.setPos1(ctx.getSource().getPlayerOrException());
                                ctx.getSource().sendSuccess(() -> Component.literal("Position 1 set to " + pos), false);
                            } catch (RuntimeException e) {
                                ctx.getSource().sendFailure(Component.literal(e.getMessage()));
                                return 0;
                            }
                            return 1;
                        }))
                .then(Commands.literal("pos2")
                        .executes(ctx -> {
                            try {
                                ChunkPos pos = ChunkClaimSelectionManager.setPos2(ctx.getSource().getPlayerOrException());
                                ctx.getSource().sendSuccess(() -> Component.literal("Position 2 set to " + pos), false);
                                return 1;
                            } catch (RuntimeException e) {
                                ctx.getSource().sendFailure(Component.literal(e.getMessage()));
                                return 0;
                            }

                        }))
                .then(Commands.literal("personal")
                        .executes(ctx -> {
                            try {
                                String result = ChunkClaimSelectionManager.claimChunksCore(ctx.getSource().getPlayerOrException(), ctx.getSource().getServer());
                                ctx.getSource().sendSuccess(() -> Component.literal(result), false);
                            } catch (RuntimeException e) {
                                ctx.getSource().sendFailure(Component.literal(e.getMessage()));
                                return 0;
                            }
                            return 1;
                        }))
                .then(Commands.literal("faction")
                        .requires(source -> {
                            try {
                                return FactionStateManager.get(source.getServer()).playerIsOwnerOrOfficer(source.getPlayerOrException().getUUID());
                            } catch (CommandSyntaxException e) {
                                return false;
                            }
                        })
                        .executes(ctx -> {
                            try {
                                String result = ChunkClaimSelectionManager.claimChunksFaction(ctx.getSource().getPlayerOrException(), ctx.getSource().getServer());
                                ctx.getSource().sendSuccess(() -> Component.literal(result), false);
                            } catch (RuntimeException e) {
                                ctx.getSource().sendFailure(Component.literal(e.getMessage()));
                                return 0;
                            }
                            return 1;

                        }))
                .then(Commands.literal("factionPoints")
                        .requires(source -> {
                            try {
                                return FactionStateManager.get(source.getServer()).playerIsOwnerOrOfficer(source.getPlayerOrException().getUUID());
                            } catch (CommandSyntaxException e) {
                                return false;
                            }
                        })
                        .executes(ctx -> {
                            try {
                                Faction faction = FactionStateManager.get(ctx.getSource().getServer()).getFactionByPlayer(ctx.getSource().getPlayerOrException().getUUID());
                                ctx.getSource().sendSuccess(() -> Component.literal("Your faction has " + ClaimManager.get(ctx.getSource().getServer()).getPoints(faction.getName()) + " points. You can claim " + ClaimManager.get(ctx.getSource().getServer()).getPoints(faction.getName()) / ServerConfig.pointGenerationAmount + " more chunks."), false);
                            } catch (RuntimeException | CommandSyntaxException e) {
                                ctx.getSource().sendFailure(Component.literal(e.getMessage()));
                                return 0;
                            }
                            return 1;

                        }))
                .then(Commands.literal("unclaimFaction")
                        .requires(source -> {
                            try {
                                return FactionStateManager.get(source.getServer()).playerIsOwnerOrOfficer(source.getPlayerOrException().getUUID());
                            } catch (CommandSyntaxException e) {
                                return false;
                            }
                        })
                        .executes(ctx -> {
                            try {
                                int result = ChunkClaimSelectionManager.unclaimChunksFaction(ctx.getSource().getPlayerOrException(), ctx.getSource().getServer());
                                ctx.getSource().sendSuccess(() -> Component.literal("Unclaimed " + result + " chunks."), false);
                            } catch (RuntimeException e) {
                                ctx.getSource().sendFailure(Component.literal(e.getMessage()));
                                return 0;

                            }
                            return 1;
                        }))
                .then(Commands.literal("admin")
                        .requires(source -> source.hasPermission(2)
                        )
                        .executes(ctx -> {
                            try {
                                int result = ChunkClaimSelectionManager.claimChunksAdmin(ctx.getSource().getPlayerOrException(), ctx.getSource().getServer());
                                ctx.getSource().sendSuccess(() -> Component.literal("Claimed " + result + " chunks."), false);
                            } catch (RuntimeException e) {
                                ctx.getSource().sendFailure(Component.literal(e.getMessage()));
                                return 0;
                            }
                            return 1;
                        })
                )
                .then(Commands.literal("unclaimAdmin")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> {
                            try {
                                int result = ChunkClaimSelectionManager.unclaimChunksAdmin(ctx.getSource().getPlayerOrException(), ctx.getSource().getServer());
                                ctx.getSource().sendSuccess(() -> Component.literal("Unclaimed " + result + " chunks."), false);
                            } catch (RuntimeException e) {
                                ctx.getSource().sendFailure(Component.literal(e.getMessage()));
                                return 0;
                            }
                            return 1;
                        }))
                .then(Commands.literal("whereAmI")
                        .executes(ctx -> {
                            try {
                                MinecraftServer server = ctx.getSource().getServer();
                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                ClaimData result = ClaimManager.get(server).getClaim(player.level().dimension(), player.chunkPosition());
                                if (result != null) {
                                    switch (result.type) {
                                        case FACTION:
                                            ctx.getSource().sendSuccess(() -> Component.literal("This chunk is owned by faction " + result.owner), false);
                                            break;
                                        case CORE:
                                            ctx.getSource().sendSuccess(() -> Component.literal("This chunk is owned by player " + Utils.getPlayerNameOffline(UUID.fromString(result.owner), server)), false);
                                            break;
                                        case ADMIN:
                                            ctx.getSource().sendSuccess(() -> Component.literal("This chunk was claimed by an admin."), false);
                                    }
                                    return 1;
                                }
                                ctx.getSource().sendSuccess(() -> Component.literal("This chunk is not claimed."), false);
                                return 1;

                            } catch (RuntimeException e) {
                                ctx.getSource().sendFailure(Component.literal(e.getMessage()));
                                return 0;

                            }
                        })
                )
                .then(Commands.literal("resetUnclaimedChunks")
                        .requires(source -> source.hasPermission(2)
                        )
                        .then(Commands.argument("resetChunks", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    try {
                                        boolean resetChunks = BoolArgumentType.getBool(ctx, "resetChunks");
                                        if (resetChunks) {
                                            ChunkCleaner.scheduleWipe(ctx.getSource().getServer());
                                            ctx.getSource().sendSuccess(() -> Component.literal("Unclaimed chunks will be reset the next restart."), false);
                                        } else {
                                            ChunkCleaner.stopWipe();
                                            ctx.getSource().sendSuccess(() -> Component.literal("Unclaimed chunks will not be reset the next restart."), false);
                                        }

                                    } catch (RuntimeException e) {
                                        ctx.getSource().sendFailure(Component.literal(e.getMessage()));
                                        return 0;
                                    }
                                    return 1;
                                }))

                )
        );
    }
}
