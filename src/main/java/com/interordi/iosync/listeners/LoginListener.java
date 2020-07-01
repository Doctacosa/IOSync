package com.interordi.iosync.listeners;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

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

		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}


	@EventHandler
	public void onPlayerLogin(PlayerLoginEvent event) {

		System.out.println("> Loading inventory for " + event.getPlayer().getDisplayName());

		File source = new File(this.storagePath + event.getPlayer().getUniqueId() + ".dat");
		File dest = new File(this.serverPath + event.getPlayer().getUniqueId() + ".dat");

		if (Files.exists(source.toPath())) {
			try {
				Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
				System.out.println("> Done!");
			} catch (IOException e) {
				System.out.println("ERROR: Failed to copy file from storage");
				System.out.println("Reason: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}


	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {

		System.out.println("> Saving inventory for " + event.getPlayer().getDisplayName());

		//Avoid item duping
		plugin.getServer().savePlayers();

		File source = new File(this.serverPath + event.getPlayer().getUniqueId() + ".dat");
		File dest = new File(this.storagePath + event.getPlayer().getUniqueId() + ".dat");

		System.out.println("> Path: " + this.serverPath + event.getPlayer().getUniqueId() + ".dat");

		if (Files.exists(source.toPath())) {
			try {
				Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
				System.out.println("> Done!");
			} catch (IOException e) {
				System.out.println("ERROR: Failed to write file to storage");
				System.out.println("Reason: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}
}