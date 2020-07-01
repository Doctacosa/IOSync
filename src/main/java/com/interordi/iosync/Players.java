package com.interordi.iosync;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;


public class Players {

	private static IOSync plugin;
	private static String storagePath;
	private static String serverPath;

	private static String positionsPath = "plugins/IOSync/positions.yml";
	private static Map< UUID, Location > posPlayers;
	
	
	public static void init(IOSync plugin, String storagePath, String serverPath) {
		Players.plugin = plugin;
		Players.storagePath = storagePath;
		Players.serverPath = serverPath;

		posPlayers = new HashMap< UUID, Location >();
	}


	//Load a player's inventory from storage
	public static void loadPlayer(Player player) {

		if (storagePath.isEmpty() || serverPath.isEmpty())
			return;

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


	//Save a player's inventory to storage
	public static void savePlayer(Player player) {

		setPlayerPosition(player);

		if (storagePath.isEmpty() || serverPath.isEmpty())
			return;

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


	//Get the positions of all players
	public static void loadPositions() {
		
		File statsFile = new File(positionsPath);
		FileConfiguration statsAccess = YamlConfiguration.loadConfiguration(statsFile);
		
		ConfigurationSection posData = statsAccess.getConfigurationSection("positions");
		if (posData == null) {
			plugin.getLogger().info("ERROR: Positions YML section not found");
			return;	//Nothing yet, exit
		}
		Set< String > cs = posData.getKeys(false);
		if (cs == null) {
			plugin.getLogger().info("ERROR: Couldn't get player keys");
			return;	//No players found, nothing to do
		}

		
		//Loop on each player
		for (String key : cs) {
			UUID uuid = UUID.fromString(key);
			ConfigurationSection raw = posData.getConfigurationSection(key);
			Location pos = new Location(
				Bukkit.getServer().getWorld(raw.getString("world")),
				raw.getDouble("x"), raw.getDouble("y"), raw.getDouble("z"),
				Float.parseFloat(raw.getString("yaw")), Float.parseFloat(raw.getString("pitch"))
			);
			posPlayers.put(uuid, pos);
		}

	}


	//Save the positions of all players
	public static void savePositions() {

		File statsFile = new File(positionsPath);
		FileConfiguration statsAccess = YamlConfiguration.loadConfiguration(statsFile);
		
		statsAccess.set("positions", "");
		
		for (Map.Entry< UUID , Location > entry : posPlayers.entrySet()) {
			UUID uuid = entry.getKey();
			Location pos = entry.getValue();
			
			statsAccess.set("positions." + uuid + ".world", pos.getWorld().getName());
			statsAccess.set("positions." + uuid + ".x", pos.getX());
			statsAccess.set("positions." + uuid + ".y", pos.getY());
			statsAccess.set("positions." + uuid + ".z", pos.getZ());
			statsAccess.set("positions." + uuid + ".yaw", pos.getYaw());
			statsAccess.set("positions." + uuid + ".pitch", pos.getPitch());
		}
		
		try {
			statsAccess.save(statsFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	//Set a player's position
	public static void setPlayerPosition(Player player) {
		posPlayers.put(player.getUniqueId(), player.getLocation());
		savePositions();
	}


	//Get a player's position
	public static Location getPlayerPosition(UUID uuid) {
		return posPlayers.get(uuid);
	}
	
}