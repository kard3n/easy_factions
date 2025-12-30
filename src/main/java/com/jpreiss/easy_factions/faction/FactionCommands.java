package com.jpreiss.easy_factions.faction;

import com.jpreiss.easy_factions.Utils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

public class FactionCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("faction")
                // Create
                .then(Commands.literal("create")
                        .requires(source -> {
                            try {
                                return !FactionStateManager.get().playerIsInFaction(source.getPlayerOrException().getUUID());
                            } catch (CommandSyntaxException e) {
                                return false;
                            }
                        })
                        .then(Commands.argument("name", StringArgumentType.greedyString()).executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String name = StringArgumentType.getString(ctx, "name");
                            FactionStateManager data = FactionStateManager.get();

                            try {
                                data.createFaction(name, player);
                                ctx.getSource().sendSuccess(() -> Component.literal("Faction \"" + name + "\" created!"), true);
                            } catch (RuntimeException e) {
                                ctx.getSource().sendFailure(Component.literal(e.getMessage()));
                            }
                            return 1;
                        })))

                // Invite
                .then(Commands.literal("invite")
                        .requires(source -> {
                            try {
                                return FactionStateManager.get().playerOwnsFaction(source.getPlayerOrException().getUUID());
                            } catch (CommandSyntaxException e) {
                                return false;
                            }
                        })
                        .then(Commands.argument("target", EntityArgument.player()).suggests(UNINVITED_PLAYERS).executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                            FactionStateManager data = FactionStateManager.get();

                            try {
                                String name = data.invitePlayer(player, target);
                                ctx.getSource().sendSuccess(() -> Component.literal("Invited " + target.getName().getString()), true);
                                //
                                target.sendSystemMessage(Component.literal("You were invited to a faction! Type /faction join <" + name + ">"));
                            } catch (RuntimeException e) {
                                ctx.getSource().sendFailure(Component.literal(e.getMessage()));
                            }
                            return 1;
                        })))

                // Join
                .then(Commands.literal("join")
                        .requires(source -> {
                            try {
                                return !FactionStateManager.get().playerIsInFaction(source.getPlayerOrException().getUUID());
                            } catch (CommandSyntaxException e) {
                                return false;
                            }
                        })
                        .then(Commands.argument("name", StringArgumentType.greedyString()).suggests(FACTIONS_INVITATIONS_SUGGESTION).executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String name = StringArgumentType.getString(ctx, "name");
                            FactionStateManager data = FactionStateManager.get();

                            try {
                                data.joinFaction(player, name);
                                ctx.getSource().sendSuccess(() -> Component.literal("Joined \"" + name + "\"!"), true);
                            } catch (RuntimeException e) {
                                ctx.getSource().sendFailure(Component.literal(e.getMessage()));
                            }

                            return 1;
                        })))

                // Leave
                .then(Commands.literal("leave")
                        .requires(source -> {
                            try {
                                return FactionStateManager.get().playerIsInFaction(source.getPlayerOrException().getUUID());
                            } catch (CommandSyntaxException e) {
                                return false;
                            }
                        }).executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            FactionStateManager data = FactionStateManager.get();

                            try {
                                data.leaveFaction(player);
                                ctx.getSource().sendSuccess(() -> Component.literal("You left the faction."), true);
                            } catch (RuntimeException e) {
                                ctx.getSource().sendFailure(Component.literal(e.getMessage()));
                            }
                            return 1;
                        }))
                // kick
                .then(Commands.literal("kick")
                        .requires(source -> {
                            try {
                                return FactionStateManager.get().playerOwnsFaction(source.getPlayerOrException().getUUID());
                            } catch (CommandSyntaxException e) {
                                return false;
                            }
                        })
                        .then(Commands.argument("member", StringArgumentType.word()).suggests(FACTION_MEMBERS)
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String targetName = StringArgumentType.getString(ctx, "member");
                                    FactionStateManager data = FactionStateManager.get();

                                    try {
                                        data.kickFromFaction(player, targetName, ctx.getSource().getServer());
                                        ctx.getSource().sendSuccess(() -> Component.literal("Kicked " + targetName), true);
                                    } catch (RuntimeException e) {
                                        ctx.getSource().sendFailure(Component.literal(e.getMessage()));
                                    }
                                    return 1;
                                })))

                // Friendly Fire
                .then(Commands.literal("friendlyFire")
                        .requires(source -> {
                            try {
                                return FactionStateManager.get().playerOwnsFaction(source.getPlayerOrException().getUUID());
                            } catch (CommandSyntaxException e) {
                                return false;
                            }
                        })
                        .then(Commands.argument("enabled", BoolArgumentType.bool()).executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            boolean newFFState = BoolArgumentType.getBool(ctx, "enabled");
                            FactionStateManager data = FactionStateManager.get();

                            try {
                                data.setFriendlyFire(player, newFFState);
                                String status = newFFState ? "enabled" : "disabled";
                                ctx.getSource().sendSuccess(() -> Component.literal("Friendly fire is now " + status + "."), true);
                            } catch (RuntimeException e) {
                                ctx.getSource().sendFailure(Component.literal(e.getMessage()));
                            }

                            return 1;

                        })))

                // Faction info
                .then(Commands.literal("about")
                        .requires(source -> {
                            try {
                                return FactionStateManager.get().playerIsInFaction(source.getPlayerOrException().getUUID());
                            } catch (CommandSyntaxException e) {
                                return false;
                            }
                        }).executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            Faction playerFaction = FactionStateManager.get().getFactionByPlayer(player.getUUID());

                            if (playerFaction == null) {
                                ctx.getSource().sendSuccess(() -> Component.literal("You are currently not in a faction."), true);
                            } else {
                                StringBuilder builder = new StringBuilder();
                                builder.append("You are a member of \"").append(playerFaction.getName()).append("\"\nYour members are: ");
                                for(UUID member : playerFaction.getMembers()) {
                                    builder.append(Utils.getPlayerNameOffline(member, ctx.getSource().getServer())).append(" ");
                                }
                                ctx.getSource().sendSuccess(() -> Component.literal(builder.toString()), true);
                            }

                            return 1;
                        }))

        );
    }

    /**
     * Suggestions for joining a faction. Suggests the factions a user is invited to.
     */
    private static final SuggestionProvider<CommandSourceStack> FACTIONS_INVITATIONS_SUGGESTION = (context, builder) -> {
        ServerPlayer player = context.getSource().getPlayerOrException();
        FactionStateManager data = FactionStateManager.get();

        List<String> invites = data.getInvitesForPlayer(player.getUUID());

        for (String factionName : invites) {
            builder.suggest(factionName);
        }

        return builder.buildFuture();
    };

    /**
     * Suggestion provider for players that are possible to invite to the faction.
     * Recommends all players that are not part of the faction and have not been invited yet.
     */
    private static final SuggestionProvider<CommandSourceStack> UNINVITED_PLAYERS = (context, builder) -> {
        ServerPlayer leader = context.getSource().getPlayerOrException();
        FactionStateManager data = FactionStateManager.get();
        Faction leaderFaction = data.getFactionByPlayer(leader.getUUID());


        for (ServerPlayer p : context.getSource().getServer().getPlayerList().getPlayers()) {
            // Skip user executing the command
            if (p.getUUID().equals(leader.getUUID())) continue;

            // Skip players who are already members of the faction
            if (leaderFaction != null && leaderFaction.getMembers().contains(p.getUUID())) {
                continue;
            }

            // Skip those already invited
            if (leaderFaction != null && leaderFaction.getInvited().contains(p.getUUID())) continue;

            builder.suggest(p.getName().getString());
        }
        return builder.buildFuture();
    };

    /**
     * Suggests all players that are inside the faction of the leader, except its leader
     */
    private static final SuggestionProvider<CommandSourceStack> FACTION_MEMBERS = (context, builder) -> {
        ServerPlayer leader = context.getSource().getPlayerOrException();
        FactionStateManager data = FactionStateManager.get();
        Faction leaderFaction = data.getFactionByPlayer(leader.getUUID());

        for(UUID memberUUID : leaderFaction.getMembers()) {
            if (memberUUID.equals(leader.getUUID())) continue;

            builder.suggest(Utils.getPlayerNameOffline(memberUUID, context.getSource().getServer()));
        }

        return builder.buildFuture();
    };
}