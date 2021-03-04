package com.interordi.iosync;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.interordi.iosync.utilities.Http;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Switch {

	private static IOSync plugin = null;
	private static boolean bungeeInit = false;

	
	public static void init(IOSync plugin) {
		Switch.plugin = plugin;
	}


	public static boolean requestSwitch(Player player, String server) {
		
		final String finalUrl = "https://localhost/";

		//Run in its own thread
		new Thread(() -> {
			String responseRaw = Http.readUrl(finalUrl);
			if (responseRaw.trim().equals("[]"))
				responseRaw = "{}";
			
			JsonElement jsonRoot = JsonParser.parseString(responseRaw);
			JsonObject jsonObject = jsonRoot.getAsJsonObject();
			if (jsonObject.has("success")) {
				String message = jsonObject.get("success").getAsString();
			} else if (jsonObject.has("error")) {
				String message = jsonObject.get("error").getAsString();
			}
			//	Players.updatePledge(finalUsername, );
		}).start();

		return true;
	}


	//Execute a server switch on the given player
	public static boolean executeSwitch(Player target, String destination) {
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
}
