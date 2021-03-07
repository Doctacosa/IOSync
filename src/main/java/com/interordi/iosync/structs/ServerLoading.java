package com.interordi.iosync.structs;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

public class ServerLoading {

	public Set< Player > players;
	public String server;
	public Instant loading;
	public BossBar bar;


	public ServerLoading(Player player, String server) {

		this.players = new HashSet< Player >();

		this.server = server;
		this.players.add(player);
		this.loading = Instant.now();
		this.bar = Bukkit.createBossBar("Loading world", BarColor.GREEN, BarStyle.SOLID);
		this.bar.addPlayer(player);
		this.bar.setProgress(0.0);
	}


	public void addPlayer(Player player) {
		this.bar.addPlayer(player);
	}


	public void removePlayer(Player player) {
		this.bar.removePlayer(player);
	}
}
