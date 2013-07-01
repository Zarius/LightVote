package com.gmail.zariust.LightVote;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.gmail.zariust.LightVote.votes.VoteManager;
//import java.util.Set;

/**
 * Handle events for all Player related events
 * @author XUPWUP
 */
public class LVTPlayerListener implements Listener {
    private final LightVote plugin;
    private final VoteManager voteManager;

	public LVTPlayerListener(LightVote instance) {
        plugin = instance;
        voteManager = new VoteManager(plugin);
    }
	
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerBedEnter (PlayerBedEnterEvent e)
	{
		if (plugin.config.bedVote) {
			Player player = e.getPlayer();
            voteManager.currentWorld = player.getWorld();
			long currenttime = player.getWorld().getTime();
			//String[] commandArgs = {""};
            if (!voteManager.voting) {
                voteManager.startVote(Utils.isDay(currenttime) ? "night" : "day", player);
			} else {
                voteManager.addToVote(Utils.isDay(currenttime) ? "night" : "day", player, true);
			}
			//player.sendMessage(ChatColor.GOLD + "Sleeping, attempting to vote for day time...");
			//onPlayerCommand(player, null, String.valueOf("lvt"), commandArgs);
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerInteract (PlayerInteractEvent e)
	{
		try {
		  if (plugin.config.itemVote) {	
			if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
				Player player = e.getPlayer();
                    voteManager.currentWorld = player.getWorld();
				long currenttime = player.getWorld().getTime();
				Material itemHits;
				Material itemInHand;
				Material noVoteItemHits;
				Material noVoteItemInHand;
				if (Utils.isDay(currenttime)) {
					itemHits = plugin.config.bedVoteItemHitsNight;
					itemInHand = plugin.config.bedVoteItemInHandNight;				
					noVoteItemHits = plugin.config.bedVoteNoVoteItemHitsNight;
					noVoteItemInHand = plugin.config.bedVoteNoVoteItemInHandNight;				
				} else {
					itemHits = plugin.config.bedVoteItemHitsDay;
					itemInHand = plugin.config.bedVoteItemInHandDay;
					noVoteItemHits = plugin.config.bedVoteNoVoteItemHitsNight;
					noVoteItemInHand = plugin.config.bedVoteNoVoteItemInHandNight;				
				}

				plugin.log.sMdebug(plugin, "Bedvote interaction detected... items loaded. itemHits: "+itemHits+" NoVoteItemHits: "+noVoteItemHits.name()+" itemhit: "+e.getClickedBlock().getType().name());

				if (e.getClickedBlock().getType() == itemHits) {
					if (e.getItem() != null) {
						if (e.getItem().getType() == itemInHand) {
							plugin.log.sMdebug(plugin, "Bedvote interaction detected... items matched.");
                                if (!voteManager.voting) {
                                    voteManager.startVote(Utils.isDay(currenttime) ? "night" : "day", player);
							} else {
                                    voteManager.addToVote(Utils.isDay(currenttime) ? "night" : "day", player, true);
							}
						}
					}
				} else if (e.getClickedBlock().getType() == noVoteItemHits) {
					if (e.getItem() != null) {
						plugin.log.sMdebug(plugin, "Bedvote interaction detected 'novote'... item held: "+e.getItem().getType().name()+" item needed: "+noVoteItemInHand.name());
						if (e.getItem().getType() == noVoteItemInHand) {
                                if (voteManager.voting) {
								//startVote(!(isDay(currenttime)), player);
							//} else {
                                    voteManager.addToVote(Utils.isDay(currenttime) ? "night" : "day", player, false);
							}
						}
					}
				}
			}
		  }
		} catch (Exception exception) {
			System.err.println("LightVote - 'onPlayerInteract' Error: " + exception.getMessage());			  
		}
	}

}
