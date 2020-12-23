package com.interordi.iosync.listeners;

import com.interordi.iosync.IOSync;
import com.interordi.iosync.Players;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;


public class SpawnListener implements Listener {

	boolean enablePositionSaving = false;
	
	public SpawnListener(IOSync plugin, boolean enablePositionSaving) {
		this.enablePositionSaving = enablePositionSaving;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}


	@EventHandler
	public void onPlayerBedEnter(PlayerBedEnterEvent event) {
		Players.setPlayerSpawn(event.getPlayer());
	}


	@EventHandler
	public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
		Players.setPlayerSpawn(event.getPlayer());
	}


	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		Location respawnLocation = Players.getPlayerSpawn(event.getPlayer().getUniqueId());
		if (respawnLocation != null)
			event.setRespawnLocation(respawnLocation);
	}

}