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

        if (args.length == 0 || (args.length == 1 && (matches(args[0], "help"))))
            return cmdHelp(sender);

        if (matches(args[0], "info"))
            return cmdInfo(sender, player);

        if (!(LightVote.config.lightVoteNoCommands)) {
            if (matches(args[0], "start"))
                cmdStart(sender, args);
            else if (matches(args[0], "day"))
                cmdStartOrAdd(sender, "day");
            else if (matches(args[0], "night"))
                cmdStartOrAdd(sender, "night");
            else if (matches(args[0], "sun"))
                cmdStartOrAdd(sender, "sun");
            else {
                if (matches(args[0], "y") || matches(args[0], "yes")) {
                    parent.voteManager.addToVote(parent.voteManager.dayVote, sender, true);
                } else if (matches(args[0], "n") || matches(args[0], "no")) {
                    parent.log.sMdebug(parent, "Starting no vote...");
                    parent.voteManager.addToVote(parent.voteManager.dayVote, sender, false);
                }
            }
        }
        return true;
    }

    private void cmdStartOrAdd(CommandSender sender, String voteType) {
        if (!parent.voteManager.voting) {
            parent.voteManager.startVote(voteType, sender);
        } else {
            parent.voteManager.addToVote(parent.voteManager.dayVote, sender, parent.voteManager.dayVote == voteType);
        }
    }

    private void cmdStart(CommandSender sender, String[] args) {
        if (args.length > 1) {
            if (matches(args[1], "night")) {
                parent.voteManager.startVote("night", sender);
            } else if (matches(args[1], "sun")) {
                parent.voteManager.startVote("sun", sender);
            } else {
                parent.voteManager.startVote("day", sender);
            }
        } else {
            parent.voteManager.startVote("day", sender);
        }
    }

    private boolean cmdInfo(CommandSender sender, Player player) {
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

    /**
     * @param sender
     * @return
     */
    private boolean cmdHelp(CommandSender sender) {
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

    private boolean matches(String match, String arg) {
        return arg.equalsIgnoreCase(match) || arg.equalsIgnoreCase(LightVote.translate.tr(match));
    }

}
