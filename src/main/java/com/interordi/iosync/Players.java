package com.interordi.iosync;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTFile;
import de.tr7zw.changeme.nbtapi.NBTList;


public class Players implements Runnable {

	private IOSync plugin;
	private String storagePath;
	private String serverPath;
	private String playerPermissions;

	private String configPath = "plugins/IOSync/";

	private String positionsFile = "positions.yml";
	private String spawnsFile = "spawns.yml";
	private String bedsFile = "beds.yml";
	private Map< UUID, Location > posPlayers;
	private Map< UUID, Location > spawnsPlayers;
	private Map< UUID, Location > bedsPlayers;

	private Map< UUID, Location > backupPosPlayers;
	private Map< UUID, Location > backupSpawnsPlayers;

	private boolean saving = false;
	
	
	public Players(IOSync plugin, String storagePath, String serverPath, String playerPermissions) {
		this.plugin = plugin;
		this.storagePath = storagePath;
		this.serverPath = serverPath;
		this.playerPermissions = playerPermissions;

		backupPosPlayers = new HashMap< UUID, Location >();
		backupSpawnsPlayers = new HashMap< UUID, Location >();

		loadAllData();
	}


	//Load all the data from files
	public void loadAllData() {
		posPlayers = loadPositions(positionsFile);
		spawnsPlayers = loadPositions(spawnsFile);
		bedsPlayers = loadPositions(bedsFile);
	}


	//Save all the data to files
	public void saveAllPositions() {
		saveAllPositions(false);
	}

