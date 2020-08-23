package com.interordi.iosync.listeners;

import com.interordi.iosync.IOSync;
import com.interordi.iosync.Players;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;


public class LoginListener implements Listener {

	
	public LoginListener(IOSync plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}


	@EventHandler
	public void onPlayerLogin(PlayerLoginEvent event) {
		Players.loadPlayer(event.getPlayer());
	}


	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		//Save all players to ensure periodic safety saves
		Players.saveAllPlayers();
		//Players.savePlayer(event.getPlayer());
	}


	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		//On login, return the player to his last position
		Location pos = Players.getPlayerPosition(event.getPlayer().getUniqueId());
		if (pos != null)
			event.getPlayer().teleport(pos);
		else
			event.getPlayer().teleport(event.getPlayer().getWorld().getSpawnLocation());
	}
}