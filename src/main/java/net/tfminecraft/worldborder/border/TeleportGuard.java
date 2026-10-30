package net.tfminecraft.worldborder.border;

import java.util.EnumSet;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import net.tfminecraft.worldborder.WorldBorder;
import net.tfminecraft.worldborder.cache.Cache;

/**
 * The border is only checked every few ticks, so a jump that lands past the lethal band
 * (an ender pearl cannon, chorus fruit, a portal) would never be seen inside it. Teleports
 * players set off themselves can't land in the lethal band or past the buffer; staff and
 * plugin teleports (COMMAND, PLUGIN) still can, which is how players reach far builds.
 */
public final class TeleportGuard implements Listener {

    static final Set<TeleportCause> PLAYER_TELEPORTS = EnumSet.of(
            TeleportCause.ENDER_PEARL,
            TeleportCause.CONSUMABLE_EFFECT,
            TeleportCause.NETHER_PORTAL,
            TeleportCause.END_PORTAL,
            TeleportCause.END_GATEWAY);

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        check(event);
    }

    // Portal events have their own handler list, so they need their own handler.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) {
        check(event);
    }

    private void check(PlayerTeleportEvent event) {
        if (!PLAYER_TELEPORTS.contains(event.getCause()) || !BorderManager.affects(event.getPlayer())) {
            return;
        }
        Location to = event.getTo();
        World world = to == null ? null : to.getWorld();
        Region region = world == null ? null : Cache.borderForWorld(world.getName());
        if (region == null) {
            return;
        }
        Region.Zone zone = region.zoneAt(to.getX(), to.getZ(), Cache.graceInset);
        if (zone != Region.Zone.LETHAL && zone != Region.Zone.BEYOND) {
            return;
        }
        Location from = event.getFrom();
        if (world.equals(from.getWorld())
                && region.zoneAt(from.getX(), from.getZ(), Cache.graceInset) == Region.Zone.BEYOND) {
            // Already out past the buffer, e.g. a pearl thrown around a far build.
            return;
        }
        event.setCancelled(true);
        WorldBorder.plugin.getLogger().info("Stopped " + event.getPlayer().getName() + "'s "
                + event.getCause() + " teleport to " + zone + " (" + to.getBlockX() + ", " + to.getBlockZ()
                + ") in " + world.getName() + ".");
    }
}
