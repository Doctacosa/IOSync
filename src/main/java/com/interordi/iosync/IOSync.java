package com.interordi.iosync;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import com.interordi.iosync.listeners.LoginListener;
import com.interordi.iosync.listeners.SpawnListener;
import com.interordi.iosync.utilities.CommandTargets;
import com.interordi.iosync.utilities.Commands;

public class IOSync extends JavaPlugin {

	private LoginListener thisLoginListener;
	private SpawnListener thisSpawnListener;
	private Players thisPlayers;
	private Switch switchSupport;

	private String apiService;


	public void onEnable() {

		//Always ensure we've got a copy of the config in place (does not overwrite existing)
		this.saveDefaultConfig();

		//Get the storage path to use, if any
		String storagePath = this.getConfig().getString("storage-path", "");
		if (storagePath.equalsIgnoreCase("null"))
			storagePath = "";
		if (!storagePath.endsWith("/") && !storagePath.endsWith("\\") && !storagePath.isEmpty())
			storagePath += "/";

		apiService = this.getConfig().getString("api-service", "");

		//Get the location of the playerdata folder
		String serverPath = "";
		try {
			BufferedReader is = new BufferedReader(new FileReader("server.properties"));
			Properties props = new Properties();
			props.load(is);
			is.close();
			serverPath = props.getProperty("level-name");
		} catch (IOException e) {
			Bukkit.getLogger().warning("ERROR: Couldn't read level-name, defaulting to 'world'");
			serverPath = "world";
		}
		serverPath = "./" + serverPath + "/playerdata/";

		thisLoginListener = new LoginListener(this, (!storagePath.isEmpty() && !serverPath.isEmpty()));
		thisSpawnListener = new SpawnListener(this, (!storagePath.isEmpty() && !serverPath.isEmpty()));
		thisPlayers = new Players(this, storagePath, serverPath);
		
		switchSupport = new Switch(this);

		//Save the data on a regular basis
		getServer().getScheduler().scheduleSyncRepeatingTask(this, thisPlayers, 5*60*20L, 5*60*20L);

		getLogger().info("IOSync enabled");
	}


	public void onDisable() {
		thisPlayers.saveAllData(true);

		getLogger().info("IOSync disabled");
	}
	

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		
		//Get the list of potential targets if a selector was used
		CommandTargets results = Commands.findTargets(getServer(), sender, cmd, label, args);
		
		boolean result = false;
		if (results.position != -1) {
			//Run the command for each target identified by the selector
			for (String target : results.targets) {
				args[results.position] = target;
				
				result = runCommand(sender, cmd, label, args);
			}
		} else {
			//Run the command as-is
			result = runCommand(sender, cmd, label, args);
		}
		
		return result;
	}
	
	
	//Actually run the entered command
	public boolean runCommand(CommandSender sender, Command cmd, String label, String[] args) {
		
		if (cmd.getName().equalsIgnoreCase("switch") || cmd.getName().equalsIgnoreCase("sswitch")) {

			if (!sender.hasPermission("iosync.switch")) {
				sender.sendMessage("§cYou are not allowed to use this command.");
				return true;
			}

			Player target = null;
			String destination = "";

			if (args.length >= 2) {
				destination = args[0];
				target = getServer().getPlayer(args[1]);

				//Target not found, maybe the player inverted the two options?
				if (target == null) {
					target = getServer().getPlayer(args[0]);
					destination = args[1];
				}
			} else if (args.length == 1) {
				if (!(sender instanceof Player))
					return false;

				destination = args[0];
				target = (Player)sender;
			} else {
				sender.sendMessage("§cMissing parameter: destination server");
				return true;
			}

			if (target == null) {
				sender.sendMessage("§cTarget player not found!");
				return true;
			}

			//Emit silent switch signal
			if (cmd.getName().equalsIgnoreCase("sswitch")) {
				getLogger().info("|IOCMD|sswitch " + target.getDisplayName());
			}


			if (!apiService.isEmpty())
				return switchSupport.requestSwitch(target, destination);
			else
				return switchSupport.executeSwitch(target, destination);

		} else if (cmd.getName().equalsIgnoreCase("lobby")) {

			Player target = (Player)sender;
			String lobby = "lobby";

			return switchSupport.executeSwitch(target, lobby);
		}

		return false;
	}


	//Get the URL of the API service, if available
	public String getApiServer() {
		return apiService;
	}


	public Players getPlayersInst() {
		return thisPlayers;
	}
}