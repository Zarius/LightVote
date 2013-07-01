package com.gmail.zariust.LightVote;

import java.util.HashMap;
import java.util.logging.Logger;

import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;

public class Log {

    final HashMap<Player, Boolean> debugees = new HashMap<Player, Boolean>();
    Logger logger = Logger.getLogger("Minecraft");

    void logWarning(LightVote lightVote, String msg) {
        logger.warning("[" + lightVote.getDescription().getName() + "] " + msg);
    }

    void logInfo(LightVote lightVote, String msg) {
        logger.info("[" + lightVote.getDescription().getName() + "] " + msg);
    }

    public boolean isDebugging(LightVote lightVote, final Player player) {
        if (debugees.containsKey(player)) {
            return debugees.get(player);
        } else {
            return false;
        }
    }

    public void setDebugging(LightVote lightVote, final Player player, final boolean value) {
        debugees.put(player, value);
    }

    public void sM(LightVote lightVote, String message) {
    	PluginDescriptionFile pdfFile = lightVote.getDescription();
        logger.info("[" + pdfFile.getName() + ":" + pdfFile.getVersion() + "] " + message);
    }

    public void sMdebug(LightVote lightVote, String message) {
    	if (lightVote.config.debugMessages) {
    		sM(lightVote, message);
    	}
    }

}
