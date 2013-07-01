package com.gmail.zariust.LightVote;


import java.io.IOException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
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
    public Log log;
	public LVTConfig config;
	public ConfigManager configManager;

    @Override
    public void onEnable() { 
        registerListeners();
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
    }

    public void registerListeners() {
        playerListener = new LVTPlayerListener(this);
        // Register event for beds
        PluginManager pm = this.getServer().getPluginManager();
        pm.registerEvents(playerListener, this);
    }

    public void loadConfig() {
        ConfigManager.load(this);
    }

    public void loadLanguageFile() {
        LightVote.translate = new RealTranslationFile(this, config.language).load();
        log.sM(this, LightVote.translate.tr("Language is " + config.language));
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
}
