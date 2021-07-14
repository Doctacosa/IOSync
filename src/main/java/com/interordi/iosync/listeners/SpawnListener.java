package com.interordi.iosync.listeners;

import com.interordi.iosync.IOSync;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerRespawnEvent;


public class SpawnListener implements Listener {

	IOSync plugin;
	boolean enablePositionSaving = false;
	
	
	public SpawnListener(IOSync plugin, boolean enablePositionSaving) {
		this.plugin = plugin;
		this.enablePositionSaving = enablePositionSaving;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}


	@EventHandler
	public void onPlayerBedEnter(PlayerBedEnterEvent event) {
		plugin.getPlayersInst().setPlayerSpawn(event.getPlayer(), event.getBed().getLocation());
	}


	@EventHandler
	public void onBlockBreakEvent(BlockBreakEvent event) {

		//Only check for beds
		Material mat = event.getBlock().getType();
		if (mat != Material.BLACK_BED &&
			mat != Material.BLUE_BED &&
			mat != Material.GREEN_BED &&
			mat != Material.CYAN_BED &&
			mat != Material.BROWN_BED &&
			mat != Material.PURPLE_BED &&
			mat != Material.ORANGE_BED &&
			mat != Material.LIGHT_GRAY_BED &&
			mat != Material.GRAY_BED &&
			mat != Material.LIGHT_BLUE_BED &&
			mat != Material.LIME_BED &&
			mat != Material.RED_BED &&
			mat != Material.MAGENTA_BED &&
			mat != Material.PINK_BED &&
			mat != Material.YELLOW_BED &&
			mat != Material.WHITE_BED)
			return;


		//Get the coordinates of the head of the bed
		Location loc = event.getBlock().getLocation();

		BlockData metadata = event.getBlock().getBlockData();
		Bed bedData = (Bed)metadata;
		if (bedData.getPart() == Bed.Part.FOOT) {
			switch (bedData.getFacing()) {
				case NORTH:
					loc.add(0, 0, -1);
				break;
				case SOUTH:
					loc.add(0, 0, 1);
				break;
				case WEST:
					loc.add(-1, 0, 0);
				break;
				case EAST:
					loc.add(1, 0, 0);
				break;
				default:
					System.err.println("Impossible bed direction: " + bedData.getFacing());
				break;
			}
		}
		
		//Check if a respawn location must be unset
		plugin.getPlayersInst().bedBroken(loc);
	}


	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		Location respawnLocation = plugin.getPlayersInst().getPlayerSpawn(event.getPlayer().getUniqueId());
		if (respawnLocation != null)
			event.setRespawnLocation(respawnLocation);
		else
			event.setRespawnLocation(event.getPlayer().getWorld().getSpawnLocation());
	}

}