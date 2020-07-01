package com.interordi.iosync;

import org.bukkit.plugin.java.JavaPlugin;

import com.interordi.iosync.listeners.LoginListener;

public class IOSync extends JavaPlugin {

	LoginListener thisLoginListener;


	public void onEnable() {

		//Always ensure we've got a copy of the config in place (does not overwrite existing)
		this.saveDefaultConfig();

		//Get the paths to copy to and from
		String storagePath = this.getConfig().getString("storage-path");
		String serverPath = this.getConfig().getString("server-path");
		if (!storagePath.endsWith("/") && !storagePath.endsWith("\\"))
			storagePath += "/";
		if (!serverPath.endsWith("/") && !serverPath.endsWith("\\"))
			serverPath += "/";

		thisLoginListener = new LoginListener(this, storagePath, serverPath);

		getLogger().info("IOSync enabled");
	}

	public void onDisable() {
		getLogger().info("IOSync disabled");
	}
	
}