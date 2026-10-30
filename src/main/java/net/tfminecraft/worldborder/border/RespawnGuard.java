package net.tfminecraft.worldborder.border;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

import net.tfminecraft.worldborder.WorldBorder;
import net.tfminecraft.worldborder.cache.Cache;

/**
 * A bed or anchor in the lethal band would kill the player again every border check,
 * so respawns that would land there go to the world spawn instead.
 */
public final class RespawnGuard implements Listener {

    // Runs after other plugins have picked the respawn point.
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        if (!BorderManager.affects(event.getPlayer())) {
            return;
        }
        Location to = event.getRespawnLocation();
        World world = to.getWorld();
        Region region = world == null ? null : Cache.borderForWorld(world.getName());
        if (region == null || region.zoneAt(to.getX(), to.getZ(), Cache.graceInset) != Region.Zone.LETHAL) {
            return;
        }
        Location safe = safeSpot(world, region);
        event.setRespawnLocation(safe);
        WorldBorder.plugin.getLogger().info("Moved " + event.getPlayer().getName()
                + "'s respawn out of the lethal band (" + to.getBlockX() + ", " + to.getBlockZ()
                + ") to " + describe(world, safe) + ".");
    }

    /** The world spawn, or the middle of the border when the spawn is out in the lethal band too. */
    static Location safeSpot(World world, Region region) {
        Location spawn = world.getSpawnLocation();
        if (region.zoneAt(spawn.getX(), spawn.getZ(), Cache.graceInset) != Region.Zone.LETHAL) {
            return spawn;
        }
        int x = (int) Math.floorDiv((long) region.minX + region.maxX, 2L);
        int z = (int) Math.floorDiv((long) region.minZ + region.maxZ, 2L);
        return new Location(world, x + 0.5, world.getHighestBlockYAt(x, z) + 1, z + 0.5);
    }

    static String describe(World world, Location safe) {
        return safe.equals(world.getSpawnLocation())
                ? "the spawn of " + world.getName()
                : "the centre of the " + world.getName() + " border";
    }
}