	public void saveAllPositions(boolean instant) {
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
	public void loadPlayer(UUID playerUuid) {
		loadPlayer(playerUuid, false);
	}

	public void loadPlayer(UUID playerUuid, boolean enablePositionSaving) {

		if (storagePath.isEmpty() || serverPath.isEmpty())
			return;

		File source = new File(storagePath + playerUuid + ".dat");
		File dest = new File(serverPath + playerUuid + ".dat");

		if (Files.exists(source.toPath())) {
			try {
				String version = getProcessVersion();

				Location serverSpawn = Bukkit.getServer().getWorlds().get(0).getSpawnLocation();

				//If the player already has a location, extract it before overwriting it
				//It will be used as a fallback if the plugin doesn't have anything yet
				if (dest.exists() && !dest.isDirectory()) {
					Location playerSpawn = serverSpawn.clone();
					NBTFile playerData = new NBTFile(dest);


					//Get the player's position from the current file
					NBTList< Double > posTag = playerData.getDoubleList("Pos");
					if (posTag != null) {
						playerSpawn.setX(posTag.get(0));
						playerSpawn.setY(posTag.get(1));
						playerSpawn.setZ(posTag.get(2));
					}

					NBTList< Float > rotTag = playerData.getFloatList("Rotation");
					if (rotTag != null) {
						playerSpawn.setYaw(rotTag.get(0));
						playerSpawn.setPitch(rotTag.get(1));
					}

					World world = getWorldFromId(playerData.getString("Dimension"));
					if (world != null)
						playerSpawn.setWorld(world);
					backupPosPlayers.put(playerUuid, playerSpawn);


					//Get the player's respawn position with the same logic
					Location playerRespawn = serverSpawn.clone();
					//1.21.5+ logic
					if (playerData.hasTag("respawn")) {
						NBTCompound respawnGroup = playerData.getCompound("respawn");
						int[] pos = respawnGroup.getIntArray("pos");
						playerRespawn.setX(pos[0]);
						playerRespawn.setY(pos[1]);
						playerRespawn.setZ(pos[2]);
						world = getWorldFromId(respawnGroup.getString("dimension"));
						if (world != null)
							playerRespawn.setWorld(world);
						playerRespawn.setYaw(respawnGroup.getFloat("yaw"));
						playerRespawn.setPitch(respawnGroup.getFloat("pitch"));
						
						backupSpawnsPlayers.put(playerUuid, playerRespawn);
					}
					//1.21.4- logic
					else if (playerData.hasTag("SpawnX") &&
						playerData.hasTag("SpawnY") &&
						playerData.hasTag("SpawnZ") &&
						playerData.hasTag("SpawnDimension")
					) {
						playerRespawn.setX(playerData.getFloat("SpawnX"));
						playerRespawn.setY(playerData.getFloat("SpawnY"));
						playerRespawn.setZ(playerData.getFloat("SpawnZ"));
						world = getWorldFromId(playerData.getString("SpawnDimension"));
						if (world != null)
							playerRespawn.setWorld(world);
						backupSpawnsPlayers.put(playerUuid, playerRespawn);
					}
				}

				Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

				//NBT manipulation: https://github.com/tr7zw/Item-NBT-API

				//Reset the player's position to spawn
				//On actual join, will teleport to last position
				if (serverSpawn != null) {
					NBTFile playerData = new NBTFile(dest);

					//Set position
					NBTList< Double > posTag = playerData.getDoubleList("Pos");
					posTag.clear();
					posTag.add(serverSpawn.getX());
					posTag.add(serverSpawn.getY());
					posTag.add(serverSpawn.getZ());

					//Set rotation
					NBTList< Float > rotTag = playerData.getFloatList("Rotation");
					rotTag.clear();
					rotTag.add(serverSpawn.getYaw());
					rotTag.add(serverSpawn.getPitch());

					//Send to overworld
					playerData.setString("Dimension", "minecraft:overworld");
					
					playerData.save();

					//Copy with changes
					/*
					NBTFile fileTest = new NBTFile(new File("./test.nbt"));
					fileTest.mergeCompound(playerData);
					//fileTest.mergeCompound(posTag);
					fileTest.save();
					*/
				}

				//Set the bed's position in the file
				Location bed = plugin.getPlayersInst().getPlayerBed(playerUuid);

				if (bed != null) {
					NBTFile playerData = new NBTFile(dest);

					String dimension = null;
					if (bed.getWorld() != null)
						dimension = bed.getWorld().getEnvironment().toString();
					else
						dimension = Bukkit.getServer().getWorlds().get(0).getSpawnLocation().getWorld().getEnvironment().toString();

					dimension = dimension.toLowerCase();
					if (dimension.equals("normal"))
						dimension = "overworld";

					//Set bed positions
					if (version.equals("1.21.5+")) {
						NBTCompound respawnGroup = playerData.addCompound("respawn");
						int[] pos = {bed.getBlockX(), bed.getBlockY(), bed.getBlockZ()};
						float zero = 0;
						respawnGroup.setIntArray("pos", pos);
						respawnGroup.setFloat("yaw", zero);
						respawnGroup.setFloat("pitch", zero);
						respawnGroup.setString("dimension", getIdFromWorld(bed.getWorld()));
					} else {
						playerData.setString("SpawnDimension", "minecraft:" + dimension);
						playerData.setInteger("SpawnX", bed.getBlockX());
						playerData.setInteger("SpawnY", bed.getBlockY());
						playerData.setInteger("SpawnZ", bed.getBlockZ());
					}
					
					playerData.save();
				}
				//If no bed, unset bed
				else {
					NBTFile playerData = new NBTFile(dest);
					//1.21.5+ method
					playerData.removeKey("respawn");
					//1.21.4- method
					playerData.removeKey("SpawnDimension");
					playerData.removeKey("SpawnX");
					playerData.removeKey("SpawnY");
					playerData.removeKey("SpawnZ");
					playerData.save();
				}

			} catch (IOException e) {
				Bukkit.getLogger().severe("ERROR: Failed to copy file from storage");
				Bukkit.getLogger().severe("Reason: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}


	//Save a player's inventory to storage
	public void savePlayerData(Player player) {

		if (storagePath.isEmpty() || serverPath.isEmpty())
			return;

		//Avoid item duping
		plugin.getServer().savePlayers();

		saveAllPositions();
		savePlayerInventory(player);
	}


	//Save the data of all players, good for periodic checks
	public void saveAllPlayers() {

		//Save positions right away
		for (Player player : plugin.getServer().getOnlinePlayers()) {
			setPlayerPosition(player);
		}
		saveAllPositions();

		if (storagePath.isEmpty() || serverPath.isEmpty())
			return;

		//Avoid item duping
		plugin.getServer().savePlayers();

		for (Player player : plugin.getServer().getOnlinePlayers()) {
			savePlayerInventory(player);
		}
	}


	//Save the data of one player
	public void savePlayerInventory(Player player) {
		File source = new File(serverPath + player.getUniqueId() + ".dat");
		File dest = new File(storagePath + player.getUniqueId() + ".dat");

		if (Files.exists(source.toPath())) {
			try {
				String version = getProcessVersion();

				Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

				//Try to assign the given permissions to the player file
				if (playerPermissions != null && !playerPermissions.isEmpty()) {
					try {
						Set< PosixFilePermission > othersRead = PosixFilePermissions.fromString(playerPermissions);
						Files.setPosixFilePermissions(dest.toPath(), othersRead);
					} catch (UnsupportedOperationException e) {
						//Not supported on this platform; ignore
					}
				}

				//Read the bed position as set in the file
				NBTFile playerData = new NBTFile(dest);
				World world;
				Location bed = null;

				//Get the player's respawn position
				//1.21.5+ logic
				if (playerData.hasTag("respawn")) {
					NBTCompound respawnGroup = playerData.getCompound("respawn");
					int[] pos = respawnGroup.getIntArray("pos");
					world = getWorldFromId(respawnGroup.getString("dimension"));
					if (world != null) {
						bed = new Location(
							world,
							pos[0],
							pos[1],
							pos[2]
						);
					}
					bed.setYaw(respawnGroup.getInteger("yaw"));
					bed.setPitch(respawnGroup.getInteger("pitch"));
				}
				//1.21.4- logic
				else if (playerData.hasTag("SpawnX") &&
					playerData.hasTag("SpawnY") &&
					playerData.hasTag("SpawnZ") &&
					playerData.hasTag("SpawnDimension")
				) {
					world = getWorldFromId(playerData.getString("SpawnDimension"));
					if (world != null) {
						bed = new Location(
							world,
							playerData.getInteger("SpawnX"),
							playerData.getInteger("SpawnY"),
							playerData.getInteger("SpawnZ")
						);
					}
				}

				if (bed != null && plugin.getServer().getWorlds().contains(bed.getWorld()))
					plugin.getPlayersInst().setPlayerBed(player, bed);

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

			if (pos != null) {
				try {
					String world = Bukkit.getServer().getWorlds().get(0).getSpawnLocation().getWorld().getName();
					if (pos.getWorld() != null)
						world = pos.getWorld().getName();

					statsAccess.set("positions." + uuid + ".world", world);
					statsAccess.set("positions." + uuid + ".x", pos.getX());
					statsAccess.set("positions." + uuid + ".y", pos.getY());
					statsAccess.set("positions." + uuid + ".z", pos.getZ());
					statsAccess.set("positions." + uuid + ".yaw", pos.getYaw());
					statsAccess.set("positions." + uuid + ".pitch", pos.getPitch());
				} catch (NullPointerException e) {
					Bukkit.getLogger().severe("Failed to save the position in  " + filename + " for " + uuid.toString());
					e.printStackTrace();
				}
			} else {
				statsAccess.set("positions." + uuid, null);
			}
		}
		
		try {
			statsAccess.save(statsFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	//Get a world reference based on its ID
	public World getWorldFromId(String worldName) {
		World world = Bukkit.getWorlds().get(0);
		if (worldName.equalsIgnoreCase("minecraft:overworld"))
			world = Bukkit.getWorlds().get(0);
		else if (worldName.equalsIgnoreCase("minecraft:the_nether"))
			world = Bukkit.getWorlds().get(1);
		else if (worldName.equalsIgnoreCase("minecraft:the_end"))
			world = Bukkit.getWorlds().get(2);
		else
			world = Bukkit.getWorld(worldName.substring("minecraft:".length()));

		if (world == null)
			Bukkit.getLogger().info("World NOT found as backup: " + worldName);

		return world;
	}


	//Get a world ID based on its name
	public String getIdFromWorld(World world) {
		String worldName = world.getName();
		if (worldName.equalsIgnoreCase(Bukkit.getWorlds().get(0).getName()))
			worldName = "minecraft:overworld";
		else if (worldName.equalsIgnoreCase(Bukkit.getWorlds().get(1).getName()))
			worldName = "minecraft:the_nether";
		else if (worldName.equalsIgnoreCase(Bukkit.getWorlds().get(2).getName()))
			worldName = "minecraft:the_end";
		else
			worldName = "minecraft:" + worldName;

		return worldName;
	}


	//Check which version we're dealing with
	public String getProcessVersion() {
		String gameVersion = Bukkit.getVersion().split("-")[0];
		List< Integer > subVersion = null;
		String process = "1.21.5+";
		if (gameVersion.contains(".")) {
			subVersion = Arrays.asList(gameVersion.split("\\."))
				.stream().map(x -> Integer.parseInt(x))
				.collect(Collectors.toList());

			if (subVersion.get(0) <= 1 &&
				subVersion.get(1) <= 21 &&
				subVersion.get(2) < 5) {
				process = "1.21.4-";
			}
		} else {
			Bukkit.getLogger().severe("Unreadable game version: " + gameVersion + " from " + Bukkit.getVersion() + ", assuming latest");
		}

		return process;
	}


	//Set a player's position
	public void setPlayerPosition(Player player) {
		posPlayers.put(player.getUniqueId(), player.getLocation());
	}

	//Get a player's position
	public Location getPlayerPosition(UUID uuid) {
		return posPlayers.get(uuid);
	}

	//Get a player's backup position
	public Location getBackupPlayerPosition(UUID uuid) {
		return backupPosPlayers.get(uuid);
	}

	
	//Set a player's spawn
	public void setPlayerSpawn(Player player, Location loc) {
		spawnsPlayers.put(player.getUniqueId(), loc);
	}

	//Get a player's spawn
	public Location getPlayerSpawn(UUID uuid) {
		return spawnsPlayers.get(uuid);
	}


	//Set a player's bed position
	public void setPlayerBed(Player player, Location bed) {
		bedsPlayers.put(player.getUniqueId(), bed);
	}

	//Get a player's bed position
	public Location getPlayerBed(UUID uuid) {
		return bedsPlayers.get(uuid);
	}


	@Override
	public void run() {
		saveAllPlayers();
	}
	
}