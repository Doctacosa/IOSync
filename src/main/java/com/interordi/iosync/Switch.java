package com.interordi.iosync;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.interordi.iosync.structs.ServerLoading;
import com.interordi.iosync.utilities.Http;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;

public class Switch implements Runnable {

	private IOSync plugin = null;
	private boolean bungeeInit = false;
	private int timerTask = -1;

	private Map< String, ServerLoading > serversLoading;

	private int loadDuration = 20;

	
	public Switch(IOSync plugin) {
		this.plugin = plugin;
		serversLoading = new HashMap< String, ServerLoading >();
	}


	public boolean requestSwitch(Player target, String destination) {

		if (isAlreadySwitching(target)) {
			target.sendMessage(ChatColor.RED + "You already have a move underway, please wait.");
			return true;
		}
		
		//If that server is already loading, add the player and immediately return
		if (serversLoading.containsKey(destination)) {
			serversLoading.get(destination).addPlayer(target);
			target.sendMessage(ChatColor.YELLOW + "This world is being prepared, you will be moved automatically when ready.");
			target.sendTitle(ChatColor.WHITE + "Loading...", ChatColor.WHITE + "Please wait!", 10, 100, 10);
			return true;
		}

		final String finalUrl = plugin.getApiServer() + "servers/start/?id=" + destination;

		//Run in its own thread
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			String responseRaw = Http.readUrl(finalUrl);
			if (responseRaw.trim().equals("[]"))
				responseRaw = "{}";
			
			JsonParser jsonParser = new JsonParser();
			JsonElement jsonRoot = jsonParser.parse(responseRaw);
			JsonObject jsonObject = jsonRoot.getAsJsonObject();
			if (jsonObject.has("success")) {
				String message = jsonObject.get("success").getAsString();
				Bukkit.getScheduler().runTask(plugin, () -> {
					if (message.equalsIgnoreCase("Ready")) {
						//Server is ready, move now
						executeSwitch(target, destination);
					} else if (message.equalsIgnoreCase("Loading")) {
						//Server not ready, display a loading notification
						target.sendMessage(ChatColor.YELLOW + "This world is being prepared, you will be moved automatically when ready.");
						target.sendTitle(ChatColor.WHITE + "Loading...", ChatColor.WHITE + "Please wait!", 10, 100, 10);
						prepareSwitch(target, destination);
					}
				});
			} else if (jsonObject.has("error")) {
				String message = jsonObject.get("error").getAsString();
				Bukkit.getScheduler().runTask(plugin, () -> {
					if (message.equalsIgnoreCase("Full")) {
						//RAM usage is maxed out, tell the player to try again later
						target.sendMessage(ChatColor.YELLOW + "This world is not currently available due to a high server load, try another one and come back later!");
					} else if (message.equalsIgnoreCase("Unknown server")) {
						//Unknown server, trying anyway
						executeSwitch(target, destination);
					} else if (message.equalsIgnoreCase("Undefined group")) {
						//Unknown server, trying anyway
						executeSwitch(target, destination);
					}
				});
			}
		});

		return true;
	}


	//Execute a server switch on the given player
	public boolean executeSwitch(Player target, String destination) {
		//Send the world Switch message to Bungee
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(b);
		try {
			out.writeUTF("Connect");
			out.writeUTF(destination);
		} catch (IOException e) {
		}

		if (!bungeeInit) {
			Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
			bungeeInit = true;
		}

		//Save players so their inventory and metadata is up to date
		Bukkit.getServer().savePlayers();
		Players.savePlayer(target);
		target.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());

		return true;
	}


	//Display a loading display
	public void prepareSwitch(Player target, String destination) {
		if (timerTask == -1)
			timerTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this, 1*20L, 1*20L);
		serversLoading.put(destination, new ServerLoading(target, destination));
	}


	//Check if a player is already waiting for a switch
	public boolean isAlreadySwitching(Player player) {
		for (ServerLoading serverData : serversLoading.values()) {
			if (serverData.players.contains(player))
				return true;
		}
		return false;
	}


	@Override
	public void run() {
		//Every second, update display bars and disable finished events
		Instant now = Instant.now();

		for (String server : serversLoading.keySet()) {
			ServerLoading serverData = serversLoading.get(server);
			double elapsed = now.getEpochSecond() - serverData.loading.getEpochSecond();

			if (elapsed >= loadDuration) {
				serverData.bar.removeAll();
				serversLoading.remove(server);
				if (serversLoading.isEmpty()) {
					Bukkit.getScheduler().cancelTask(timerTask);
					timerTask = -1;
				}

				for (Player player : serverData.players) {
					if (player.isOnline()) {
						player.sendTitle(ChatColor.WHITE + "Switching...", ChatColor.WHITE + "Please wait!", 10, 100, 10);
						executeSwitch(player, server);
					}
				}
			} else {
				double progress = elapsed / loadDuration;
				if (progress >= 0.0 && serverData.bar != null) {
					serverData.bar.setProgress(progress);
				}
			}
		}
	}
}
