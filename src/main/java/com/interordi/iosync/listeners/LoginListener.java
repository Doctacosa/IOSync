package com.interordi.iosync.listeners;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.interordi.iosync.IOSync;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.RegisteredListener;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;


public class LoginListener implements Listener {

	IOSync plugin;
	boolean enablePositionSaving = false;
	
	
	public LoginListener(IOSync plugin, boolean enablePositionSaving) {
		this.plugin = plugin;
		this.enablePositionSaving = enablePositionSaving;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);

		Bukkit.getLogger().info("Testing for login event support:");
		boolean found = false;

		//Action on connecting, early in the process
		//New Paper model
		try {
			if (!found) {
				Class< ? > playerLogin = Class.forName("io.papermc.paper.event.player.PlayerServerFullCheckEvent");
				Bukkit.getLogger().info("- PlayerServerFullCheckEvent found");

				EventExecutor executor = (listener, event) -> {
					if (playerLogin.isInstance(event)) {
						try {
							//Fetching parameters
							Method getPlayerProfileMethod = event.getClass().getMethod("getPlayerProfile");
							PlayerProfile player = (PlayerProfile) getPlayerProfileMethod.invoke(event);

							//Executing logic
							plugin.getPlayersInst().loadPlayer(player.getId(), enablePositionSaving);
						} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
							e.printStackTrace();
						}
					}
				};

				//Register handler manually
				HandlerList handlerList = (HandlerList) playerLogin.getMethod("getHandlerList").invoke(null);
				handlerList.register(new RegisteredListener(new Listener() {}, executor, EventPriority.NORMAL, plugin, false));
				found = true;
			}
		} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			//Not found skipping
			Bukkit.getLogger().info("- PlayerServerFullCheckEvent not found");
		}

		//Classic Spigot support
		try {
			if (!found) {
				Class< ? > playerLogin = Class.forName("org.bukkit.event.player.PlayerLoginEvent");
				Bukkit.getLogger().info("- PlayerLoginEvent found");

				EventExecutor executor = (listener, event) -> {
					if (playerLogin.isInstance(event)) {
						try {
							//Fetching parameters
							Method getPlayerMethod = event.getClass().getMethod("getPlayer");
							Player player = (Player) getPlayerMethod.invoke(event);

							//Executing logic
							plugin.getPlayersInst().loadPlayer(player.getUniqueId(), enablePositionSaving);
						} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
							e.printStackTrace();
						}
					}
				};

				//Register handler manually
				HandlerList handlerList = (HandlerList) playerLogin.getMethod("getHandlerList").invoke(null);
				handlerList.register(new RegisteredListener(new Listener() {}, executor, EventPriority.NORMAL, plugin, false));
				found = true;
			}
		} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			//Not found, skipping
			Bukkit.getLogger().info("- PlayerLoginEvent not found");
		}
	}


	//Action on leaving
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerQuit(PlayerQuitEvent event) {
		//Save all players to ensure periodic safety saves
		Location pos = event.getPlayer().getBedSpawnLocation();
		plugin.getPlayersInst().setPlayerSpawn(event.getPlayer(), pos);

		plugin.getPlayersInst().saveAllPlayers();
		//Players.savePlayerData(event.getPlayer());
	}


	//Action on joining
	@EventHandler(priority=EventPriority.LOWEST)
	public void onPlayerSpawnLocationEvent(AsyncPlayerSpawnLocationEvent event) {
		UUID uuid = event.getConnection().getProfile().getId();
		Location pos = plugin.getPlayersInst().getPlayerPosition(uuid);
		Location posBackup = plugin.getPlayersInst().getBackupPlayerPosition(uuid);

		if (pos != null && plugin.getServer().getWorlds().contains(pos.getWorld())) {
			//We have a location on record, send them there
			event.setSpawnLocation(pos);
		} else if (posBackup != null && plugin.getServer().getWorlds().contains(posBackup.getWorld())) {
			//No location recorded, use the one from the player's profile
			event.setSpawnLocation(posBackup);
		} else if (enablePositionSaving) {
			//No known position, send back to spawn
			event.setSpawnLocation(event.getSpawnLocation());
		}
	}
}