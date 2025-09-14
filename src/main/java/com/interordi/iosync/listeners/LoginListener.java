package com.interordi.iosync.listeners;

import com.interordi.iosync.IOSync;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;


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


	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerQuit(PlayerQuitEvent event) {
		//Save all players to ensure periodic safety saves
		Location pos = event.getPlayer().getBedSpawnLocation();
		plugin.getPlayersInst().setPlayerSpawn(event.getPlayer(), pos);

		plugin.getPlayersInst().saveAllPlayers();
		//Players.savePlayerData(event.getPlayer());
	}


	@EventHandler(priority=EventPriority.LOWEST)
	public void onPlayerSpawnLocationEvent(PlayerSpawnLocationEvent event) {
		Location pos = plugin.getPlayersInst().getPlayerPosition(event.getPlayer().getUniqueId());
		if (pos != null && plugin.getServer().getWorlds().contains(pos.getWorld()))
			event.setSpawnLocation(pos);
		else if (enablePositionSaving)
			event.setSpawnLocation(event.getPlayer().getWorld().getSpawnLocation());
	}
}