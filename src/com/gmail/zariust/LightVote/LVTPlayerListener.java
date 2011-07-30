package com.gmail.zariust.LightVote;

import java.util.List;
import java.util.HashSet;
//import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;

/**
 * Handle events for all Player related events
 * @author XUPWUP
 */
public class LVTPlayerListener extends PlayerListener {
    private final LightVote plugin;
    //private Logger log;

	//private double reqYesVotes, minAgree;
	//private int permaOffset; 
	//private int voteTime, voteFailDelay, votePassDelay, voteRemindCount;
	//private boolean perma, bedVote;
	private static final int nightstart = 14000;
	//private Set<String> canStartVotes = null;
    
    public LVTPlayerListener(LightVote instance, Logger log) {
        plugin = instance;
        //this.log = log;
    }
	
    
    /*public void config(double reqYesVotes, double minAgree, int permaOffset, int voteTime, int voteFailDelay, int votePassDelay, int voteRemindCount, boolean perma, Set<String> set, boolean bedVote){
    	this.reqYesVotes = reqYesVotes;
    	this.minAgree = minAgree;
    	this.permaOffset = permaOffset;
    	this.voteTime = voteTime;
    	this.voteFailDelay = voteFailDelay;
    	this.votePassDelay = votePassDelay;
    	this.voteRemindCount = voteRemindCount;
    	this.perma = perma;
    	canStartVotes = set;
    	this.bedVote = bedVote;
    }*/

	private Integer agrees = 0;
	private boolean dayVote = true;
	private int remindCounter = 0;
	private boolean disabled = false;
	private boolean voting = false;
	Timer tReset = null;
	private World currentWorld = null;
	
	private HashSet<Player> voters = new HashSet<Player>();
	
	Timer t = new Timer();
	Timer reminder;
	
	
	private boolean isDay(long currenttime){
		return ((currenttime % 24000) < 12000 && currenttime > 0 )|| (currenttime < 0 && (currenttime % 24000) < -12000);
	}
	
	
	public void setReset(){
		tReset = new Timer();
    	tReset.schedule(new timeReset(), 15000, 15000);
	}
	
	private class timeReset extends TimerTask{
		public void run(){
			long currenttime = plugin.getServer().getWorlds().get(0).getTime();
			boolean isNight = !isDay(currenttime);
			currenttime = currenttime - (currenttime % 24000); // one day lasts 24000
			currenttime += plugin.config.permaOffset;
			if (isNight) currenttime += nightstart;
			plugin.getServer().getWorlds().get(0).setTime(currenttime);
		}
	}
	
	
	private class voteEnd extends TimerTask{
		public void run(){
			endVote();
		}
	}
	
	private class reEnable extends TimerTask{
		public void run(){
			disabled = false;
		}
	}
	
	private class remind extends TimerTask{
		public void run(){
			int timeBetween = plugin.config.voteTime / (plugin.config.voteRemindCount+1);
			remindCounter++;
			if (remindCounter > plugin.config.voteRemindCount) {
				reminder.cancel();
				return;
			}
			
			for (Player player : currentWorld.getPlayers()) {
				Integer seconds = (plugin.config.voteTime - remindCounter * timeBetween) / 1000;
				player.sendMessage(
					LightVote.translate.tr(ChatColor.GOLD + "Vote for {what}, {seconds} seconds remaining.")
					.replaceAll("{what}", (dayVote ? "day" : "night"))
					.replaceAll("{seconds}", seconds.toString())
				);
			}
			//plugin.getServer().broadcastMessage(ChatColor.GOLD + "Vote for " + (dayVote ? "day" : "night") + ", " + (plugin.config.voteTime - remindCounter*timeBetween)/1000 + " seconds remaining.");
		}
	}	
	
