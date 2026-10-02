package net.tfminecraft.worldborder;

import net.tfminecraft.worldborder.cache.Cache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

class WorldBorderTest {
    @TempDir Path directory;
    private ServerMock server;
    private WorldBorder plugin;

    @BeforeEach
    void setUp() {
        Cache.worlds = new ArrayList<>();
        Cache.borders = new LinkedHashMap<>();
        Cache.tickIntervalTicks = 10;
        Cache.damage = 8.0;
        Cache.graceInset = 20;
        Cache.titleWarning = "§cTurn back";
        Cache.subtitleWarning = "§cYou feel continuing further would be very dangerous...";
        server = MockBukkit.mock();
        plugin = MockBukkit.load(WorldBorder.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
        WorldBorder.plugin = null;
        Cache.worlds = new ArrayList<>();
        Cache.borders = new LinkedHashMap<>();
    }

    @Test
    void enablesWithBundledConfigAndCommand() {
        assertSame(plugin, WorldBorder.plugin);
        assertTrue(Files.isRegularFile(plugin.getDataFolder().toPath().resolve("config.yml")));
        assertEquals(10, Cache.tickIntervalTicks);
        assertEquals(8.0, Cache.damage);
        assertNotNull(plugin.getCommand("worldborder").getExecutor());
        assertSame(plugin.getCommand("worldborder").getExecutor(),
                plugin.getCommand("worldborder").getTabCompleter());
        server.getScheduler().performTicks(10);
    }

    @Test
    void reloadsExistingConfigWithoutReplacingIt() throws Exception {
        Path config = plugin.getDataFolder().toPath().resolve("config.yml");
        String custom = "tick-interval-ticks: 5\ndamage: 3\nborders: {}\n";
        Files.writeString(config, custom);
        plugin.reloadAll();
        assertEquals(5, Cache.tickIntervalTicks);
        assertEquals(3, Cache.damage);
        plugin.onEnable();
        assertEquals(custom, Files.readString(config));
        plugin.onDisable();
        plugin.onDisable();
    }

    @Test
    void createsDataDirectoryAndReportsMissingCommand() throws Exception {
        WorldBorder controlled = spy(plugin);
        Path data = directory.resolve("new-data");
        doReturn(data.toFile()).when(controlled).getDataFolder();
        doReturn(null).when(controlled).getCommand("worldborder");
        Logger logger = mock(Logger.class);
        doReturn(logger).when(controlled).getLogger();
        controlled.onEnable();
        assertTrue(Files.isRegularFile(data.resolve("config.yml")));
        verify(logger).severe("Command 'worldborder' missing from plugin.yml");
        controlled.onDisable();
    }

    @Test
    void reportsMissingBundledResource() {
        WorldBorder controlled = spy(plugin);
        doReturn(directory.toFile()).when(controlled).getDataFolder();
        doReturn(null).when(controlled).getResource("config.yml");
        Logger logger = mock(Logger.class);
        doReturn(logger).when(controlled).getLogger();
        controlled.onEnable();
        assertFalse(Files.exists(directory.resolve("config.yml")));
        verify(logger).warning("Missing bundled resource: config.yml");
        verify(logger).severe(contains("Failed to load config.yml:"));
        controlled.onDisable();
    }

    @Test
    void reportsResourceReadFailureAndClosesItsStream() throws Exception {
        WorldBorder controlled = spy(plugin);
        doReturn(directory.toFile()).when(controlled).getDataFolder();
        InputStream broken = mock(InputStream.class);
        when(broken.transferTo(any())).thenThrow(new IOException("read failed"));
        doReturn(broken).when(controlled).getResource("config.yml");
        Logger logger = mock(Logger.class);
        doReturn(logger).when(controlled).getLogger();
        controlled.onEnable();
        verify(logger).severe("Failed to copy default resource config.yml: read failed");
        verify(broken).close();
        controlled.onDisable();
    }
}
