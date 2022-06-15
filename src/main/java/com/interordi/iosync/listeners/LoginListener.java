package com.interordi.iosync.listeners;

import com.interordi.iosync.IOSync;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
		plugin.getPlayersInst().loadPlayer(event.getPlayer(), enablePositionSaving);
	}


	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		//Save all players to ensure periodic safety saves
		plugin.getPlayersInst().saveAllPlayers();
		//Players.savePlayerData(event.getPlayer());
	}
}