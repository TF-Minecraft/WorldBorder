package net.tfminecraft.worldborder.loader;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import net.tfminecraft.worldborder.WorldBorder;
import net.tfminecraft.worldborder.border.Region;
import net.tfminecraft.worldborder.cache.Cache;

public final class ConfigLoader {

    public void load(File configFile) {
        FileConfiguration config = new YamlConfiguration();
        try {
            config.load(configFile);
        } catch (IOException | InvalidConfigurationException ex) {
            WorldBorder.plugin.getLogger().severe("Failed to load config.yml: " + ex.getMessage());
            return;
        }

        Cache.tickIntervalTicks = Math.max(1, config.getInt("tick-interval-ticks", 10));
        Cache.damage = Math.max(0.0, config.getDouble("damage", 8.0));
        Cache.graceInset = Math.max(0, config.getInt("grace-inset", 20));
        Cache.titleWarning = config.getString("title-warning", Cache.titleWarning);
        Cache.subtitleWarning = config.getString("subtitle-warning", Cache.subtitleWarning);
        Cache.maxTravelSpeed = Math.max(0.0, config.getDouble("max-travel-speed", 350.0));

        Cache.worlds = new java.util.ArrayList<>(config.getStringList("worlds"));

        LinkedHashMap<String, Region> borders = new LinkedHashMap<>();
        ConfigurationSection bordersSection = config.getConfigurationSection("borders");
        if (bordersSection != null) {
            for (String worldKey : bordersSection.getKeys(false)) {
                String path = "borders." + worldKey + ".";
                int minX = config.getInt(path + "min-x");
                int maxX = config.getInt(path + "max-x");
                int minZ = config.getInt(path + "min-z");
                int maxZ = config.getInt(path + "max-z");
                if (minX > maxX || minZ > maxZ) {
                    WorldBorder.plugin.getLogger().warning(
                            "Skipping invalid border for world '" + worldKey
                                    + "': min must be <= max (x: " + minX + "-" + maxX
                                    + ", z: " + minZ + "-" + maxZ + ")");
                    continue;
                }
                int lethalDepth = config.getInt(path + "lethal-depth", Region.NO_LETHAL_DEPTH);
                if (lethalDepth < 0) {
                    lethalDepth = Region.NO_LETHAL_DEPTH;
                }
                int bufferWidth = Math.max(Region.ENDLESS_BUFFER,
                        config.getInt(path + "buffer-width", Region.ENDLESS_BUFFER));
                Region region = new Region(worldKey, minX, maxX, minZ, maxZ, lethalDepth, bufferWidth);
                warnIfBufferCanBeCrossed(region);
                borders.put(worldKey, region);
            }
        }
        Cache.borders = borders;
    }

    /**
     * The border is only checked every tick-interval-ticks, so a lethal band narrower than
     * one check's worth of travel can be flown across without ever being seen inside it.
     */
    private void warnIfBufferCanBeCrossed(Region region) {
        if (region.bufferWidth == Region.ENDLESS_BUFFER) {
            return;
        }
        if (region.lethalDepth == Region.NO_LETHAL_DEPTH) {
            WorldBorder.plugin.getLogger().warning(
                    "Border for world '" + region.world + "' has buffer-width " + region.bufferWidth
                            + " but no lethal-depth: anyone who survives the damage can cross the buffer.");
            return;
        }
        double minimum = Cache.maxTravelSpeed * Cache.tickIntervalTicks / 20.0;
        if (region.lethalBandWidth() < minimum) {
            WorldBorder.plugin.getLogger().warning(
                    "Border for world '" + region.world + "' has a lethal band of " + region.lethalBandWidth()
                            + " blocks (lethal-depth " + region.lethalDepth + " to buffer-width " + region.bufferWidth
                            + "): at max-travel-speed " + Cache.maxTravelSpeed + " it needs at least "
                            + minimum + " blocks so no one crosses it between checks.");
        }
    }
}
