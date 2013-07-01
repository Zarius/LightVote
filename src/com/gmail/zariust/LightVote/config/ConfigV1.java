package com.gmail.zariust.LightVote.config;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Scanner;

import org.bukkit.Material;

import com.gmail.zariust.LightVote.LVTConfig;
import com.gmail.zariust.LightVote.LightVote;

public class ConfigV1 {
    LightVote parent = null;

    public ConfigV1(LightVote parent) {
        this.parent = parent;
    }
    // default configuration
    //private double reqYesVotes = 0.05, minAgree = 0.5;
    //private int permaOffset = 4000; 
    //private int voteTime = 30000, voteFailDelay = 30000, votePassDelay = 50000, voteRemindCount = 2;
    //private boolean bedVote = false;
    //private boolean perma = false;
    //private boolean debugMessages;
    public static final String defaultConfig = 
    	"language fr\n" +
    	"# At least 'required-yes-percentage'*peopleOnServer people must vote yes, " +
    	"and there must be more people that voted yes than no" + '\n' + 
    	"# day" + '\n' +
    	"required-yes-percentage-day 5" + '\n' +
     	"minimum-agree-percentage-day 50" + '\n' +
    	"# night" + '\n' +
     	"required-yes-percentage-night 5" + '\n' +
     	"minimum-agree-percentage-night 50" + '\n' +
    	"# sun" + '\n' +
     	"required-yes-percentage-sun 5" + '\n' +
     	"minimum-agree-percentage-sun 50" + '\n' +
    	"vote-fail-delay 30" + '\n' +
    	"vote-pass-delay 50" + '\n' +
    	"vote-time 30" + '\n' +
    	"reminders 2" + '\n' +
    	"# enable bedvote (sleeping in a bed starts or agrees with a vote)" + '\n' +
    	"bedvote yes" + '\n' +
    	"# no commands - if this is enabled then all lightvote voting commands are disabled (bedvote or itemvote must be used)" + '\n' +
    	"lightvote-nocommands no" + '\n' +
    	"##### Configure items for commandless voting (hitting 'itemhits' with 'iteminhand' starts/agrees/disagrees with a vote)" + '\n' +
    	"# These are for starting a vote or agreeing to a vote in progress (day and night can be different if you want)" + '\n' +
    	"itemvote yes" + '\n' +
    	"bedvote-iteminhand-day TORCH" + '\n' +
    	"bedvote-itemhits-day BED_BLOCK" + '\n' +
    	"bedvote-iteminhand-night TORCH" + '\n' +
    	"bedvote-itemhits-night BED_BLOCK" + '\n' +
    	"# These are for disagreeing (voting no) with a vote in progress)" + '\n' +
    	"bedvote-novote-iteminhand-day TORCH" + '\n' +
    	"bedvote-novote-itemhits-day DIRT" + '\n' +
    	"bedvote-novote-iteminhand-night TORCH" + '\n' +
    	"bedvote-novote-itemhits-night DIRT" + '\n' +
    	"debug-messages no" + '\n' +
    	"use-permissions no" + '\n' +
    	"permanent no" + '\n' +
        "perma-offset 4000" + '\n' +
    	"enable-metrics yes";
    
