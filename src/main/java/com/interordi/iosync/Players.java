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

	private static String configPath = "plugins/IOSync/";

	private static String positionsFile = "positions.yml";
	private static Map< UUID, Location > posPlayers;
	
	
	public static void init(IOSync plugin, String storagePath, String serverPath) {
		Players.plugin = plugin;
		Players.storagePath = storagePath;
		Players.serverPath = serverPath;

		loadAllData();
	}


	//Load all the data from files
	public static void loadAllData() {
		posPlayers = loadPositions(positionsFile);
	}


	//Save all the data to files
	public static void saveAllData() {
		savePositions(positionsFile, posPlayers);
	}


	//Load a player's inventory from storage
	public static void loadPlayer(Player player) {

		if (storagePath.isEmpty() || serverPath.isEmpty())
			return;

		File source = new File(storagePath + player.getUniqueId() + ".dat");
		File dest = new File(serverPath + player.getUniqueId() + ".dat");

		if (Files.exists(source.toPath())) {
			try {
				Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				System.out.println("ERROR: Failed to copy file from storage");
				System.out.println("Reason: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}


	//Save a player's inventory to storage
	public static void savePlayer(Player player) {

		if (storagePath.isEmpty() || serverPath.isEmpty())
			return;

		//Avoid item duping
		plugin.getServer().savePlayers();

		saveOnePlayer(player);
	}


	//Save the data of all players, good for periodic checks
	public static void saveAllPlayers() {

		//Save positions right away
		for (Player player : plugin.getServer().getOnlinePlayers()) {
			setPlayerPosition(player);
		}
		saveAllData();

		if (storagePath.isEmpty() || serverPath.isEmpty())
			return;

		//Avoid item duping
		plugin.getServer().savePlayers();

		for (Player player : plugin.getServer().getOnlinePlayers()) {
			saveOnePlayer(player);
		}
	}


	//Save the data of one player
	public static void saveOnePlayer(Player player) {
		setPlayerPosition(player);
		saveAllData();

		File source = new File(serverPath + player.getUniqueId() + ".dat");
		File dest = new File(storagePath + player.getUniqueId() + ".dat");

		if (Files.exists(source.toPath())) {
			try {
				Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				System.out.println("ERROR: Failed to write file to storage");
				System.out.println("Reason: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}


	//Get the positions of all players
	public static Map< UUID, Location > loadPositions(String filename) {

		Map< UUID, Location > positions = new HashMap< UUID, Location >();
		
		File statsFile = new File(configPath + filename);
		FileConfiguration statsAccess = YamlConfiguration.loadConfiguration(statsFile);
		
		ConfigurationSection posData = statsAccess.getConfigurationSection("positions");
		if (posData == null) {
			plugin.getLogger().info("ERROR: Positions YML section not found");
			return positions;	//Nothing yet, exit
		}
		Set< String > cs = posData.getKeys(false);
		if (cs == null) {
			plugin.getLogger().info("ERROR: Couldn't get player keys");
			return positions;	//No players found, nothing to do
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
			positions.put(uuid, pos);
		}

		return positions;
	}


	//Save the positions of all players
	public static void savePositions(String filename, Map< UUID, Location > positions) {

		File statsFile = new File(configPath + filename);
		FileConfiguration statsAccess = YamlConfiguration.loadConfiguration(statsFile);
		
		statsAccess.set("positions", "");
		
		for (Map.Entry< UUID , Location > entry : positions.entrySet()) {
			UUID uuid = entry.getKey();
			Location pos = entry.getValue();

			try {
				statsAccess.set("positions." + uuid + ".world", pos.getWorld().getName());
				statsAccess.set("positions." + uuid + ".x", pos.getX());
				statsAccess.set("positions." + uuid + ".y", pos.getY());
				statsAccess.set("positions." + uuid + ".z", pos.getZ());
				statsAccess.set("positions." + uuid + ".yaw", pos.getYaw());
				statsAccess.set("positions." + uuid + ".pitch", pos.getPitch());
			} catch (NullPointerException e) {
				System.out.println("Failed to save the position for " + uuid.toString());
			}
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
	}


	//Get a player's position
	public static Location getPlayerPosition(UUID uuid) {
		return posPlayers.get(uuid);
	}
	
}