package com.gmail.zariust.LightVote;

import org.bukkit.plugin.java.JavaPlugin;

import com.gmail.zariust.LightVote.config.ConfigManager;
import com.gmail.zariust.LightVote.metrics.Metrics;
import com.gmail.zariust.LightVote.votes.VoteManager;

import fr.crafter.tickleman.RealPlugin.RealTranslationFile;

/**
 * LightVote for Bukkit
 *
 * @author XUPWUP
 */
public class LightVote extends JavaPlugin {
    public static Metrics metrics = null;
    public static RealTranslationFile translate;
    public Log log = new Log();
    public static LVTConfig config;
    public ConfigManager configManager;
    public VoteManager voteManager = new VoteManager(this);

    @Override
    public void onEnable() { 
        ConfigManager.load(this);
        Dependencies.init(this);
        loadLanguageFile();
        registerListeners();
        registerCommands();
        PermanentTime.init();
    }

    @Override
    public void onDisable() {
        PermanentTime.cancel();
    }

    public void registerListeners() {
        this.getServer().getPluginManager().registerEvents(new LVTPlayerListener(this), this);
    }

    private void registerCommands() {
        this.getCommand("lvt").setExecutor(new CommandManager(this));
    }

    public void loadLanguageFile() {
        LightVote.translate = new RealTranslationFile(this, config.language).load();
        log.sM(this, LightVote.translate.tr("Language is " + config.language));
    }

}
