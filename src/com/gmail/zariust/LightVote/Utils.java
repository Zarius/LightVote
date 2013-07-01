package com.gmail.zariust.LightVote;

import java.util.Collection;
import java.util.HashMap;

import org.bukkit.World;
import org.bukkit.entity.Player;

public class Utils {

    public static boolean isDay(long currenttime) {
    	return ((currenttime % 24000) < 12000 && currenttime > 0 )|| (currenttime < 0 && (currenttime % 24000) < -12000);
    }

    public static Collection<Player> onlinePlayers(World world)
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