	private void endVote(){
		plugin.sMdebug("Starting endvote...");
		List<Player> playerlist = currentWorld.getPlayers();
		plugin.sMdebug("Endvote: got players...");
		String msg = "";
		boolean passed = false;

		double reqYesVotes;
		double minAgree;
		
		int numplayers = playerlist.size();
		if (dayVote) {
			reqYesVotes = plugin.config.reqYesVotesDay;
			minAgree = plugin.config.minAgreeDay;
		} else {
			reqYesVotes = plugin.config.reqYesVotesNight;
			minAgree = plugin.config.minAgreeNight;
		}
		if (voters.size() > numplayers * reqYesVotes){
			if (agrees > minAgree * voters.size()) {
				Integer disagrees = voters.size() - agrees;
				msg = LightVote.translate.tr("Vote passed. ({agrees} yes, {disagrees} no)")
				.replaceAll("{agrees}", agrees.toString())
				.replaceAll("{disagrees}", disagrees.toString());
				//msg = "Vote passed. (" + agrees + " yes, " + (voters.size() - agrees) + " no)";
				long currenttime = currentWorld.getTime();
				currenttime = currenttime - (currenttime % 24000); // one day lasts 24000
				
				if (currenttime < 0){
					currenttime *= -1;
					plugin.sM("LVT: Current time was negative!");
				}
				
				if (!dayVote) currenttime += nightstart;
				if(plugin.config.perma) currenttime += plugin.config.permaOffset;
				
				currentWorld.setTime(currenttime);
				passed = true;
				plugin.sM("LVT: changed time to "+ (dayVote ? "day" : "night"));
			}
			else {
				Integer disagrees = voters.size() - agrees;
				msg = LightVote.translate.tr("Vote failed. ({agrees} yes, {disagrees} no)")
				.replaceAll("{agrees}", agrees.toString())
				.replaceAll("{disagrees}", disagrees.toString());
				//msg = "Vote failed. (" + agrees + " yes, " + (voters.size() - agrees) + " no)";
				plugin.sM("LVT: vote failed (" + voters.size() + " votes, "+ agrees + " agree)");
			}
		}else{
			Double numReqYesVotes = Math.ceil(numplayers * reqYesVotes * 100) / 100;
			msg = LightVote.translate.tr("Vote failed, insufficient \"yes\" votes. ({agrees}/{required})")
			.replaceAll("{agrees}", agrees.toString())
			.replaceAll("{required}", numReqYesVotes.toString());
			// msg = "Vote failed, insufficient \"yes\" votes. (" + agrees + "/" + (numplayers * reqYesVotes) + ")";
			plugin.sM("LVT: vote failed, insufficient votes (" + agrees + " yes votes, "+ numplayers + " players, req " + (numplayers * reqYesVotes)+ ")");
		}
		
		plugin.sMdebug("Endvote: checked status, broadcasting message...");

		for (Player player : playerlist) {
			player.sendMessage(ChatColor.GOLD + msg);
		}
		//plugin.getServer().broadcastMessage(ChatColor.GOLD + msg);
		agrees = 0;
		voters = new HashSet<Player>();
		voting = false;
		disabled = true;
		Timer reenable = new Timer();
		reenable.schedule(new reEnable(), (passed ? plugin.config.votePassDelay : plugin.config.voteFailDelay));
	}
	
	public boolean canSVote(CommandSender sender){
		if(sender instanceof Player) {
			plugin.logInfo("in perm check");
			if (plugin.config.usePermissions && plugin.permissionHandler != null) {
				Player player = (Player) sender;
				plugin.logInfo("in perm check perm -"+ plugin.permissionHandler.has(player, "lvt.vote.time"));
				return plugin.permissionHandler.has(player, "lvt.vote.time");				
			}
			return plugin.config.canStartVotes == null || plugin.config.canStartVotes.contains(((Player) sender).getName().toLowerCase());
		} else return true;
	}
	
