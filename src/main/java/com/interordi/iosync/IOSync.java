package com.interordi.iosync;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.interordi.iosync.listeners.LoginListener;
import com.interordi.utilities.CommandTargets;
import com.interordi.utilities.Commands;

public class IOSync extends JavaPlugin {

	LoginListener thisLoginListener;

	private boolean bungeeInit = false;


	public void onEnable() {

		//Always ensure we've got a copy of the config in place (does not overwrite existing)
		this.saveDefaultConfig();

		//Get the paths to copy to and from
		String storagePath = this.getConfig().getString("storage-path");
		String serverPath = this.getConfig().getString("server-path");
		if (!storagePath.endsWith("/") && !storagePath.endsWith("\\"))
			storagePath += "/";
		if (!serverPath.endsWith("/") && !serverPath.endsWith("\\"))
			serverPath += "/";

		thisLoginListener = new LoginListener(this, storagePath, serverPath);

		getLogger().info("IOSync enabled");
	}

	public void onDisable() {
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
		
		if (cmd.getName().equalsIgnoreCase("switch")) {

			if (!sender.hasPermission("iosync.switch")) {
				sender.sendMessage("§cYou are not allowed to use this command.");
				return true;
			}

			Player target = null;
			String destination = "";

			if (args.length >= 2) {
				destination = args[0];
				target = getServer().getPlayer(args[1]);
			} else if (args.length == 1) {
				if (!(sender instanceof Player))
					return false;

				destination = args[0];
				target = (Player)sender;
			} else {
				sender.sendMessage("§cMissing parameter: destination server");
				return true;
			}


			//Send the world Switch message to Bungee
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(b);
			try {
				out.writeUTF("Connect");
				out.writeUTF(destination);
			} catch (IOException e) {
			}

			if (!bungeeInit) {
				this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
				bungeeInit = true;
			}

			//Save players so their inventory and metadata is up to date
			getServer().savePlayers();
			//TODO: Copy file
			target.sendPluginMessage(this, "BungeeCord", b.toByteArray());

			return true;
		}

		return false;
	}
		
}