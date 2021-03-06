package com.gmail.zariust.LightVote.votes;

import java.util.Collection;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import com.gmail.zariust.LightVote.LightVote;
import com.gmail.zariust.LightVote.PermanentTime;
import com.gmail.zariust.LightVote.Utils;
//import java.util.Set;

/**
 * Handle events for all Player related events
 * @author XUPWUP
 */
public class VoteManager implements Listener {
    private final LightVote plugin;
    //private Logger log;

	public VoteManager(LightVote instance) {
        plugin = instance;
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
    public String dayVote = "day";
	private int remindCounter = 0;
	private boolean disabled = false;
    public boolean voting = false;
    public World currentWorld = null;
	
	private HashSet<Player> voters = new HashSet<Player>();
	
	Timer t = new Timer();
	Timer reminder;
	
	
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
			
			for (Player player : Utils.onlinePlayers(currentWorld)) {
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
		plugin.log.sMdebug(plugin, "Starting endvote...");
		Collection<Player> playerlist = Utils.onlinePlayers(currentWorld);
		plugin.log.sMdebug(plugin, "Endvote: got players...");
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
		for (Player player : Utils.onlinePlayers(plugin.getServer().getWorld("world_skylands"))) {
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
						plugin.log.sM(plugin, "LVT: Current time was negative!");
					}

					if (dayVote == "night") currenttime += PermanentTime.nightstart;
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
					plugin.log.sM(plugin, "LVT: changed time to "+ (dayVote));
				}
				else {
					Integer disagrees = voters.size() - agrees;
					msg = LightVote.translate.tr("Vote failed. ({agrees} yes, {disagrees} no)")
					.replace("{agrees}", agrees.toString())
					.replace("{disagrees}", disagrees.toString());
					plugin.log.sM(plugin, "LVT: vote failed (" + voters.size() + " votes, "+ agrees + " agree)");
				}
			}else{
				Double numReqYesVotes = Math.ceil(numplayers * reqYesVotes * 100) / 100;
				msg = LightVote.translate.tr("Vote failed, insufficient \"yes\" votes. ({agrees}/{required})")
				.replace("{agrees}", agrees.toString())
				.replace("{required}", numReqYesVotes.toString());
				plugin.log.sM(plugin, "LVT: vote failed, insufficient votes (" + agrees + " yes votes, "+ numplayers + " players, req " + (numplayers * reqYesVotes)+ ")");
			}
		}
		
		plugin.log.sMdebug(plugin, "Endvote: checked status, broadcasting message...");

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
		if (voters.size() == Utils.onlinePlayers(currentWorld).size()){// plugin.getServer().getOnlinePlayers().length){
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
		
		if (Utils.isDay(currentWorld.getTime())){ // it is day now
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
		plugin.log.sMdebug(plugin, "Startvote detected... just before broadcast message.");

		for (Player player : Utils.onlinePlayers(currentWorld)) {
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
		if (voters.size() == Utils.onlinePlayers(currentWorld).size()){
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

}
