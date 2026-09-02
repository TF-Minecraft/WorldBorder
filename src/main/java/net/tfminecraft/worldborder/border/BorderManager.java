package net.tfminecraft.worldborder.border;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import net.tfminecraft.worldborder.WorldBorder;
import net.tfminecraft.worldborder.cache.Cache;

public final class BorderManager {

    private final WorldBorder plugin;
    private final Set<UUID> warningShown = new HashSet<>();
    private BukkitTask task;

    public BorderManager(WorldBorder plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        long interval = Cache.tickIntervalTicks;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, interval, interval);
        plugin.getLogger().info("Border tick started (interval=" + interval + " ticks).");
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        warningShown.clear();
    }

    private void tick() {
        pruneOfflineWarnings();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isDead()) {
                continue;
            }

            if (!affects(player)) {
                clearWarning(player);
                continue;
            }

            Region region = Cache.borderForWorld(player.getWorld().getName());
            if (region == null) {
                clearWarning(player);
                continue;
            }

            Region.Zone zone = region.zoneAt(
                    player.getLocation().getX(),
                    player.getLocation().getZ(),
                    Cache.graceInset);

            switch (zone) {
                case OUTSIDE -> {
                    clearWarning(player);
                    if (Cache.damage > 0) {
                        player.damage(Cache.damage);
                    }
                }
                case WARNING -> {
                    warningShown.add(player.getUniqueId());
                    player.sendTitle(
                            Cache.titleWarning,
                            Cache.subtitleWarning,
                            0,
                            15,
                            0);
                }
                case SAFE -> clearWarning(player);
            }
        }
    }

    private void pruneOfflineWarnings() {
        Iterator<UUID> iterator = warningShown.iterator();
        while (iterator.hasNext()) {
            UUID id = iterator.next();
            Player player = Bukkit.getPlayer(id);
            if (player == null || !player.isOnline()) {
                iterator.remove();
            }
        }
    }

    private void clearWarning(Player player) {
        if (warningShown.remove(player.getUniqueId())) {
            player.resetTitle();
        }
    }

    private boolean affects(Player player) {
        GameMode mode = player.getGameMode();
        if (mode != GameMode.SURVIVAL && mode != GameMode.ADVENTURE) {
            return false;
        }
        if (player.hasPermission("worldborder.bypass")) {
            return false;
        }
        return true;
    }
}
