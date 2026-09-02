package net.tfminecraft.worldborder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.bukkit.plugin.java.JavaPlugin;

import net.tfminecraft.worldborder.cache.Cache;
import net.tfminecraft.worldborder.border.BorderManager;
import net.tfminecraft.worldborder.command.CommandManager;
import net.tfminecraft.worldborder.loader.ConfigLoader;

public class WorldBorder extends JavaPlugin {

    public static WorldBorder plugin;

    private final ConfigLoader configLoader = new ConfigLoader();
    private final BorderManager borderManager = new BorderManager(this);
    private final CommandManager commandManager = new CommandManager();

    @Override
    public void onEnable() {
        plugin = this;
        createFolders();
        createConfigs();
        loadConfigs();
        var cmd = getCommand(commandManager.cmd);
        if (cmd != null) {
            cmd.setExecutor(commandManager);
            cmd.setTabCompleter(commandManager);
        } else {
            getLogger().severe("Command 'worldborder' missing from plugin.yml");
        }
        borderManager.start();
        getLogger().info("WorldBorder enabled.");
    }

    @Override
    public void onDisable() {
        borderManager.stop();
    }

    public void reloadAll() {
        loadConfigs();
        borderManager.start();
    }

    private void loadConfigs() {
        configLoader.load(new File(getDataFolder(), "config.yml"));
        getLogger().info("Loaded " + Cache.borders.size() + " world border(s).");
    }

    private void createFolders() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
    }

    private void createConfigs() {
        copyResourceIfMissing("config.yml");
    }

    private void copyResourceIfMissing(String relativePath) {
        File target = new File(getDataFolder(), relativePath);
        if (target.exists()) {
            return;
        }
        target.getParentFile().mkdirs();
        try (InputStream in = getResource(relativePath)) {
            if (in == null) {
                getLogger().warning("Missing bundled resource: " + relativePath);
                return;
            }
            Files.copy(in, target.toPath());
        } catch (IOException ex) {
            getLogger().severe("Failed to copy default resource " + relativePath + ": " + ex.getMessage());
        }
    }
}
