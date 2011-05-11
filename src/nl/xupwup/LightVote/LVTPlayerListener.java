package nl.xupwup.LightVote;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerListener;

/**
 * Handle events for all Player related events
 * @author XUPWUP
 */
public class LVTPlayerListener extends PlayerListener {
    private final LightVote plugin;
    private Logger log;

	private double reqYesVotes, minAgree;
	private int permaOffset; 
	private int voteTime, voteFailDelay, votePassDelay, voteRemindCount;
	private boolean perma, bedVote;
	private static final int nightstart = 14000;
	private Set<String> canStartVotes = null;
    
    public LVTPlayerListener(LightVote instance, Logger log) {
        plugin = instance;
        this.log = log;
    }
	
    
    public void config(double reqYesVotes, double minAgree, int permaOffset, int voteTime, int voteFailDelay, int votePassDelay, int voteRemindCount, boolean perma, Set<String> set, boolean bedVote){
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
    }
    
    private int agrees = 0;
	private boolean day = true;
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
			currenttime += permaOffset;
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
			int timeBetween = voteTime / (voteRemindCount+1);
			remindCounter++;
			if (remindCounter > voteRemindCount) {
				reminder.cancel();
				return;
			}
			
			plugin.getServer().broadcastMessage(ChatColor.GOLD + "Vote for " + (day ? "day" : "night") + ", " + (voteTime - remindCounter*timeBetween)/1000 + " seconds remaining.");
		}
	}	
	
	private void endVote(){
		plugin.sMdebug("Starting endvote...");
		List<Player> playerlist = currentWorld.getPlayers();
		plugin.sMdebug("Endvote: got players...");
		String msg = "";
		boolean passed = false;
		
		int numplayers = playerlist.size();
		
		if (voters.size() > numplayers * reqYesVotes){
			if (agrees > minAgree * voters.size()) {
				msg = "Vote passed. (" + agrees + " yes, " + (voters.size() - agrees) + " no)";
				long currenttime = currentWorld.getTime();
				currenttime = currenttime - (currenttime % 24000); // one day lasts 24000
				
				if (currenttime < 0){
					currenttime *= -1;
					plugin.sM("LVT: Current time was negative!");
				}
				
				if (!day) currenttime += nightstart;
				if(perma) currenttime += permaOffset;
				
				currentWorld.setTime(currenttime);
				passed = true;
				plugin.sM("LVT: changed time to "+ (day ? "day" : "night"));
			}
			else {
				msg = "Vote failed. (" + agrees + " yes, " + (voters.size() - agrees) + " no)";
				plugin.sM("LVT: vote failed (" + voters.size() + " votes, "+ agrees + " agree)");
			}
		}else{
			msg = "Vote failed, insufficient \"yes\" votes. (" + agrees + "/" + (numplayers * reqYesVotes) + ")";
			plugin.sM("LVT: vote failed, insufficient votes (" + agrees + " yes votes, "+ numplayers + " players, req " + (numplayers * reqYesVotes)+ ")");
		}
		
		plugin.sMdebug("Endvote: checked status, broadcasting message...");

		plugin.getServer().broadcastMessage(ChatColor.GOLD + msg);
		agrees = 0;
		voters = new HashSet<Player>();
		voting = false;
		disabled = true;
		Timer reenable = new Timer();
		reenable.schedule(new reEnable(), (passed ? votePassDelay : voteFailDelay));
	}
	
	public boolean canSVote(CommandSender sender){
		if(sender instanceof Player) {
			return canStartVotes == null || canStartVotes.contains(((Player) sender).getName().toLowerCase());
		}else return true;
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

		if (split.length == 0 || (split.length == 1 && split[0].equalsIgnoreCase("help"))){
			sender.sendMessage(ChatColor.GOLD + "Lightvote commands");
			if(canSVote(sender)) {
				sender.sendMessage(ChatColor.GOLD + "/lvt start -- start a vote(for day)");
				sender.sendMessage(ChatColor.GOLD + "/lvt start night -- start a vote for night");
			}
			sender.sendMessage(ChatColor.GOLD + "/lvt yes/no -- vote");
			sender.sendMessage(ChatColor.GOLD + "/lvt help -- this message");
			sender.sendMessage(ChatColor.GOLD + "/lvt info -- some information");
			return true;
		}
		
		if(split[0].equalsIgnoreCase("info")){
			sender.sendMessage(ChatColor.GOLD + "Lightvote created by XUPWUP");
			sender.sendMessage(ChatColor.GOLD + "Lightvote version " + plugin.getDescription().getVersion());
			sender.sendMessage(ChatColor.GOLD + "Static time is " + (perma ? "enabled" : "disabled"));
			sender.sendMessage(ChatColor.GOLD + "Current time:" + player.getWorld().getTime()%24000 + " ("+player.getWorld().getName()+")");
			return true;
		}
		
		if (split[0].equalsIgnoreCase("start")){
			if(!canSVote(sender)){
				sender.sendMessage(ChatColor.GOLD + "You are not allowed to start votes.");
				return true;
			}
			if (voting) {
				sender.sendMessage(ChatColor.GOLD + "A vote is still in progress.");
				return true;
			}
			
			if (disabled){
				sender.sendMessage(ChatColor.GOLD + "You cannot vote again this quickly.");
				return true;
			}
			
			if (split.length > 1){
				if (split[1].equalsIgnoreCase("night")) day = false;
				else day = true;
			}else day = true;
			
			long currenttime = currentWorld.getTime();				
			
			if (isDay(currenttime)){ // it is day now
				if (day){
					sender.sendMessage(ChatColor.GOLD + "It is already day!");
					return true;
				}
			}else{ // it is night now
				if (!day){
					sender.sendMessage(ChatColor.GOLD + "It is already night!");
					return true;
				}
			}
			
			String daymsg = "";
			if (day){
				daymsg = "for daylight";
			}else daymsg = "for darkness";
			
			String pname;
			if(sender instanceof Player) {
				pname =((Player) sender).getName();
				voters.add((Player) sender);
				agrees++;
			}else pname = "<CONSOLE>";
			voting = true;
			plugin.getServer().broadcastMessage(ChatColor.GOLD + "Lightvote " + daymsg + " in world '"+currentWorld.getName()+"' started by "+ pname + ",");
			plugin.getServer().broadcastMessage(ChatColor.GOLD + "type /lvt yes, or /lvt no to vote.");
			
			t.schedule(new voteEnd(), voteTime);
			if (voters.size() == currentWorld.getPlayers().size()){
				t.cancel();
				t = new Timer();
				endVote();
				return true;
			}
			
			reminder = new Timer();
			
			if (voteRemindCount > 0){
				remindCounter = 0;
				int timeBetween = voteTime / (voteRemindCount+1);
				reminder.schedule(new remind(), timeBetween, timeBetween);
			}
			return true;
		}else{
			if(sender instanceof Player) if (voters.contains((Player) sender)){
				sender.sendMessage(ChatColor.GOLD + "You have already voted");
				return true;
			}
			if (!voting){
				sender.sendMessage(ChatColor.GOLD + "No votes in progress, use /lvt start to start a vote");
				return true;
			}
			
			boolean agreed = false;
			if (split[0].equalsIgnoreCase("yes") || split[0].equalsIgnoreCase("y")) {
				agrees++;
				agreed = true;
			}else if (! (split[0].equalsIgnoreCase("no") || split[0].equalsIgnoreCase("n"))) {
				sender.sendMessage(ChatColor.GOLD + "Invalid answer \"" + split[0] + "\", please use \"y\", or \"n\"");
				return true;
			}
			
			if(sender instanceof Player) voters.add((Player)sender);
			if (voters.size() == currentWorld.getPlayers().size()){// plugin.getServer().getOnlinePlayers().length){
				t.cancel();
				t = new Timer();
				reminder.cancel();
				endVote();
			}
			sender.sendMessage(ChatColor.GOLD + "Thanks for voting! (" + (agreed ? "yes" : "no") + ")");
		}
		return true;
	}

	public void onPlayerBedEnter (PlayerBedEnterEvent e)
	{
		if (this.bedVote) {
			Player player = e.getPlayer();
			String[] commandArgs = {""};
			if (!voting) {
				commandArgs[0]="start";
			} else {
				commandArgs[0] = "yes";
			}
			player.sendMessage(ChatColor.GOLD + "Sleeping, attempting to vote for day time...");
			onPlayerCommand(player, null, String.valueOf("lvt"), commandArgs);
		}
	}
}