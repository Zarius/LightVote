package nl.xupwup.LightVote;

import java.util.HashSet;
//import java.util.Set;

import org.bukkit.Material;

public class LVTConfig {
	public Material bedVoteItem = null;
	public double reqYesVotesDay = 0.05;
	public double minAgreeDay = 0.5;
	public double reqYesVotesNight = 0.05;
	public double minAgreeNight = 0.5;
	public int permaOffset = 4000; 
	public int voteTime = 30000;
	public int voteFailDelay = 30000;
	public int votePassDelay = 50000;
	public int voteRemindCount = 2;
	public boolean lightVoteNoCommands = false;
	public boolean bedVote = false;
	public boolean itemVote = true;
	public Material bedVoteItemInHandDay = Material.getMaterial("TORCH");
	public Material bedVoteItemHitsDay = Material.getMaterial("BED_BLOCK");
	public Material bedVoteItemInHandNight = Material.getMaterial("TORCH");
	public Material bedVoteItemHitsNight = Material.getMaterial("BED_BLOCK");
	public Material bedVoteNoVoteItemInHandDay = Material.getMaterial("TORCH");
	public Material bedVoteNoVoteItemHitsDay = Material.getMaterial("DIRT");
	public Material bedVoteNoVoteItemInHandNight = Material.getMaterial("TORCH");
	public Material bedVoteNoVoteItemHitsNight = Material.getMaterial("DIRT");
	public boolean perma = false;
	public boolean debugMessages = false;
	public HashSet<String> canStartVotes = null;
	
	public String msgAlreadyVoted = "You have already voted";
	public String msgVoteAcknowledgement = "Thanks for voting! (%yesno)";
}
