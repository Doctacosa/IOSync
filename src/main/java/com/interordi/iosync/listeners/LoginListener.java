package com.interordi.iosync.listeners;

import com.interordi.iosync.IOSync;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class LoginListener implements Listener {

	private IOSync plugin;
	private String storagePath;
	private String serverPath;
	
	
	public LoginListener(IOSync plugin, String storagePath, String serverPath) {
		this.plugin = plugin;
		this.storagePath = storagePath;
		this.serverPath = serverPath;
	}


	@EventHandler
	public void onPlayerLogin(PlayerLoginEvent event) {
	}


	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
	}
}