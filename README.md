# IOSync

![Logo](https://www.interordi.com/images/plugins/iosync-96.png)

This Bukkit plugin synchronizes a player's data across multiple Minecraft server instances. This is especially useful in a BungeeCord setting, where players want a continuous experience moving between multiple server instances.

This is done by copying a player's data file (`world/playerdata/UUID.dat`) to a common directory when they disconnect. On reconnection, on the same server or another, the file is copied in place before the server's processes try to read from it. Effectively, the player's data travels with them.

Multiple groups of synchronization on the same network can be done; for example, all survival servers vs all minigame servers. This is possibly by defining different save directories in the config. All of the servers using the same directory will be put in a common pool to share that data.

One notable exception comes from player positions, which must NOT be shared across servers. As such, these are stored separately in data files.

For server owners on POSIX-compatible systems (Linux and others), you can set the `player-permissions` config value to set the file permissions that the player data should have on saving to the shared directory. Minecraft versions up to 1.20.1 set read permissions to the group; this is no longer the case. This lets you revert this behavior. Don't use unless you know what you're doing, you could break the sync process!

Note that this plugin benefits from an update on each protocol version change (usually game version updates) of Minecraft. It doesn't tend to outright break, but a warning on boot will be shown.


## Setup guide

1. Download the plugin and place it in the `plugins/` directory.
2. Start and stop the server to generate the configuration file.
3. Edit `config.yml` to set `storage-path`, which is a directory path. Servers with the same `storage-path` value will share their player data. Make sure that the directory exists and is writable!
4. Start your server. That's it, you're done!


## Current limitations

* All game servers must be in the same file system (multiple hardware or separate Docker instances not supported).
* On first join after installation, players might be sent to spawn. This can happen only once per player per server.


## Configuration

`storage-path`: The directory to use to store the players data. Servers with the same storage-path value will share their player data. If left empty, no synchronisation will happen. This can be useful to have the `/switch` method available.  
`player-permissions`: The POSIX file permissions to apply to the saved player data. Format: `rw-r--r--`. Defaults to null, applying no changes. Don't use unless you know what you're doing!  


## Commands

`/switch`: Used by authorized players to change servers. This has to be used instead of BungeeCord's `/server` to ensure that the player's data is properly saved before the disconnection registers.  


## Permissions

`iosync.switch`: Access to the server switch command.  
