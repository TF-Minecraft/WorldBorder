package net.tfminecraft.worldborder.cache;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.tfminecraft.worldborder.border.Region;

/**
 * Runtime scalars from config.yml.
 */
public final class Cache {

    private Cache() {}

    public static int tickIntervalTicks = 10;
    public static double damage = 8.0;
    public static int graceInset = 20;
    public static String titleWarning = "§cTurn back";
    public static String subtitleWarning = "§cYou feel continuing further would be very dangerous...";
    /**
     * Fastest a player can travel, in blocks per second. Only used to warn when a
     * lethal band is narrow enough to cross between two border checks. The default
     * is roughly the most the server's movement check normally accepts from an elytra
     * flight (about 17 blocks a tick). It is a guideline, not a hard limit: lag or a
     * client that holds back packets can move further, so keep bands well above it.
     */
    public static double maxTravelSpeed = 350.0;

    /**
     * When empty, borders apply to any world with an entry in {@link #borders}.
     * When non-empty, only listed worlds that also have a border entry are enforced.
     */
    public static List<String> worlds = new ArrayList<>();

    public static Map<String, Region> borders = new LinkedHashMap<>();

    public static Region borderForWorld(String worldName) {
        if (!worlds.isEmpty() && !worlds.contains(worldName)) {
            return null;
        }
        return borders.get(worldName);
    }
}
