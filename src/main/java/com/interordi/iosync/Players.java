package com.interordi.iosync;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.bukkit.entity.Player;


public class Players {

	private static IOSync plugin;
	private static String storagePath;
	private static String serverPath;
	
	
	public static void init(IOSync plugin, String storagePath, String serverPath) {
		Players.plugin = plugin;
		Players.storagePath = storagePath;
		Players.serverPath = serverPath;

	}


	public static void loadPlayer(Player player) {

		System.out.println("> Loading inventory for " + player.getDisplayName());

		File source = new File(storagePath + player.getUniqueId() + ".dat");
		File dest = new File(serverPath + player.getUniqueId() + ".dat");

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


	public static void savePlayer(Player player) {

		System.out.println("> Saving inventory for " + player.getDisplayName());

		//Avoid item duping
		plugin.getServer().savePlayers();

		File source = new File(serverPath + player.getUniqueId() + ".dat");
		File dest = new File(storagePath + player.getUniqueId() + ".dat");

		System.out.println("> Path: " + serverPath + player.getUniqueId() + ".dat");

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