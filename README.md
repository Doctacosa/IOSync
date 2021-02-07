# IOSync

This Bukkit plugin synchronizes a player's data across multiple Minecraft server instances. This is especially useful in a BungeeCord setting, where players want a continuous experience moving between multiple server instances.

This is done by copying a player's data file (`world/playerdata/UUID.dat`) to a common directory when they disconnect. On reconnection, on the same server or another, the file is copied in place before the server's processes try to read from it. Effectively, the player's data travels with them.

Multiple groups of synchronization on the same network can be done; for example, all survival servers vs all minigame servers. This is possibly by defining different save directories in the config. All of the servers using the same directory will be put in a common pool to share that data.

One notable exception comes from player positions, which must NOT be shared accross servers. To keep this constant between visits, the position of each player is stored in `positions.yml`.


## Configuration

`storage-path`: The directory to use to store the players data. If left empty, no synchronisation will happen. This can be useful to have the `/switch` method available.


## Commands

`switch`: Used by authorized players to change servers. This has to be used instead of BungeeCord's `/server` to ensure that the player's data is properly saved before the disconnection registers.


## Permissions

`iosync.switch`: Access to the server switch command.
