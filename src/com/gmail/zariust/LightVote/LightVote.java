package com.gmail.zariust.LightVote;


import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.gmail.zariust.LightVote.config.ConfigManager;
import com.gmail.zariust.LightVote.metrics.Metrics;

import fr.crafter.tickleman.RealPlugin.RealTranslationFile;


/**
 * LightVote for Bukkit
 *
 * @author XUPWUP
 */
public class LightVote extends JavaPlugin {
    public static Metrics metrics = null;
    
	public static RealTranslationFile translate;
	private LVTPlayerListener playerListener;
    private final HashMap<Player, Boolean> debugees = new HashMap<Player, Boolean>();
	private Logger log;
	public LVTConfig config;
	public ConfigManager configManager;

    void logWarning(String msg) {
		log.warning("["+getDescription().getName()+"] "+msg);		
	}
	void logInfo(String msg) {
		log.info("["+getDescription().getName()+"] "+msg);
	}
	
    @Override
    public void onEnable() { 
    	log = Logger.getLogger("Minecraft");
        registerListeners();
    	registerEvents();		
        loadConfig();
        loadLanguageFile();

        if(config.perma)
            setPermanentTime();
        
        if (config.enableMetrics)
            enableMetrics();
    }

    @Override
    public void onDisable() {
    	if (playerListener.tReset != null) playerListener.tReset.cancel();
    
        // NOTE: All registered events are automatically unregistered when a plugin is disabled
    
        sM("Disabled");
    }

    public void registerListeners() {
        playerListener = new LVTPlayerListener(this);
    }

    public void registerEvents() {
        // Register event for beds
    	PluginManager pm = this.getServer().getPluginManager();
    	pm.registerEvents(playerListener, this);
    }

    public void loadConfig() {
        ConfigManager.load(this);
    }

    public void loadLanguageFile() {
        LightVote.translate = new RealTranslationFile(this, config.language).load();
        sM(LightVote.translate.tr("Language is " + config.language));
    }

    public void setPermanentTime() {
        playerListener.setReset();
    }

    public void enableMetrics() {
        try {
            metrics = new Metrics(this);
            metrics.start();
        } catch (IOException e) {
            // Failed to submit the stats :-(
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
    		String label, String[] args) {

    	return playerListener.onPlayerCommand(sender, command, label, args);
    }

    public boolean isDebugging(final Player player) {
        if (debugees.containsKey(player)) {
            return debugees.get(player);
        } else {
            return false;
        }
    }

    public void setDebugging(final Player player, final boolean value) {
        debugees.put(player, value);
    }

    public void sM(String message) {
    	PluginDescriptionFile pdfFile = getDescription();
    	log.info("[" + pdfFile.getName() + ":" + pdfFile.getVersion() + "] " + message);
    }

    public void sMdebug(String message) {
    	if (config.debugMessages) {
    		sM(message);
    	}
    }
}