	public boolean onPlayerCommand(CommandSender sender, Command command,
    		String label, String[] args){

		Player player = (Player) sender;
		if (sender instanceof Player) {
			player = (Player) sender;
			currentWorld = player.getWorld();
		} else {
			plugin.sM("onPlayerCommand - sender is not a player, skipping commands.");
			return false;
		}
		String[] split = args;
		if (!label.equalsIgnoreCase("lvt")) return false;

		if (split.length == 0 || (split.length == 1 && (split[0].equalsIgnoreCase("help") || split[0].equalsIgnoreCase(LightVote.translate.tr("help"))))){
			sender.sendMessage(ChatColor.GOLD + LightVote.translate.tr("Lightvote commands"));
			if (!(plugin.config.lightVoteNoCommands)) {
				if(canSVote(sender)) {
					sender.sendMessage(ChatColor.GOLD + LightVote.translate.tr("/lvt start -- start a vote(for day)"));
					sender.sendMessage(ChatColor.GOLD + LightVote.translate.tr("/lvt start night -- start a vote for night"));
				}
				sender.sendMessage(ChatColor.GOLD + LightVote.translate.tr("/lvt yes/no -- vote"));
			}

			if (plugin.config.bedVote) {
				sender.sendMessage(ChatColor.GOLD + LightVote.translate.tr("Bedvote: sleep in a bed to start a vote for day or agree to one in progress."));
			}
			
			sender.sendMessage(
				ChatColor.GOLD + LightVote.translate.tr("Itemvote: vote for day - hit {itemyes} onto {hitsyes} for yes. {itemno} onto {hitsno} for no.")
				.replaceAll("{itemyes}", plugin.config.bedVoteItemInHandDay.toString())
				.replaceAll("{hitsyes}", plugin.config.bedVoteItemHitsDay.toString())
				.replaceAll("{itemno}", plugin.config.bedVoteNoVoteItemInHandDay.toString())
				.replaceAll("{hitsno}", plugin.config.bedVoteNoVoteItemHitsDay.toString())
			);
			//sender.sendMessage(
			//	ChatColor.GOLD + "Itemvote: vote for day - hit "
			//	+ plugin.config.bedVoteItemInHandDay + " onto "
			//	+ plugin.config.bedVoteItemHitsDay + " for yes. " 
			//	+ plugin.config.bedVoteNoVoteItemInHandDay + " onto "
			//	+ plugin.config.bedVoteNoVoteItemHitsDay + " for no."
			//);
			sender.sendMessage(
				ChatColor.GOLD + LightVote.translate.tr("Itemvote: vote for night - hit {itemyes} onto {hitsyes} for yes. {itemno} onto {hitsno} for no.")
				.replaceAll("{itemyes}", plugin.config.bedVoteItemInHandNight.toString())
				.replaceAll("{hitsyes}", plugin.config.bedVoteItemHitsNight.toString())
				.replaceAll("{itemno}", plugin.config.bedVoteNoVoteItemInHandNight.toString())
				.replaceAll("{hitsno}", plugin.config.bedVoteNoVoteItemHitsNight.toString())
			);
			//sender.sendMessage(ChatColor.GOLD + "Itemvote: vote for night - hit "+plugin.config.bedVoteItemInHandNight+" onto "+plugin.config.bedVoteItemHitsNight+ " for yes. " 
			//		+plugin.config.bedVoteNoVoteItemInHandNight+" onto " +plugin.config.bedVoteNoVoteItemHitsNight+" for no.");

			sender.sendMessage(ChatColor.GOLD + LightVote.translate.tr("/lvt help -- this message"));
			sender.sendMessage(ChatColor.GOLD + LightVote.translate.tr("/lvt info -- some information"));
			return true;
		}
		
		if(split[0].equalsIgnoreCase("info")||split[0].equalsIgnoreCase(LightVote.translate.tr("info"))){
			sender.sendMessage(
				ChatColor.GOLD + LightVote.translate.tr("Lightvote created by XUPWUP, further developer by Xarqn")
			);
			sender.sendMessage(
				ChatColor.GOLD + LightVote.translate.tr("Lightvote version {version}")
				.replaceAll("{version}", plugin.getDescription().getVersion())
			);
			sender.sendMessage(
				ChatColor.GOLD + LightVote.translate.tr("Static time is {time}")
				.replaceAll("{time}", LightVote.translate.tr(plugin.config.perma ? "enabled" : "disabled"))
			);
			sender.sendMessage(
				ChatColor.GOLD + LightVote.translate.tr("Current time: {time} ({world})")
				.replaceAll("{time}", (new Double(player.getWorld().getTime() % 24000)).toString())
				.replaceAll("{world}", player.getWorld().getName())
			);
			sender.sendMessage(
				ChatColor.GOLD + LightVote.translate.tr("Bedvote is: " + (plugin.config.bedVote ? "on - sleep in a bed to vote for day." : "off."))
			);
			sender.sendMessage(
				ChatColor.GOLD + LightVote.translate.tr("Itemvote: vote for day - hit {itemyes} onto {hitsyes} for yes. {itemno} onto {hitsno} for no.")
				.replaceAll("{itemyes}", plugin.config.bedVoteItemInHandDay.toString())
				.replaceAll("{hitsyes}", plugin.config.bedVoteItemHitsDay.toString())
				.replaceAll("{itemno}", plugin.config.bedVoteNoVoteItemInHandDay.toString())
				.replaceAll("{hitsno}", plugin.config.bedVoteNoVoteItemHitsDay.toString())
			);
			sender.sendMessage(
				ChatColor.GOLD + LightVote.translate.tr("Itemvote: vote for night - hit {itemyes} onto {hitsyes} for yes. {itemno} onto {hitsno} for no.")
				.replaceAll("{itemyes}", plugin.config.bedVoteItemInHandNight.toString())
				.replaceAll("{hitsyes}", plugin.config.bedVoteItemHitsNight.toString())
				.replaceAll("{itemno}", plugin.config.bedVoteNoVoteItemInHandNight.toString())
				.replaceAll("{hitsno}", plugin.config.bedVoteNoVoteItemHitsNight.toString())
			);
			return true;
		}

		if (!(plugin.config.lightVoteNoCommands)) {
		if (
			split[0].equalsIgnoreCase("start")
			|| split[0].equalsIgnoreCase(LightVote.translate.tr("start"))
		) {

			if (split.length > 1){
				if (
					split[1].equalsIgnoreCase("night")
					|| split[1].equalsIgnoreCase(LightVote.translate.tr("night"))
				) {
					startVote(false, sender);
				} else {
					startVote(true, sender);
				}
			} else {
				startVote(true, sender);
			}
			
			//long currenttime = currentWorld.getTime();				
						
			//startVote(this.dayVote, sender);
		} else if (
			split[0].equalsIgnoreCase("day")
			|| split[0].equalsIgnoreCase(LightVote.translate.tr("day"))
		) {
			if (!this.voting) {
				startVote(true, sender);
			} else {
				addToVote(this.dayVote, sender, this.dayVote);
			}
		} else if (
			split[0].equalsIgnoreCase("night")
			|| split[0].equalsIgnoreCase(LightVote.translate.tr("night"))
		) {
			if (!this.voting) {
				startVote(false, sender);
			} else {
				addToVote(this.dayVote, sender, (!this.dayVote));
			}
		} else {
			if (
				split[0].equalsIgnoreCase("yes") || split[0].equalsIgnoreCase("y")
				|| split[0].equalsIgnoreCase(LightVote.translate.tr("yes"))
				|| split[0].equalsIgnoreCase(LightVote.translate.tr("y"))
			) {
				addToVote(this.dayVote, sender, true);
			} else if (
				split[0].equalsIgnoreCase("no") || split[0].equalsIgnoreCase("n")
				|| split[0].equalsIgnoreCase(LightVote.translate.tr("no"))
				|| split[0].equalsIgnoreCase(LightVote.translate.tr("n"))
			) {
				plugin.sMdebug("Starting no vote...");
				addToVote(this.dayVote, sender, false);
			}
		}
		}
		return true;
	}
	
