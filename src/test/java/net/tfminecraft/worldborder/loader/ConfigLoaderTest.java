package net.tfminecraft.worldborder.loader;

import net.tfminecraft.worldborder.WorldBorder;
import net.tfminecraft.worldborder.border.Region;
import net.tfminecraft.worldborder.cache.Cache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

class ConfigLoaderTest {
    @TempDir Path directory;
    private final ConfigLoader loader = new ConfigLoader();
    private Logger logger;
    private WorldBorder previousPlugin;
    private int previousTicks;
    private double previousDamage;
    private int previousInset;
    private String previousTitle;
    private String previousSubtitle;
    private List<String> previousWorlds;
    private java.util.Map<String, Region> previousBorders;

    @BeforeEach
    void setUp() {
        previousPlugin = WorldBorder.plugin;
        previousTicks = Cache.tickIntervalTicks;
        previousDamage = Cache.damage;
        previousInset = Cache.graceInset;
        previousTitle = Cache.titleWarning;
        previousSubtitle = Cache.subtitleWarning;
        previousWorlds = Cache.worlds;
        previousBorders = Cache.borders;
        WorldBorder.plugin = mock(WorldBorder.class);
        logger = mock(Logger.class);
        when(WorldBorder.plugin.getLogger()).thenReturn(logger);
        Cache.titleWarning = "default title";
        Cache.subtitleWarning = "default subtitle";
        Cache.worlds = new ArrayList<>(List.of("old-world"));
        Cache.borders = new LinkedHashMap<>();
        Cache.borders.put("old-world", new Region("old-world", 0, 10, 0, 10));
    }

    @AfterEach
    void tearDown() {
        WorldBorder.plugin = previousPlugin;
        Cache.tickIntervalTicks = previousTicks;
        Cache.damage = previousDamage;
        Cache.graceInset = previousInset;
        Cache.titleWarning = previousTitle;
        Cache.subtitleWarning = previousSubtitle;
        Cache.worlds = previousWorlds;
        Cache.borders = previousBorders;
    }

    private void load(String yaml) throws Exception {
        Path file = directory.resolve("config.yml");
        Files.writeString(file, yaml);
        loader.load(file.toFile());
    }

    @Test
    void absentSettingsUseDefaultsAndRemovePreviousWorlds() throws Exception {
        load("{}");
        assertEquals(10, Cache.tickIntervalTicks);
        assertEquals(8.0, Cache.damage);
        assertEquals(20, Cache.graceInset);
        assertEquals("default title", Cache.titleWarning);
        assertEquals("default subtitle", Cache.subtitleWarning);
        assertTrue(Cache.worlds.isEmpty());
        assertTrue(Cache.borders.isEmpty());
    }

    @Test
    void loadsScalarsWorldFilterAndInclusiveCoordinates() throws Exception {
        load("""
                tick-interval-ticks: 30
                damage: 2.5
                grace-inset: 4
                title-warning: 'Turn back'
                subtitle-warning: 'Danger ahead'
                worlds: [overworld]
                borders:
                  overworld: {min-x: -10, max-x: 20, min-z: -30, max-z: 40}
                  point: {min-x: 1, max-x: 1, min-z: 2, max-z: 2}
                """);
        assertEquals(30, Cache.tickIntervalTicks);
        assertEquals(2.5, Cache.damage);
        assertEquals(4, Cache.graceInset);
        assertEquals("Turn back", Cache.titleWarning);
        assertEquals("Danger ahead", Cache.subtitleWarning);
        assertEquals(List.of("overworld"), Cache.worlds);
        assertEquals(List.of("overworld", "point"), new ArrayList<>(Cache.borders.keySet()));
        Region border = Cache.borders.get("overworld");
        assertEquals("overworld", border.world);
        assertEquals(-10, border.minX);
        assertEquals(20, border.maxX);
        assertEquals(-30, border.minZ);
        assertEquals(40, border.maxZ);
        Region point = Cache.borders.get("point");
        assertEquals(Region.Zone.WARNING, point.zoneAt(1, 2, 0));
        verifyNoInteractions(logger);
    }

    @Test
    void clampsNegativeSettingsAndSkipsEachInvertedAxis() throws Exception {
        load("""
                tick-interval-ticks: 0
                damage: -2
                grace-inset: -1
                borders:
                  bad-x: {min-x: 5, max-x: 4, min-z: 0, max-z: 1}
                  bad-z: {min-x: 0, max-x: 1, min-z: 5, max-z: 4}
                  valid: {min-x: 0, max-x: 1, min-z: 0, max-z: 1}
                """);
        assertEquals(1, Cache.tickIntervalTicks);
        assertEquals(0.0, Cache.damage);
        assertEquals(0, Cache.graceInset);
        assertEquals(List.of("valid"), new ArrayList<>(Cache.borders.keySet()));
        verify(logger).warning(contains("world 'bad-x'"));
        verify(logger).warning(contains("world 'bad-z'"));
    }

    @Test
    void missingFileLeavesActiveConfigurationUntouched() {
        Cache.tickIntervalTicks = 37;
        loader.load(directory.resolve("missing.yml").toFile());
        assertEquals(37, Cache.tickIntervalTicks);
        assertEquals(List.of("old-world"), Cache.worlds);
        assertEquals(List.of("old-world"), new ArrayList<>(Cache.borders.keySet()));
        verify(logger).severe(contains("Failed to load config.yml:"));
    }

    @Test
    void malformedYamlLeavesActiveConfigurationUntouched() throws Exception {
        Cache.tickIntervalTicks = 37;
        load("borders: [unterminated");
        assertEquals(37, Cache.tickIntervalTicks);
        assertEquals(List.of("old-world"), Cache.worlds);
        assertEquals(List.of("old-world"), new ArrayList<>(Cache.borders.keySet()));
        verify(logger).severe(contains("Failed to load config.yml:"));
    }
}
