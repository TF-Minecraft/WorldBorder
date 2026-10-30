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
        Location safe = world.getSpawnLocation();
        String where = "the spawn of " + world.getName();
        if (region.zoneAt(safe.getX(), safe.getZ(), Cache.graceInset) == Region.Zone.LETHAL) {
            // The world spawn is out in the band too, so use the middle of the border.
            int x = (int) Math.floorDiv((long) region.minX + region.maxX, 2L);
            int z = (int) Math.floorDiv((long) region.minZ + region.maxZ, 2L);
            safe = new Location(world, x + 0.5, world.getHighestBlockYAt(x, z) + 1, z + 0.5);
            where = "the centre of the " + world.getName() + " border";
        }
        event.setRespawnLocation(safe);
        WorldBorder.plugin.getLogger().info("Moved " + event.getPlayer().getName()
                + "'s respawn out of the lethal band (" + to.getBlockX() + ", " + to.getBlockZ()
                + ") to " + where + ".");
    }
}
