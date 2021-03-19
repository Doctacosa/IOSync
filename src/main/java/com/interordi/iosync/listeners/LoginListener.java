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

	IOSync plugin;
	boolean enablePositionSaving = false;
	
	
	public LoginListener(IOSync plugin, boolean enablePositionSaving) {
		this.plugin = plugin;
		this.enablePositionSaving = enablePositionSaving;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}


	@EventHandler
	public void onPlayerLogin(PlayerLoginEvent event) {
		plugin.getPlayersInst().loadPlayer(event.getPlayer());
	}


	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		//Save all players to ensure periodic safety saves
		plugin.getPlayersInst().saveAllPlayers();
		//Players.savePlayer(event.getPlayer());
	}


	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		//On login, return the player to his last position
		Location pos = plugin.getPlayersInst().getPlayerPosition(event.getPlayer().getUniqueId());
		if (pos != null)
			event.getPlayer().teleport(pos);
		else if (enablePositionSaving)
			event.getPlayer().teleport(event.getPlayer().getWorld().getSpawnLocation());
	}
}