    public File voteStartersFile;
    
    
    public void loadConfig(LightVote lightVote) {
        lightVote.config = new LVTConfig();
        File folder = new File("plugins" + File.separator + "LightVote");
        if (!folder.exists()) {
            folder.mkdir();
        }
        
        File configFile = new File(folder.getAbsolutePath() + File.separator +"LightVote.conf");
        if (configFile.exists()){
            lightVote.log.sM(lightVote, "Scanning properties file.");
            Scanner sc = null;
            try {
                sc = new Scanner(configFile);
                parseSettings(this, lightVote, sc);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }else{
            lightVote.log.sM(lightVote, "Creating properties file.");
            BufferedWriter out = null;
            try {
                configFile.createNewFile();
                out = new BufferedWriter(new FileWriter(configFile));
                out.write(ConfigV1.defaultConfig);
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        lightVote.log.sM(lightVote, "Properties loaded.  Debug messages: " +lightVote.config.debugMessages);
        
        voteStartersFile = new File(folder.getAbsolutePath() + File.separator +"voteStarters.conf");
        if(voteStartersFile.exists()){
            updateVoters(voteStartersFile);
        }else{
            BufferedWriter out = null;
            try {
                voteStartersFile.createNewFile();
                out = new BufferedWriter(new FileWriter(voteStartersFile));
                out.write("*");
                out.close();
                lightVote.config.canStartVotes = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    
    public void parseSettings(ConfigV1 config, LightVote lightVote, Scanner sc){ 
    	while(sc.hasNext()){
    		String thisline = sc.nextLine();
    		String[] contents = thisline.split(" ");
    		if (contents.length > 1){
    			if (contents[0].equals("language")){
    				lightVote.config.language = contents[1];
    			}else if (contents[0].equals("minimum-agree-percentage-day")){
    					lightVote.config.minAgreeDay = Double.parseDouble(contents[1]);
    					lightVote.config.minAgreeDay /= 100.0;
    			}else if (contents[0].equals("required-yes-percentage-day")){
    				lightVote.config.reqYesVotesDay = Double.parseDouble(contents[1]);
    				lightVote.config.reqYesVotesDay /= 100.0;
    			}else if (contents[0].equals("minimum-agree-percentage-night")){
    					lightVote.config.minAgreeNight = Double.parseDouble(contents[1]);
    					lightVote.config.minAgreeNight /= 100.0;
    //System.out.println("minAgreeNight = " + config.minAgreeNight);
    			}else if (contents[0].equals("required-yes-percentage-night")){
    				lightVote.config.reqYesVotesNight = Double.parseDouble(contents[1]);
    				lightVote.config.reqYesVotesNight /= 100.0;
    			}else if (contents[0].equals("minimum-agree-percentage-sun")){
    				lightVote.config.minAgreeSun = Double.parseDouble(contents[1]);
    				lightVote.config.minAgreeSun /= 100.0;
    			}else if (contents[0].equals("required-yes-percentage-sun")){
    				lightVote.config.reqYesVotesSun = Double.parseDouble(contents[1]);
    				lightVote.config.reqYesVotesSun /= 100.0;
    			}else if (contents[0].equals("vote-fail-delay")){
    				lightVote.config.voteFailDelay = Integer.parseInt(contents[1]) * 1000;
    			}else if (contents[0].equals("vote-pass-delay")){
    				lightVote.config.votePassDelay = Integer.parseInt(contents[1]) * 1000;
    			}else if (contents[0].equals("vote-time")){
    				lightVote.config.voteTime = Integer.parseInt(contents[1]) * 1000;
    			}else if (contents[0].equals("reminders")){
    				lightVote.config.voteRemindCount = Integer.parseInt(contents[1]);
    			}else if (contents[0].equals("permanent")){
    				lightVote.config.perma = contents[1].equals("yes");
                }else if (contents[0].equals("perma-offset")){
                    lightVote.config.permaOffset = Integer.parseInt(contents[1]);
    			}else if (contents[0].equals("bedvote")){
    				lightVote.config.bedVote = contents[1].equals("yes");
    			}else if (contents[0].equals("itemvote")){
    				lightVote.config.itemVote = contents[1].equals("yes");
    			}else if (contents[0].equals("lightvote-nocommands")){
    				lightVote.config.lightVoteNoCommands = contents[1].equals("yes");
    			}else if (contents[0].equals("bedvote-iteminhand-day")){
    				lightVote.config.bedVoteItemInHandDay = Material.getMaterial(contents[1]);
    				if (lightVote.config.bedVoteItemInHandDay == null) System.out.println("Bad material 1 : " + contents[1]);
    			}else if (contents[0].equals("bedvote-itemhits-day")){
    				lightVote.config.bedVoteItemHitsDay = Material.getMaterial(contents[1]);
    				if (lightVote.config.bedVoteItemHitsDay == null) System.out.println("Bad material 1 : " + contents[1]);
    			}else if (contents[0].equals("bedvote-iteminhand-night")){
    				lightVote.config.bedVoteItemInHandNight = Material.getMaterial(contents[1]);
    				if (lightVote.config.bedVoteItemInHandNight == null) System.out.println("Bad material 1 : " + contents[1]);
    			}else if (contents[0].equals("bedvote-itemhits-night")){
    				lightVote.config.bedVoteItemHitsNight = Material.getMaterial(contents[1]);
    				if (lightVote.config.bedVoteItemHitsNight == null) System.out.println("Bad material 1 : " + contents[1]);
    
    			}else if (contents[0].equals("bedvote-novote-iteminhand-day")){
    				lightVote.config.bedVoteNoVoteItemInHandDay = Material.getMaterial(contents[1]);
    				if (lightVote.config.bedVoteNoVoteItemInHandDay == null) System.out.println("Bad material 1 : " + contents[1]);
    			}else if (contents[0].equals("bedvote-novote-itemhits-day")){
    				lightVote.config.bedVoteNoVoteItemHitsDay = Material.getMaterial(contents[1]);
    				if (lightVote.config.bedVoteNoVoteItemHitsDay == null) System.out.println("Bad material 1 : " + contents[1]);
    			}else if (contents[0].equals("bedvote-novote-iteminhand-night")){
    				lightVote.config.bedVoteNoVoteItemInHandNight = Material.getMaterial(contents[1]);
    				if (lightVote.config.bedVoteNoVoteItemInHandNight == null) System.out.println("Bad material 1 : " + contents[1]);
    			}else if (contents[0].equals("bedvote-novote-itemhits-night")){
    				lightVote.config.bedVoteNoVoteItemHitsNight = Material.getMaterial(contents[1]);
    				if (lightVote.config.bedVoteNoVoteItemHitsNight == null) System.out.println("Bad material 1 : " + contents[1]);
    			}else if (contents[0].equals("debug-messages")){
    				lightVote.config.debugMessages = contents[1].equals("yes");
    			}else if (contents[0].equals("use-permissions")){
    				lightVote.config.usePermissions = contents[1].equals("yes");
    			}else if (contents[0].equals("enable-metrics")){
                    lightVote.config.usePermissions = contents[1].equals("yes");
    			}
    		}
    	}
    }

    public void updateVoters(File f) {
        try {
            Scanner sc = new Scanner(f);
            parent.config.canStartVotes = new HashSet<String>();
            while (sc.hasNext()) {
                String name = sc.next();
                if (name.equals("*")) {
                    parent.config.canStartVotes = null;
                    return;
                }
                parent.config.canStartVotes.add(name.toLowerCase());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

}
