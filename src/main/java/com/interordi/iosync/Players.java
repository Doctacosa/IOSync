package com.interordi.iosync;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ConcurrentModificationException;
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


public class Players implements Runnable {

	private IOSync plugin;
	private String storagePath;
	private String serverPath;

	private String configPath = "plugins/IOSync/";

	private String positionsFile = "positions.yml";
	private String spawnsFile = "spawns.yml";
	private String bedsFile = "beds.yml";
	private Map< UUID, Location > posPlayers;
	private Map< UUID, Location > spawnsPlayers;
	private Map< UUID, Location > bedsPlayers;

	private boolean saving = false;
	
	
	public Players(IOSync plugin, String storagePath, String serverPath) {
		this.plugin = plugin;
		this.storagePath = storagePath;
		this.serverPath = serverPath;

		loadAllData();
	}


	//Load all the data from files
	public void loadAllData() {
		posPlayers = loadPositions(positionsFile);
		spawnsPlayers = loadPositions(spawnsFile);
		bedsPlayers = loadPositions(bedsFile);
	}


	//Save all the data to files
	public void saveAllData() {
		saveAllData(false);
	}

	public void saveAllData(boolean instant) {
		//No need to save if we're already saving
		if (saving)
			return;
		
		saving = true;

		//Save using copies to avoid a ConcurrentModificationException
		Map< UUID, Location > posPlayersCopy = new HashMap< UUID, Location >();
		posPlayersCopy.putAll(posPlayers);
		Map< UUID, Location > spawnsPlayersCopy = new HashMap< UUID, Location >();
		spawnsPlayersCopy.putAll(spawnsPlayers);
		Map< UUID, Location > bedsPlayersCopy = new HashMap< UUID, Location >();
		bedsPlayersCopy.putAll(bedsPlayers);

		if (instant) {
			//On closing, use the main thread
			savePositions(positionsFile, posPlayersCopy);
			savePositions(spawnsFile, spawnsPlayersCopy);
			savePositions(bedsFile, bedsPlayersCopy);

			saving = false;

		} else {
			//Run on its own thread to avoid holding up the server
			Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
				try {
					savePositions(positionsFile, posPlayersCopy);
					savePositions(spawnsFile, spawnsPlayersCopy);
					savePositions(bedsFile, bedsPlayersCopy);
				} catch (ConcurrentModificationException e) {
					//Ignore, next save will get them
				}

				saving = false;
			});
		}
	}


	//Load a player's inventory from storage
	public void loadPlayer(Player player) {

		if (storagePath.isEmpty() || serverPath.isEmpty())
			return;

		File source = new File(storagePath + player.getUniqueId() + ".dat");
		File dest = new File(serverPath + player.getUniqueId() + ".dat");

		if (Files.exists(source.toPath())) {
			try {
				Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				Bukkit.getLogger().severe("ERROR: Failed to copy file from storage");
				Bukkit.getLogger().severe("Reason: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}


	//Save a player's inventory to storage
	public void savePlayer(Player player) {

		if (storagePath.isEmpty() || serverPath.isEmpty())
			return;

		//Avoid item duping
		plugin.getServer().savePlayers();

		saveAllData();
		saveOnePlayer(player);
	}


	//Save the data of all players, good for periodic checks
	public void saveAllPlayers() {

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
	public void saveOnePlayer(Player player) {
		File source = new File(serverPath + player.getUniqueId() + ".dat");
		File dest = new File(storagePath + player.getUniqueId() + ".dat");

		if (Files.exists(source.toPath())) {
			try {
				Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				Bukkit.getLogger().severe("ERROR: Failed to write file to storage");
				Bukkit.getLogger().severe("Reason: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}


	//Get the positions of all players
	public Map< UUID, Location > loadPositions(String filename) {

		Map< UUID, Location > positions = new HashMap< UUID, Location >();
		
		File statsFile = new File(configPath + filename);
		FileConfiguration statsAccess = YamlConfiguration.loadConfiguration(statsFile);
		
		ConfigurationSection posData = statsAccess.getConfigurationSection("positions");
		if (posData == null) {
			plugin.getLogger().info("ERROR: Positions YML section in " + filename + " not found");
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
	public void savePositions(String filename, Map< UUID, Location > positions) throws ConcurrentModificationException {

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
				Bukkit.getLogger().severe("Failed to save the position in  " + filename + " for " + uuid.toString());
			}
		}
		
		try {
			statsAccess.save(statsFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	//Set a player's position
	public void setPlayerPosition(Player player) {
		posPlayers.put(player.getUniqueId(), player.getLocation());
	}

	//Get a player's position
	public Location getPlayerPosition(UUID uuid) {
		return posPlayers.get(uuid);
	}

	
	//Set a player's spawn
	public void setPlayerSpawn(Player player, Location bed) {
		spawnsPlayers.put(player.getUniqueId(), player.getLocation());
		bedsPlayers.put(player.getUniqueId(), bed);
	}

	//Get a player's spawn
	public Location getPlayerSpawn(UUID uuid) {
		return spawnsPlayers.get(uuid);
	}


	//Handle a bed being broken
	public void bedBroken(Location broken) {
		Map< UUID, Location > bedsPlayersCopy = new HashMap< UUID, Location >();
		bedsPlayersCopy.putAll(bedsPlayers);

		for (Map.Entry< UUID , Location > entry : bedsPlayersCopy.entrySet()) {
			UUID uuid = entry.getKey();
			Location bed = entry.getValue();

			if (bed.getWorld() == broken.getWorld() &&
				bed.getX() == broken.getX() &&
				bed.getY() == broken.getY() &&
				bed.getZ() == broken.getZ()) {

				//A spawn bed has been broken, nuke the player's spawn position
				bedsPlayers.remove(uuid);
				spawnsPlayers.remove(uuid);
			}
		}
	}


	@Override
	public void run() {
		saveAllPlayers();
	}
	
}