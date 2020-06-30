package com.interordi.iosync;

import org.bukkit.plugin.java.JavaPlugin;

import com.interordi.iosync.listeners.LoginListener;

public class IOSync extends JavaPlugin {

	LoginListener thisLoginListener;


	public void onEnable() {

		//TODO: Configurable file paths
		String storagePath = "C:\\Jeux\\Minecraft\\players\\";
		String serverPath = "C:\\Jeux\\Minecraft\\server-tasmantis\\";

		thisLoginListener = new LoginListener(this, storagePath, serverPath);

		getLogger().info("IOSync enabled");
	}

	public void onDisable() {
		getLogger().info("IOSync disabled");
	}
	
}