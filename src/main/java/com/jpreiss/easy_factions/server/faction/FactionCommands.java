package com.jpreiss.easy_factions.server.faction;

import com.jpreiss.easy_factions.server.ServerConfig;
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
                                return !FactionStateManager.get(source.getServer()).playerIsInFaction(source.getPlayerOrException().getUUID());
                            } catch (CommandSyntaxException e) {
                                return false;
                            }
                        })
                        .then(Commands.argument("name", StringArgumentType.greedyString()).executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            String name = StringArgumentType.getString(context, "name");
                            FactionStateManager data = FactionStateManager.get(context.getSource().getServer());

                            try {
                                data.createFaction(name, player, context.getSource().getServer());
                                context.getSource().sendSuccess(() -> Component.literal("Faction \"" + name + "\" created!"), false);
                            } catch (RuntimeException e) {
                                context.getSource().sendFailure(Component.literal(e.getMessage()));
                            }
                            return 1;
                        })))

                // Invite
                .then(Commands.literal("invite")
                        .requires(source -> {
                            try {
                                return FactionStateManager.get(source.getServer()).playerIsOwnerOrOfficer(source.getPlayerOrException().getUUID());
                            } catch (CommandSyntaxException e) {
                                return false;
                            }
                        })
                        .then(Commands.argument("target", EntityArgument.player()).suggests(UNINVITED_PLAYERS).executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "target");
                            FactionStateManager data = FactionStateManager.get(context.getSource().getServer());

                            try {
                                String name = data.invitePlayer(player, target);
                                context.getSource().sendSuccess(() -> Component.literal("Invited " + target.getName().getString()), false);
                                //
                                target.sendSystemMessage(Component.literal("You were invited to a faction! Type /faction join <" + name + ">"));
                            } catch (RuntimeException e) {
                                context.getSource().sendFailure(Component.literal(e.getMessage()));
                            }
                            return 1;
                        })))

                // Join
                .then(Commands.literal("join")
                        .requires(source -> {
                            try {
                                return !FactionStateManager.get(source.getServer()).playerIsInFaction(source.getPlayerOrException().getUUID());
                            } catch (CommandSyntaxException e) {
                                return false;
                            }
                        })
                        .then(Commands.argument("name", StringArgumentType.greedyString()).suggests(FACTIONS_INVITATIONS_SUGGESTION).executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            String name = StringArgumentType.getString(context, "name");
                            FactionStateManager data = FactionStateManager.get(context.getSource().getServer());

                            try {
                                data.joinFaction(player, name, context.getSource().getServer());
                                context.getSource().sendSuccess(() -> Component.literal("Joined \"" + name + "\"!"), false);
                            } catch (RuntimeException e) {
                                context.getSource().sendFailure(Component.literal(e.getMessage()));
                            }

                            return 1;
                        })))

                // Leave
                .then(Commands.literal("leave")
                        .requires(source -> {
                            try {
                                return FactionStateManager.get(source.getServer()).playerIsInFaction(source.getPlayerOrException().getUUID());
                            } catch (CommandSyntaxException e) {
                                return false;
                            }
                        }).executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            FactionStateManager data = FactionStateManager.get(context.getSource().getServer());

                            try {
                                data.leaveFaction(player, context.getSource().getServer());
                                context.getSource().sendSuccess(() -> Component.literal("You left the faction."), false);
                            } catch (RuntimeException e) {
                                context.getSource().sendFailure(Component.literal(e.getMessage()));
                            }
                            return 1;
                        }))
                // kick
                .then(Commands.literal("kick")
                        .requires(source -> {
                            try {
                                return FactionStateManager.get(source.getServer()).playerIsOwnerOrOfficer(source.getPlayerOrException().getUUID());
                            } catch (CommandSyntaxException e) {
                                return false;
                            }
                        })
                        .then(Commands.argument("member", StringArgumentType.word()).suggests(FACTION_MEMBERS)
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    String targetName = StringArgumentType.getString(context, "member");
                                    FactionStateManager data = FactionStateManager.get(context.getSource().getServer());

                                    try {
                                        data.kickFromFaction(player, targetName, context.getSource().getServer());
                                        context.getSource().sendSuccess(() -> Component.literal("Kicked " + targetName), false);
                                    } catch (RuntimeException e) {
                                        context.getSource().sendFailure(Component.literal(e.getMessage()));
                                    }
                                    return 1;
                                })))

                // Friendly Fire
                .then(Commands.literal("friendlyFire")
                        .requires(source -> {
                            try {
                                if (ServerConfig.forceFriendlyFire) return false;
                                return FactionStateManager.get(source.getServer()).playerIsOwnerOrOfficer(source.getPlayerOrException().getUUID());
                            } catch (CommandSyntaxException e) {
                                return false;
                            }
                        })
                        .then(Commands.argument("enabled", BoolArgumentType.bool()).executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            boolean newFFState = BoolArgumentType.getBool(context, "enabled");
                            FactionStateManager data = FactionStateManager.get(context.getSource().getServer());

                            try {
                                data.setFriendlyFire(player, newFFState);
                                String status = newFFState ? "enabled" : "disabled";
                                context.getSource().sendSuccess(() -> Component.literal("Friendly fire is now " + status + "."), false);
                            } catch (RuntimeException e) {
                                context.getSource().sendFailure(Component.literal(e.getMessage()));
                            }

                            return 1;

                        })))

                // Faction info
                .then(Commands.literal("about")
                        .requires(source -> {
                            try {
                                return FactionStateManager.get(source.getServer()).playerIsInFaction(source.getPlayerOrException().getUUID());
                            } catch (CommandSyntaxException e) {
                                return false;
                            }
                        }).executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            Faction playerFaction = FactionStateManager.get(context.getSource().getServer()).getFactionByPlayer(player.getUUID());

                            if (playerFaction == null) {
                                context.getSource().sendSuccess(() -> Component.literal("You are currently not in a faction."), false);
                            } else {
                                StringBuilder builder = new StringBuilder();
                                builder.append("You are a member of \"").append(playerFaction.getName()).append("\"\nYour members are: ");
                                for (UUID member : playerFaction.getMembers()) {
                                    builder.append(Utils.getPlayerNameOffline(member, context.getSource().getServer())).append(" ");
                                }
                                context.getSource().sendSuccess(() -> Component.literal(builder.toString()), false);
                            }

                            return 1;
                        }))
                .then(Commands.literal("addOfficer")
                        .requires(source -> {
                            try {
                                return FactionStateManager.get(source.getServer()).playerOwnsFaction(source.getPlayerOrException().getUUID());
                            } catch (CommandSyntaxException e) {
                                return false;
                            }
                        })
                        .then(Commands.argument("member", StringArgumentType.word()).suggests(FACTION_MEMBERS_NON_OFFICER)
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    String targetName = StringArgumentType.getString(context, "member");
                                    FactionStateManager data = FactionStateManager.get(context.getSource().getServer());

                                    try {
                                        data.addOfficer(targetName, player, context.getSource().getServer());
                                        context.getSource().sendSuccess(() -> Component.literal("Added " + targetName + "as officer."), false);
                                    } catch (RuntimeException e) {
                                        context.getSource().sendFailure(Component.literal(e.getMessage()));
                                    }
                                    return 1;
                                })))
                .then(Commands.literal("removeOfficer")
                        .requires(source -> {
                            try {
                                return FactionStateManager.get(source.getServer()).playerOwnsFaction(source.getPlayerOrException().getUUID());
                            } catch (CommandSyntaxException e) {
                                return false;
                            }
                        })
                        .then(Commands.argument("member", StringArgumentType.word()).suggests(FACTION_OFFICERS)
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    String targetName = StringArgumentType.getString(context, "member");
                                    FactionStateManager data = FactionStateManager.get(context.getSource().getServer());

                                    try {
                                        data.removeOfficer(targetName, player, context.getSource().getServer());
                                        context.getSource().sendSuccess(() -> Component.literal("Demoted " + targetName), false);
                                    } catch (RuntimeException e) {
                                        context.getSource().sendFailure(Component.literal(e.getMessage()));
                                    }
                                    return 1;
                                })))

        );
    }

    /**
     * Suggestions for joining a faction. Suggests the factions a user is invited to.
     */
    private static final SuggestionProvider<CommandSourceStack> FACTIONS_INVITATIONS_SUGGESTION = (context, builder) -> {
        ServerPlayer player = context.getSource().getPlayerOrException();
        FactionStateManager data = FactionStateManager.get(context.getSource().getServer());

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
        FactionStateManager data = FactionStateManager.get(context.getSource().getServer());
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
        ServerPlayer executingUser = context.getSource().getPlayerOrException();
        FactionStateManager data = FactionStateManager.get(context.getSource().getServer());
        Faction faction = data.getFactionByPlayer(executingUser.getUUID());

        for (UUID memberUUID : faction.getMembers()) {
            if (memberUUID.equals(faction.getOwner())) continue;

            builder.suggest(Utils.getPlayerNameOffline(memberUUID, context.getSource().getServer()));
        }

        return builder.buildFuture();
    };

    /**
     * Suggests all players that are inside the faction of the leader, except its leader and officer
     */
    private static final SuggestionProvider<CommandSourceStack> FACTION_MEMBERS_NON_OFFICER = (context, builder) -> {
        ServerPlayer executingUser = context.getSource().getPlayerOrException();
        FactionStateManager data = FactionStateManager.get(context.getSource().getServer());
        Faction faction = data.getFactionByPlayer(executingUser.getUUID());

        for (UUID memberUUID : faction.getMembers()) {
            if (memberUUID.equals(faction.getOwner())) continue;
            if (faction.getOfficers().contains(memberUUID)) continue;

            builder.suggest(Utils.getPlayerNameOffline(memberUUID, context.getSource().getServer()));
        }

        return builder.buildFuture();
    };

    /**
     * Suggests all officers
     */
    private static final SuggestionProvider<CommandSourceStack> FACTION_OFFICERS = (context, builder) -> {
        ServerPlayer leader = context.getSource().getPlayerOrException();
        FactionStateManager data = FactionStateManager.get(context.getSource().getServer());
        Faction leaderFaction = data.getFactionByPlayer(leader.getUUID());

        for (UUID memberUUID : leaderFaction.getMembers()) {
            if (!leaderFaction.getOfficers().contains(memberUUID)) continue;

            builder.suggest(Utils.getPlayerNameOffline(memberUUID, context.getSource().getServer()));
        }

        return builder.buildFuture();
    };
}