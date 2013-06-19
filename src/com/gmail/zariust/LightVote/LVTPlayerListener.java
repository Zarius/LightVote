package com.gmail.zariust.LightVote;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerInteractEvent;
//import java.util.Set;

/**
 * Handle events for all Player related events
 * @author XUPWUP
 */
public class LVTPlayerListener implements Listener {
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
	private String dayVote = "day";
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
		@Override
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
		@Override
        public void run(){
			endVote();
		}
	}
	
	private class reEnable extends TimerTask{
		@Override
        public void run(){
			disabled = false;
		}
	}
	
	private class remind extends TimerTask{
		@Override
        public void run(){
			int timeBetween = plugin.config.voteTime / (plugin.config.voteRemindCount+1);
			remindCounter++;
			if (remindCounter > plugin.config.voteRemindCount) {
				reminder.cancel();
				return;
			}
			
			for (Player player : onlinePlayers(currentWorld)) {
				Integer seconds = (plugin.config.voteTime - remindCounter * timeBetween) / 1000;
				player.sendMessage(
					ChatColor.GOLD + LightVote.translate.tr("Vote for {what}, {seconds} seconds remaining.")
					.replace("{what}", LightVote.translate.tr(dayVote))
					.replace("{seconds}", seconds.toString())
				);
			}
		}
	}	
	
	private void endVote(){
		plugin.sMdebug("Starting endvote...");
		Collection<Player> playerlist = onlinePlayers(currentWorld);
		plugin.sMdebug("Endvote: got players...");
		String msg = "";
		boolean passed = false;

		double reqYesVotes;
		double minAgree;
		
		int numplayers = playerlist.size();
		if (dayVote == "day") {
			reqYesVotes = plugin.config.reqYesVotesDay;
			minAgree = plugin.config.minAgreeDay;
		} else if (dayVote == "night") {
			reqYesVotes = plugin.config.reqYesVotesNight;
			minAgree = plugin.config.minAgreeNight;
		} else if (dayVote == "sun") {
			reqYesVotes = plugin.config.reqYesVotesSun;
			minAgree = plugin.config.minAgreeSun;
		} else {
			reqYesVotes = 0.005;
			minAgree = 0.5;
		}

		int onlinePlayers = 0;
		for (Player player : onlinePlayers(plugin.getServer().getWorld("world_skylands"))) {
			if (player.isOnline()) onlinePlayers ++;
		}
		if ((dayVote == "day") && (onlinePlayers > 0)) {

			// check if someone is on world_skylands (specific)
			msg = LightVote.translate.tr("Vote failed, some players are into {world}")
				.replace("{world}", LightVote.translate.tr("world_skylands"));

		} else {

			if (voters.size() > numplayers * reqYesVotes){
				if (agrees > minAgree * voters.size()) {
					Integer disagrees = voters.size() - agrees;
					msg = LightVote.translate.tr("Vote passed. ({agrees} yes, {disagrees} no)")
					.replace("{agrees}", agrees.toString())
					.replace("{disagrees}", disagrees.toString());
					long currenttime = currentWorld.getTime();
					currenttime = currenttime - (currenttime % 24000); // one day lasts 24000

					if (currenttime < 0){
						currenttime *= -1;
						plugin.sM("LVT: Current time was negative!");
					}

					if (dayVote == "night") currenttime += nightstart;
					if (plugin.config.perma) currenttime += plugin.config.permaOffset;

					if ((dayVote == "day") || (dayVote == "night")) {
						currentWorld.setTime(currenttime);
					} else if (dayVote == "sun") {
						if (currentWorld.hasStorm() || currentWorld.isThundering()) {
							currentWorld.setWeatherDuration(1);
							currentWorld.setStorm(false);
						}
					}
					passed = true;
					plugin.sM("LVT: changed time to "+ (dayVote));
				}
				else {
					Integer disagrees = voters.size() - agrees;
					msg = LightVote.translate.tr("Vote failed. ({agrees} yes, {disagrees} no)")
					.replace("{agrees}", agrees.toString())
					.replace("{disagrees}", disagrees.toString());
					plugin.sM("LVT: vote failed (" + voters.size() + " votes, "+ agrees + " agree)");
				}
			}else{
				Double numReqYesVotes = Math.ceil(numplayers * reqYesVotes * 100) / 100;
				msg = LightVote.translate.tr("Vote failed, insufficient \"yes\" votes. ({agrees}/{required})")
				.replace("{agrees}", agrees.toString())
				.replace("{required}", numReqYesVotes.toString());
				plugin.sM("LVT: vote failed, insufficient votes (" + agrees + " yes votes, "+ numplayers + " players, req " + (numplayers * reqYesVotes)+ ")");
			}
		}
		
		plugin.sMdebug("Endvote: checked status, broadcasting message...");

		for (Player player : playerlist) {
			player.sendMessage(ChatColor.GOLD + msg);
		}
		agrees = 0;
		voters = new HashSet<Player>();
		voting = false;
		disabled = true;
		Timer reenable = new Timer();
		reenable.schedule(new reEnable(), (passed ? plugin.config.votePassDelay : plugin.config.voteFailDelay));
	}
	
	public boolean canStartVote(CommandSender sender){
		if(sender instanceof Player) {
			if (plugin.config.usePermissions) {
				Player player = (Player) sender;
				return player.hasPermission("lvt.vote.time.start");				
			}
			return plugin.config.canStartVotes == null || plugin.config.canStartVotes.contains(((Player) sender).getName().toLowerCase());
		} else return true;
	}

	public boolean canJoinVote(CommandSender sender){
		if(sender instanceof Player) {
			if (canStartVote(sender)) return true; // if you can start a vote, you can join one
			if (plugin.config.usePermissions) {
				Player player = (Player) sender;
				return player.hasPermission("lvt.vote.time.join");				
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
				if(canStartVote(sender)) {
					sender.sendMessage(ChatColor.GOLD + LightVote.translate.tr("/lvt start -- start a vote(for day)"));
					sender.sendMessage(ChatColor.GOLD + LightVote.translate.tr("/lvt start night -- start a vote for night"));
				}
				sender.sendMessage(ChatColor.GOLD + LightVote.translate.tr("/lvt yes/no -- vote"));
			}

			if (plugin.config.bedVote) {
				sender.sendMessage(ChatColor.GOLD + LightVote.translate.tr("Bedvote: sleep in a bed to start a vote for day or agree to one in progress."));
			}
			
			try {
			sender.sendMessage(
				ChatColor.GOLD + LightVote.translate.tr("Itemvote: vote for day - hit {itemyes} onto {hitsyes} for yes. {itemno} onto {hitsno} for no.")
				.replace("{itemyes}", plugin.config.bedVoteItemInHandDay.name())
				.replace("{hitsyes}", plugin.config.bedVoteItemHitsDay.name())
				.replace("{itemno}", plugin.config.bedVoteNoVoteItemInHandDay.name())
				.replace("{hitsno}", plugin.config.bedVoteNoVoteItemHitsDay.name())
			);
			sender.sendMessage(
				ChatColor.GOLD + LightVote.translate.tr("Itemvote: vote for night - hit {itemyes} onto {hitsyes} for yes. {itemno} onto {hitsno} for no.")
				.replace("{itemyes}", plugin.config.bedVoteItemInHandNight.name())
				.replace("{hitsyes}", plugin.config.bedVoteItemHitsNight.name())
				.replace("{itemno}", plugin.config.bedVoteNoVoteItemInHandNight.name())
				.replace("{hitsno}", plugin.config.bedVoteNoVoteItemHitsNight.name())
			);
			} catch (Exception e) {
				System.out.println("Exception on help");
			}

			sender.sendMessage(ChatColor.GOLD + LightVote.translate.tr("/lvt help -- this message"));
			sender.sendMessage(ChatColor.GOLD + LightVote.translate.tr("/lvt info -- some information"));
			return true;
		}
		
		if(split[0].equalsIgnoreCase("info")||split[0].equalsIgnoreCase(LightVote.translate.tr("info"))){
			sender.sendMessage(
				ChatColor.GOLD + LightVote.translate.tr("Lightvote created by XUPWUP, further developer by Xarqn, Tickleman")
			);
			sender.sendMessage(
				ChatColor.GOLD + LightVote.translate.tr("Lightvote version {version}")
				.replace("{version}", plugin.getDescription().getVersion())
			);
			sender.sendMessage(
				ChatColor.GOLD + LightVote.translate.tr("Static time is {time}")
				.replace("{time}", LightVote.translate.tr(plugin.config.perma ? "enabled" : "disabled"))
			);
			sender.sendMessage(
				ChatColor.GOLD + LightVote.translate.tr("Current time: {time} ({world})")
				.replace("{time}", (new Double(player.getWorld().getTime() % 24000)).toString())
				.replace("{world}", player.getWorld().getName())
			);
			sender.sendMessage(
				ChatColor.GOLD + LightVote.translate.tr("Bedvote is: " + (plugin.config.bedVote ? "on - sleep in a bed to vote for day." : "off."))
			);
			try {
				sender.sendMessage(
					ChatColor.GOLD + LightVote.translate.tr("Itemvote: vote for day - hit {itemyes} onto {hitsyes} for yes. {itemno} onto {hitsno} for no.")
					.replace("{itemyes}", plugin.config.bedVoteItemInHandDay.name())
					.replace("{hitsyes}", plugin.config.bedVoteItemHitsDay.name())
					.replace("{itemno}", plugin.config.bedVoteNoVoteItemInHandDay.name())
					.replace("{hitsno}", plugin.config.bedVoteNoVoteItemHitsDay.name())
						);
				sender.sendMessage(
					ChatColor.GOLD + LightVote.translate.tr("Itemvote: vote for night - hit {itemyes} onto {hitsyes} for yes. {itemno} onto {hitsno} for no.")
					.replace("{itemyes}", plugin.config.bedVoteItemInHandNight.name())
					.replace("{hitsyes}", plugin.config.bedVoteItemHitsNight.name())
					.replace("{itemno}", plugin.config.bedVoteNoVoteItemInHandNight.name())
					.replace("{hitsno}", plugin.config.bedVoteNoVoteItemHitsNight.name())
				);
			} catch (Exception e) {
				System.out.println("Exception on info");
			}
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
					startVote("night", sender);
				} else if (
					split[1].equalsIgnoreCase("sun")
					|| split[1].equalsIgnoreCase(LightVote.translate.tr("sun"))
				) {
					startVote("sun", sender);
				} else {
					startVote("day", sender);
				}
			} else {
				startVote("day", sender);
			}
			
			//long currenttime = currentWorld.getTime();				
						
			//startVote(this.dayVote, sender);
		} else if (
			split[0].equalsIgnoreCase("day")
			|| split[0].equalsIgnoreCase(LightVote.translate.tr("day"))
		) {
			if (!this.voting) {
				startVote("day", sender);
			} else {
				addToVote(this.dayVote, sender, this.dayVote == "day");
			}
		} else if (
			split[0].equalsIgnoreCase("night")
			|| split[0].equalsIgnoreCase(LightVote.translate.tr("night"))
		) {
			if (!this.voting) {
				startVote("night", sender);
			} else {
				addToVote(this.dayVote, sender, this.dayVote == "night");
			}
		} else if (
				split[0].equalsIgnoreCase("sun")
				|| split[0].equalsIgnoreCase(LightVote.translate.tr("sun"))
			) {
				if (!this.voting) {
					startVote("sun", sender);
				} else {
					addToVote(this.dayVote, sender, this.dayVote == "sun");
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
	
	public boolean addToVote(String voteWhat, CommandSender sender, boolean agreed) {
		if (!canJoinVote(sender)) {
			sender.sendMessage(ChatColor.GOLD + LightVote.translate.tr("You are not allowed to join votes."));
			return true;
		}
		
		if(sender instanceof Player) if (voters.contains(sender)){
			sender.sendMessage(ChatColor.GOLD + LightVote.translate.tr("You have already voted"));
			return true;
		}
		if (!voting){
			sender.sendMessage(
				ChatColor.GOLD + LightVote.translate.tr("'{yesno}' vote attempted but no votes in progress.")
				.replace("{yesno}", LightVote.translate.tr(agreed ? "Yes" : "No"))
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
		if (voters.size() == onlinePlayers(currentWorld).size()){// plugin.getServer().getOnlinePlayers().length){
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
		
	public boolean startVote(String voteWhat, CommandSender sender) {

		String daymsg = "";
		if (voteWhat.equals("day")) {
			daymsg = "for daylight";
		} else if (voteWhat.equals("night")) {
			daymsg = "for darkness";
		} else if (voteWhat.equals("sun")) {
			daymsg = "for sun";
		}

		String pname;
		
		this.dayVote = voteWhat;
		
		voters.clear();
		agrees = 1;

		if(sender instanceof Player) {
			pname =((Player) sender).getName();
			voters.add((Player) sender);
		}else {
			pname = "<CONSOLE>";
		}

		if(!canStartVote(sender)){
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
			if (voteWhat == "day"){
				sender.sendMessage(ChatColor.GOLD + LightVote.translate.tr("It is already day!"));
				return true;
			}
		}else{ // it is night now
			if (voteWhat == "night"){
				sender.sendMessage(ChatColor.GOLD + LightVote.translate.tr("It is already night!"));
				return true;
			}
		}
		if (voteWhat == "sun") {
			if (!currentWorld.hasStorm() && !currentWorld.isThundering()) {
				sender.sendMessage(ChatColor.GOLD + LightVote.translate.tr("The sun is already shining!"));
			}
		}

		// After all checks (vote in progress, permission, etc - set vote type to day or night
		this.dayVote = voteWhat;

		voting = true;
		plugin.sMdebug("Startvote detected... just before broadcast message.");

		for (Player player : onlinePlayers(currentWorld)) {
			player.sendMessage(
				ChatColor.GOLD + LightVote.translate.tr("Lightvote {daynight} in world '{world}' started by {player},")
				.replace("{daynight}", LightVote.translate.tr(daymsg))
				.replace("{world}", currentWorld.getName())
				.replace("{player}", pname)
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
		if (voters.size() == onlinePlayers(currentWorld).size()){
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

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerBedEnter (PlayerBedEnterEvent e)
	{
		if (plugin.config.bedVote) {
			Player player = e.getPlayer();
			currentWorld = player.getWorld();
			long currenttime = player.getWorld().getTime();
			//String[] commandArgs = {""};
			if (!voting) {
				startVote(isDay(currenttime) ? "night" : "day", player);
			} else {
				addToVote(isDay(currenttime) ? "night" : "day", player, true);
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
								startVote(isDay(currenttime) ? "night" : "day", player);
							} else {
								addToVote(isDay(currenttime) ? "night" : "day", player, true);
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
								addToVote(isDay(currenttime) ? "night" : "day", player, false);
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

	public Collection<Player> onlinePlayers(World world)
	{
		HashMap<String, Player> players = new HashMap<String, Player>();
		if (world == null) {
			return players.values();
		} else {
			for (Player player : world.getPlayers()) {
				if (player.isOnline()) players.put(player.getName(), player);
			}
			return players.values();
		}
	}

}
