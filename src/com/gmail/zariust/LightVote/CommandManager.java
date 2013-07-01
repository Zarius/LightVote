package com.gmail.zariust.LightVote;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandManager implements CommandExecutor {

    private final LightVote parent;

    public CommandManager(LightVote lightVote) {
        this.parent = lightVote;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
            String label, String[] args) {

        Player player = (Player) sender;
        if (sender instanceof Player) {
            player = (Player) sender;
            parent.voteManager.currentWorld = player.getWorld();
        } else {
            parent.log.sM(parent, "onPlayerCommand - sender is not a player, skipping commands.");
            return false;
        }
        String[] split = args;
        if (!label.equalsIgnoreCase("lvt"))
            return false;

        if (split.length == 0 || (split.length == 1 && (split[0].equalsIgnoreCase("help") || split[0].equalsIgnoreCase(LightVote.translate.tr("help"))))) {
            sender.sendMessage(ChatColor.GOLD + LightVote.translate.tr("Lightvote commands"));
            if (!(parent.config.lightVoteNoCommands)) {
                if (parent.voteManager.canStartVote(sender)) {
                    sender.sendMessage(ChatColor.GOLD + LightVote.translate.tr("/lvt start -- start a vote(for day)"));
                    sender.sendMessage(ChatColor.GOLD + LightVote.translate.tr("/lvt start night -- start a vote for night"));
                }
                sender.sendMessage(ChatColor.GOLD + LightVote.translate.tr("/lvt yes/no -- vote"));
            }

            if (parent.config.bedVote) {
                sender.sendMessage(ChatColor.GOLD + LightVote.translate.tr("Bedvote: sleep in a bed to start a vote for day or agree to one in progress."));
            }

            try {
                sender.sendMessage(
                        ChatColor.GOLD + LightVote.translate.tr("Itemvote: vote for day - hit {itemyes} onto {hitsyes} for yes. {itemno} onto {hitsno} for no.")
                                .replace("{itemyes}", parent.config.bedVoteItemInHandDay.name())
                                .replace("{hitsyes}", parent.config.bedVoteItemHitsDay.name())
                                .replace("{itemno}", parent.config.bedVoteNoVoteItemInHandDay.name())
                                .replace("{hitsno}", parent.config.bedVoteNoVoteItemHitsDay.name())
                        );
                sender.sendMessage(
                        ChatColor.GOLD + LightVote.translate.tr("Itemvote: vote for night - hit {itemyes} onto {hitsyes} for yes. {itemno} onto {hitsno} for no.")
                                .replace("{itemyes}", parent.config.bedVoteItemInHandNight.name())
                                .replace("{hitsyes}", parent.config.bedVoteItemHitsNight.name())
                                .replace("{itemno}", parent.config.bedVoteNoVoteItemInHandNight.name())
                                .replace("{hitsno}", parent.config.bedVoteNoVoteItemHitsNight.name())
                        );
            } catch (Exception e) {
                System.out.println("Exception on help");
            }

            sender.sendMessage(ChatColor.GOLD + LightVote.translate.tr("/lvt help -- this message"));
            sender.sendMessage(ChatColor.GOLD + LightVote.translate.tr("/lvt info -- some information"));
            return true;
        }

        if (split[0].equalsIgnoreCase("info") || split[0].equalsIgnoreCase(LightVote.translate.tr("info"))) {
            sender.sendMessage(
                    ChatColor.GOLD + LightVote.translate.tr("Lightvote created by XUPWUP, further developer by Xarqn, Tickleman")
                    );
            sender.sendMessage(
                    ChatColor.GOLD + LightVote.translate.tr("Lightvote version {version}")
                            .replace("{version}", parent.getDescription().getVersion())
                    );
            sender.sendMessage(
                    ChatColor.GOLD + LightVote.translate.tr("Static time is {time}")
                            .replace("{time}", LightVote.translate.tr(parent.config.perma ? "enabled" : "disabled"))
                    );
            sender.sendMessage(
                    ChatColor.GOLD + LightVote.translate.tr("Current time: {time} ({world})")
                            .replace("{time}", (new Double(player.getWorld().getTime() % 24000)).toString())
                            .replace("{world}", player.getWorld().getName())
                    );
            sender.sendMessage(
                    ChatColor.GOLD + LightVote.translate.tr("Bedvote is: " + (parent.config.bedVote ? "on - sleep in a bed to vote for day." : "off."))
                    );
            try {
                sender.sendMessage(
                        ChatColor.GOLD + LightVote.translate.tr("Itemvote: vote for day - hit {itemyes} onto {hitsyes} for yes. {itemno} onto {hitsno} for no.")
                                .replace("{itemyes}", parent.config.bedVoteItemInHandDay.name())
                                .replace("{hitsyes}", parent.config.bedVoteItemHitsDay.name())
                                .replace("{itemno}", parent.config.bedVoteNoVoteItemInHandDay.name())
                                .replace("{hitsno}", parent.config.bedVoteNoVoteItemHitsDay.name())
                        );
                sender.sendMessage(
                        ChatColor.GOLD + LightVote.translate.tr("Itemvote: vote for night - hit {itemyes} onto {hitsyes} for yes. {itemno} onto {hitsno} for no.")
                                .replace("{itemyes}", parent.config.bedVoteItemInHandNight.name())
                                .replace("{hitsyes}", parent.config.bedVoteItemHitsNight.name())
                                .replace("{itemno}", parent.config.bedVoteNoVoteItemInHandNight.name())
                                .replace("{hitsno}", parent.config.bedVoteNoVoteItemHitsNight.name())
                        );
            } catch (Exception e) {
                System.out.println("Exception on info");
            }
            return true;
        }

        if (!(parent.config.lightVoteNoCommands)) {
            if (split[0].equalsIgnoreCase("start")
                    || split[0].equalsIgnoreCase(LightVote.translate.tr("start"))) {

                if (split.length > 1) {
                    if (split[1].equalsIgnoreCase("night")
                            || split[1].equalsIgnoreCase(LightVote.translate.tr("night"))) {
                        parent.voteManager.startVote("night", sender);
                    } else if (split[1].equalsIgnoreCase("sun")
                            || split[1].equalsIgnoreCase(LightVote.translate.tr("sun"))) {
                        parent.voteManager.startVote("sun", sender);
                    } else {
                        parent.voteManager.startVote("day", sender);
                    }
                } else {
                    parent.voteManager.startVote("day", sender);
                }

                // long currenttime = currentWorld.getTime();

                // startVote(this.dayVote, sender);
            } else if (split[0].equalsIgnoreCase("day")
                    || split[0].equalsIgnoreCase(LightVote.translate.tr("day"))) {
                if (!parent.voteManager.voting) {
                    parent.voteManager.startVote("day", sender);
                } else {
                    parent.voteManager.addToVote(parent.voteManager.dayVote, sender, parent.voteManager.dayVote == "day");
                }
            } else if (split[0].equalsIgnoreCase("night")
                    || split[0].equalsIgnoreCase(LightVote.translate.tr("night"))) {
                if (!parent.voteManager.voting) {
                    parent.voteManager.startVote("night", sender);
                } else {
                    parent.voteManager.addToVote(parent.voteManager.dayVote, sender, parent.voteManager.dayVote == "night");
                }
            } else if (split[0].equalsIgnoreCase("sun")
                    || split[0].equalsIgnoreCase(LightVote.translate.tr("sun"))) {
                if (!parent.voteManager.voting) {
                    parent.voteManager.startVote("sun", sender);
                } else {
                    parent.voteManager.addToVote(parent.voteManager.dayVote, sender, parent.voteManager.dayVote == "sun");
                }
            } else {
                if (split[0].equalsIgnoreCase("yes") || split[0].equalsIgnoreCase("y")
                        || split[0].equalsIgnoreCase(LightVote.translate.tr("yes"))
                        || split[0].equalsIgnoreCase(LightVote.translate.tr("y"))) {
                    parent.voteManager.addToVote(parent.voteManager.dayVote, sender, true);
                } else if (split[0].equalsIgnoreCase("no") || split[0].equalsIgnoreCase("n")
                        || split[0].equalsIgnoreCase(LightVote.translate.tr("no"))
                        || split[0].equalsIgnoreCase(LightVote.translate.tr("n"))) {
                    parent.log.sMdebug(parent, "Starting no vote...");
                    parent.voteManager.addToVote(parent.voteManager.dayVote, sender, false);
                }
            }
        }
        return true;
    }

}
