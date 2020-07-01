package com.interordi.iosync.listeners;

import com.interordi.iosync.IOSync;
import com.interordi.iosync.Players;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

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
		Players.savePlayer(event.getPlayer());
	}
}