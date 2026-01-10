package com.jpreiss.easy_factions.server.alliance;

import com.jpreiss.easy_factions.common.RelationshipStatus;
import com.jpreiss.easy_factions.server.ServerConfig;
import com.jpreiss.easy_factions.server.faction.Faction;
import com.jpreiss.easy_factions.server.faction.FactionStateManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

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
                            builder.append("Your faction is a member of \"");
                            if (alliance.getAbbreviation() != null) {
                                builder.append("[").append(alliance.getAbbreviation()).append("] ");
                            }
                            builder.append(alliance.getName()).append("\". Members are: ");
                            for (String member : alliance.getMembers()) {
                                builder.append(member).append("\n");
                            }

                            context.getSource().sendSuccess(() -> Component.literal(builder.toString()), false);

                            return 1;
                        }))
                .then(Commands.literal("setAbbreviation")
                        .requires(source -> {
                            try {
                                if (!ServerConfig.enableAbbreviation) {
                                    return false;
                                }

                                MinecraftServer server = source.getServer();
                                ServerPlayer player = source.getPlayerOrException();
                                FactionStateManager factionManager = FactionStateManager.get(server);
                                AllianceStateManager allianceManager = AllianceStateManager.get(server);

                                Faction playerFaction = factionManager.getOwnedFaction(player.getUUID());

                                Alliance alliance = allianceManager.getAllianceByFaction(playerFaction.getName());
                                if (alliance == null) return false;
                                return alliance.getAbbreviation() == null || ServerConfig.allowAbbreviationChange;
                            } catch (CommandSyntaxException | RuntimeException e) {
                                return false;
                            }
                        })
                        .then(Commands.argument("abbreviation", StringArgumentType.word())
                                .executes(context -> {
                                    String abbreviation = StringArgumentType.getString(context, "abbreviation");
                                    if (ServerConfig.allianceAbbreviationMinLength > abbreviation.length() || abbreviation.length() > ServerConfig.allianceAbbreviationMaxLength) {
                                        context.getSource().sendFailure(Component.literal("Abbreviation must be between " + ServerConfig.allianceAbbreviationMinLength + " and " + ServerConfig.allianceAbbreviationMaxLength + " letters long."));
                                        return 1;
                                    }

                                    MinecraftServer server = context.getSource().getServer();
                                    ServerPlayer player = context.getSource().getPlayerOrException();

                                    try {
                                        AllianceStateManager allianceManager = AllianceStateManager.get(server);
                                        Alliance alliance = allianceManager.getAllianceByFaction(FactionStateManager.get(server).getFactionByPlayer(player.getUUID()).getName());
                                        allianceManager.setAbbreviation(alliance.getName(), abbreviation, server);
                                        context.getSource().sendSuccess(() -> Component.literal("Set alliance abbreviation to " + abbreviation), false);
                                    } catch (RuntimeException e) {
                                        context.getSource().sendFailure(Component.literal(e.getMessage()));
                                    }


                                    return 1;
                                })))
                .then(Commands.literal("setRelation")
                        .requires(source -> {
                            try {
                                MinecraftServer server = source.getServer();
                                ServerPlayer player = source.getPlayerOrException();
                                FactionStateManager factionManager = FactionStateManager.get(server);
                                AllianceStateManager allianceManager = AllianceStateManager.get(server);

                                Faction playerFaction = factionManager.getOwnedFaction(player.getUUID());

                                return allianceManager.getAllianceByFaction(playerFaction.getName()) != null;
                            } catch (CommandSyntaxException | RuntimeException e) {
                                return false;
                            }
                        })
                        .then(Commands.argument("targetAlliance", StringArgumentType.string())
                                .suggests(OTHER_ALLIANCES)
                                .then(Commands.argument("status", StringArgumentType.word())
                                        .suggests(RelationshipStatus.RELATIONSHIP_STATUS)
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            String statusStr = StringArgumentType.getString(context, "status");
                                            String targetAlliance = StringArgumentType.getString(context, "targetAlliance");
                                            AllianceStateManager allianceManager = AllianceStateManager.get(context.getSource().getServer());


                                            try {
                                                RelationshipStatus status = RelationshipStatus.valueOf(statusStr);

                                                allianceManager.setRelation(targetAlliance, player, status);
                                                context.getSource().sendSuccess(() -> Component.literal("Set relation with " + targetAlliance + " to " + status.name()  + ".\nThe relation between your alliances will be the lowest one between the one set by you and the one ste by them."), false);
                                            } catch (IllegalArgumentException e) {
                                                context.getSource().sendFailure(Component.literal("Invalid relationship status: " + statusStr));
                                            } catch (RuntimeException e) {
                                                context.getSource().sendFailure(Component.literal(e.getMessage()));
                                            }
                                            return 1;
                                        }))))

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

    /**
     * Suggests all other alliances
     */
    private static final SuggestionProvider<CommandSourceStack> OTHER_ALLIANCES = (context, builder) -> {
        FactionStateManager factionManager = FactionStateManager.get(context.getSource().getServer());
        AllianceStateManager stateManager = AllianceStateManager.get(context.getSource().getServer());

        try{
            Alliance alliance = stateManager.getAllianceByFaction(factionManager.getFactionByPlayer(Objects.requireNonNull(context.getSource().getPlayer()).getUUID()).getName());
            if(alliance == null) return builder.buildFuture();

            for (String allianceName: stateManager.getAllianceNames()){
                if(!allianceName.equals(alliance.getName())){
                    if(allianceName.contains(" ")){
                        builder.suggest("\"" + allianceName + "\"");
                    }
                    else{
                        builder.suggest(allianceName);
                    }

                }

            }
        }
        catch (NullPointerException ignore){}

        return  builder.buildFuture();
    };
}