	public boolean addToVote(boolean day, CommandSender sender, boolean agreed) {
		if(sender instanceof Player) if (voters.contains((Player) sender)){
			sender.sendMessage(ChatColor.GOLD + LightVote.translate.tr("You have already voted"));
			return true;
		}
		if (!voting){
			sender.sendMessage(
				ChatColor.GOLD + LightVote.translate.tr("'{yesno}' vote attempted but no votes in progress.")
				.replaceAll("{yesno}", LightVote.translate.tr(agreed ? "Yes" : "No"))
				+ (plugin.config.lightVoteNoCommands
					? LightVote.translate.tr("Use /lvt help to find out how to start a vote.")
					: LightVote.translate.tr("Use /lvt start to start a vote for day or /lvt help for more info.")
				)
			);
			return true;
		}

		//boolean agreed = false;
		//if (split[0].equalsIgnoreCase("yes") || split[0].equalsIgnoreCase("y")) {
		if (agreed) {
			agrees++;
		} else {
			//agrees--;
		}

		if(sender instanceof Player) voters.add((Player)sender);
		if (voters.size() == currentWorld.getPlayers().size()){// plugin.getServer().getOnlinePlayers().length){
			t.cancel();
			t = new Timer();
			reminder.cancel();
			endVote();
		}
		sender.sendMessage(
			ChatColor.GOLD + LightVote.translate.tr("Thanks for voting! ({vote})")
			.replace("{vote}", LightVote.translate.tr(agreed ? "yes" : "no"))
		);

		return true;
	}
		
