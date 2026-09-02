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
                borders.put(worldKey, new Region(worldKey, minX, maxX, minZ, maxZ));
            }
        }
        Cache.borders = borders;
    }
}
