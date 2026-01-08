package com.jpreiss.easy_factions.server.alliance;

import com.jpreiss.easy_factions.server.faction.Faction;
import com.jpreiss.easy_factions.server.faction.FactionStateManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class AllianceCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("alliance")
                // Create
                .then(Commands.literal("create")
                        .requires(source -> {
                            try {
                                ServerPlayer player = source.getPlayerOrException();
                                return AllianceStateManager.get(source.getServer()).getAllianceByFaction(FactionStateManager.get(source.getServer()).getOwnedFaction(player.getUUID()).getName()) == null;
                            } catch (CommandSyntaxException | RuntimeException e) {
                                return false;
                            }
                        })
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    String name = StringArgumentType.getString(context, "name");
                                    AllianceStateManager manager = AllianceStateManager.get(context.getSource().getServer());

                                    try {
                                        manager.createAlliance(name, player, context.getSource().getServer());
                                        context.getSource().sendSuccess(() -> Component.literal("Alliance \"" + name + "\" created!"), false);
                                    } catch (RuntimeException e) {
                                        context.getSource().sendFailure(Component.literal(e.getMessage()));
                                    }
                                    return 1;
                                })))

                // Invite
                .then(Commands.literal("invite")
                        .requires(source -> {
                            try {
                                ServerPlayer player = source.getPlayerOrException();
                                return AllianceStateManager.get(source.getServer()).getAllianceByFaction(FactionStateManager.get(source.getServer()).getOwnedFaction(player.getUUID()).getName()) != null;
                            } catch (CommandSyntaxException | RuntimeException e) {
                                return false;
                            }
                        })
                        .then(Commands.argument("target", StringArgumentType.greedyString())
                                .suggests(UNINVITED_FACTIONS)
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    String target = StringArgumentType.getString(context, "target");
                                    AllianceStateManager manager = AllianceStateManager.get(context.getSource().getServer());

                                    try {
                                        manager.inviteFaction(player, target, context.getSource().getServer());
                                        context.getSource().sendSuccess(() -> Component.literal("Invited \"" + target + "\"."), false);
                                    } catch (RuntimeException e) {
                                        context.getSource().sendFailure(Component.literal(e.getMessage()));
                                    }
                                    return 1;
                                })))

                // Join
                .then(Commands.literal("join")
                        .requires(source -> {
                            try {
                                ServerPlayer player = source.getPlayerOrException();
                                return AllianceStateManager.get(source.getServer()).getAllianceByFaction(FactionStateManager.get(source.getServer()).getOwnedFaction(player.getUUID()).getName()) == null;
                            } catch (RuntimeException | CommandSyntaxException e) {
                                return false;
                            }
                        }).then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests(ALLIANCE_INVITATIONS_SUGGESTION)
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    String name = StringArgumentType.getString(context, "name");
                                    AllianceStateManager manager = AllianceStateManager.get(context.getSource().getServer());

                                    try {
                                        manager.joinAlliance(player, name, context.getSource().getServer());
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
                                ServerPlayer player = source.getPlayerOrException();
                                return AllianceStateManager.get(source.getServer()).getAllianceByFaction(FactionStateManager.get(source.getServer()).getFactionByPlayer(player.getUUID()).getName()) != null;
                            } catch (CommandSyntaxException | RuntimeException e) {
                                return false;
                            }
                        })
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            AllianceStateManager manager = AllianceStateManager.get(context.getSource().getServer());

                            try {
                                manager.leaveAlliance(player, context.getSource().getServer());
                                context.getSource().sendSuccess(() -> Component.literal("Your faction has left the alliance."), false);
                            } catch (RuntimeException e) {
                                context.getSource().sendFailure(Component.literal(e.getMessage()));
                            }
                            return 1;
                        }))

                // Alliance info
                .then(Commands.literal("about")
                        .requires(source -> {
                            try {
                                ServerPlayer player = source.getPlayerOrException();
                                return AllianceStateManager.get(source.getServer()).getAllianceByFaction(FactionStateManager.get(source.getServer()).getFactionByPlayer(player.getUUID()).getName()) != null;
                            } catch (CommandSyntaxException | RuntimeException e) {
                                return false;
                            }
                        }).executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            AllianceStateManager manager = AllianceStateManager.get(context.getSource().getServer());
                            Faction playerFaction = FactionStateManager.get(context.getSource().getServer()).getFactionByPlayer(player.getUUID());

                            if (playerFaction == null) {
                                context.getSource().sendSuccess(() -> Component.literal("You are currently not in a faction."), false);
                                return 1;
                            }

                            Alliance alliance = manager.getAllianceByFaction(playerFaction.getName());

                            if (alliance == null) {
                                context.getSource().sendSuccess(() -> Component.literal("Your faction is not in an alliance."), false);
                                return 1;
                            }

                            StringBuilder builder = new StringBuilder();
                            builder.append("Your faction is a member of \"").append(alliance.getName()).append("\". Members are: ");
                            for (String member : alliance.getMembers()) {
                                builder.append(member).append("\n");
                            }

                            context.getSource().sendSuccess(() -> Component.literal(builder.toString()), false);

                            return 1;
                        }))

        );
    }

    /**
     * Suggests all alliances a faction is invited to
     */
    private static final SuggestionProvider<CommandSourceStack> ALLIANCE_INVITATIONS_SUGGESTION = (context, builder) -> {
        ServerPlayer leader = context.getSource().getPlayerOrException();
        AllianceStateManager allianceManager = AllianceStateManager.get(context.getSource().getServer());
        FactionStateManager factionManager = FactionStateManager.get(context.getSource().getServer());

        Faction leaderFaction = factionManager.getFactionByPlayer(leader.getUUID());
        if (leaderFaction == null) return builder.buildFuture();

        for (String factionName : allianceManager.getInvitesForFaction(leaderFaction.getName())) {
            builder.suggest(factionName);
        }

        return builder.buildFuture();
    };

    /**
     * Suggests all uninvited factions
     */
    private static final SuggestionProvider<CommandSourceStack> UNINVITED_FACTIONS = (context, builder) -> {
        ServerPlayer leader = context.getSource().getPlayerOrException();
        AllianceStateManager allianceManager = AllianceStateManager.get(context.getSource().getServer());
        FactionStateManager factionManager = FactionStateManager.get(context.getSource().getServer());

        Faction leaderFaction = factionManager.getFactionByPlayer(leader.getUUID());
        if (leaderFaction == null) return builder.buildFuture();
        Alliance alliance = allianceManager.getAllianceByFaction(leaderFaction.getName());
        if (alliance == null) return builder.buildFuture();


        for (String factionName : factionManager.getAllFactionNames()) {
            if (alliance.getMembers().contains(factionName)) continue;

            builder.suggest(factionName);
        }
        return builder.buildFuture();
    };
}