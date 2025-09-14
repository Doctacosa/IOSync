package com.interordi.iosync.listeners;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.interordi.iosync.IOSync;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
import org.spigotmc.event.player.PlayerSpawnLocationEvent;


public class LoginListener implements Listener {

	IOSync plugin;
	boolean enablePositionSaving = false;
	
	
	public LoginListener(IOSync plugin, boolean enablePositionSaving) {
		this.plugin = plugin;
		this.enablePositionSaving = enablePositionSaving;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);

		Bukkit.getLogger().info("Testing for login event support:");
		boolean found = false;

		//New Paper model
		try {
			if (!found) {
				Class< ? > playerLogin = Class.forName("io.papermc.paper.event.player.PlayerServerFullCheckEvent");
				Bukkit.getLogger().info("* PlayerServerFullCheckEvent found");

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
			Bukkit.getLogger().info("* PlayerServerFullCheckEvent not found");
		}

		//Classic Spigot support
		try {
			if (!found) {
				Class< ? > playerLogin = Class.forName("org.bukkit.event.player.PlayerLoginEvent");
				Bukkit.getLogger().info("* PlayerLoginEvent found");

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
			Bukkit.getLogger().info("* PlayerLoginEvent not found");
		}
	}


	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerQuit(PlayerQuitEvent event) {
		//Save all players to ensure periodic safety saves
		Location pos = event.getPlayer().getBedSpawnLocation();
		plugin.getPlayersInst().setPlayerSpawn(event.getPlayer(), pos);

		plugin.getPlayersInst().saveAllPlayers();
		//Players.savePlayerData(event.getPlayer());
	}


	@EventHandler(priority=EventPriority.LOWEST)
	public void onPlayerSpawnLocationEvent(PlayerSpawnLocationEvent event) {
		Location pos = plugin.getPlayersInst().getPlayerPosition(event.getPlayer().getUniqueId());
		if (pos != null && plugin.getServer().getWorlds().contains(pos.getWorld()))
			event.setSpawnLocation(pos);
		else if (enablePositionSaving)
			event.setSpawnLocation(event.getPlayer().getWorld().getSpawnLocation());
	}
}