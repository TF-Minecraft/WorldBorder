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
        event.setRespawnLocation(world.getSpawnLocation());
        WorldBorder.plugin.getLogger().info("Moved " + event.getPlayer().getName()
                + "'s respawn out of the lethal band (" + to.getBlockX() + ", " + to.getBlockZ()
                + ") to the spawn of " + world.getName() + ".");
    }
}