	public boolean startVote(boolean day, CommandSender sender) {

		String daymsg = "";
		if (day){
			daymsg = "for daylight";
		}else daymsg = "for darkness";
		
		String pname;
		
		this.dayVote = day;
		
		voters.clear();
		agrees = 1;

		if(sender instanceof Player) {
			pname =((Player) sender).getName();
			voters.add((Player) sender);
		}else {
			pname = "<CONSOLE>";
		}

		if(!canSVote(sender)){
			sender.sendMessage(ChatColor.GOLD + LightVote.translate.tr("You are not allowed to start votes."));
			return true;
		}
		if (voting) {
			sender.sendMessage(ChatColor.GOLD + LightVote.translate.tr("A vote is still in progress."));
			return true;
		}
		
		if (disabled){
			sender.sendMessage(ChatColor.GOLD + LightVote.translate.tr("You cannot vote again this quickly."));
			return true;
		}
		
		if (isDay(currentWorld.getTime())){ // it is day now
			if (day){
				sender.sendMessage(ChatColor.GOLD + LightVote.translate.tr("It is already day!"));
				return true;
			}
		}else{ // it is night now
			if (!day){
				sender.sendMessage(ChatColor.GOLD + LightVote.translate.tr("It is already night!"));
				return true;
			}
		}

		// After all checks (vote in progress, permission, etc - set vote type to day or night
		this.dayVote = day;
		
		voting = true;
		plugin.sMdebug("Startvote detected... just before broadcast message.");

		for (Player player : currentWorld.getPlayers()) {
			player.sendMessage(
				ChatColor.GOLD + LightVote.translate.tr("Lightvote {daynight} in world '{world}' started by {player},")
				.replaceAll("{daynight}", LightVote.translate.tr(daymsg))
				.replaceAll("{world}", currentWorld.getName())
				.replaceAll("{player}", pname)
			);
			if (plugin.config.lightVoteNoCommands) {
				player.sendMessage(ChatColor.GOLD + LightVote.translate.tr("type /lvt help to find out how to vote."));
			} else {
				player.sendMessage(ChatColor.GOLD + LightVote.translate.tr("type /lvt yes, or /lvt no to vote."));
			}
		}

		//plugin.getServer().broadcastMessage(ChatColor.GOLD + "Lightvote " + daymsg + " in world '"+currentWorld.getName()+"' started by "+ pname + ",");
		//plugin.getServer().broadcastMessage(ChatColor.GOLD + "type /lvt yes, or /lvt no to vote.");
		
		t.schedule(new voteEnd(), plugin.config.voteTime);
		if (voters.size() == currentWorld.getPlayers().size()){
			t.cancel();
			t = new Timer();
			endVote();
			return true;
		}
		
		reminder = new Timer();
		
		if (plugin.config.voteRemindCount > 0){
			remindCounter = 0;
			int timeBetween = plugin.config.voteTime / (plugin.config.voteRemindCount+1);
			reminder.schedule(new remind(), timeBetween, timeBetween);
		}
		return true;
	}

	public void onPlayerBedEnter (PlayerBedEnterEvent e)
	{
		if (plugin.config.bedVote) {
			Player player = e.getPlayer();
			currentWorld = player.getWorld();
			long currenttime = player.getWorld().getTime();
			//String[] commandArgs = {""};
			if (!voting) {
				startVote(!(isDay(currenttime)), player);
			} else {
				addToVote(!(isDay(currenttime)), player, true);
			}
			//player.sendMessage(ChatColor.GOLD + "Sleeping, attempting to vote for day time...");
			//onPlayerCommand(player, null, String.valueOf("lvt"), commandArgs);
		}
	}
	
	public void onPlayerInteract (PlayerInteractEvent e)
	{
		try {
		  if (plugin.config.itemVote) {	
			if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
				Player player = e.getPlayer();
				this.currentWorld = player.getWorld();
				long currenttime = player.getWorld().getTime();
				Material itemHits;
				Material itemInHand;
				Material noVoteItemHits;
				Material noVoteItemInHand;
				if (isDay(currenttime)) {
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

				plugin.sMdebug("Bedvote interaction detected... items loaded. itemHits: "+itemHits+" NoVoteItemHits: "+noVoteItemHits.name()+" itemhit: "+e.getClickedBlock().getType().name());

				if (e.getClickedBlock().getType() == itemHits) {
					if (e.getItem() != null) {
						if (e.getItem().getType() == itemInHand) {
							plugin.sMdebug("Bedvote interaction detected... items matched.");
							if (!voting) {
								startVote(!(isDay(currenttime)), player);
							} else {
								addToVote(!(isDay(currenttime)), player, true);
							}
						}
					}
				} else if (e.getClickedBlock().getType() == noVoteItemHits) {
					if (e.getItem() != null) {
						plugin.sMdebug("Bedvote interaction detected 'novote'... item held: "+e.getItem().getType().name()+" item needed: "+noVoteItemInHand.name());
						if (e.getItem().getType() == noVoteItemInHand) {
							if (voting) {
								//startVote(!(isDay(currenttime)), player);
							//} else {
								addToVote(!(isDay(currenttime)), player, false);
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