package net.tfminecraft.worldborder.cache;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.tfminecraft.worldborder.border.Region;

class CacheTest {
    private int originalInterval;
    private double originalDamage;
    private int originalInset;
    private String originalTitle;
    private String originalSubtitle;
    private List<String> originalWorlds;
    private Map<String, Region> originalBorders;

    @BeforeEach
    void resetState() {
        originalInterval = Cache.tickIntervalTicks;
        originalDamage = Cache.damage;
        originalInset = Cache.graceInset;
        originalTitle = Cache.titleWarning;
        originalSubtitle = Cache.subtitleWarning;
        originalWorlds = Cache.worlds;
        originalBorders = Cache.borders;
        Cache.tickIntervalTicks = 10;
        Cache.damage = 8.0;
        Cache.graceInset = 20;
        Cache.titleWarning = "§cTurn back";
        Cache.subtitleWarning = "§cYou feel continuing further would be very dangerous...";
        Cache.worlds = new ArrayList<>();
        Cache.borders = new LinkedHashMap<>();
    }

    @AfterEach
    void restoreState() {
        Cache.tickIntervalTicks = originalInterval;
        Cache.damage = originalDamage;
        Cache.graceInset = originalInset;
        Cache.titleWarning = originalTitle;
        Cache.subtitleWarning = originalSubtitle;
        Cache.worlds = originalWorlds;
        Cache.borders = originalBorders;
    }

    @Test
    void emptyAllowListReturnsAnyConfiguredWorld() {
        Region overworld = new Region("world", -100, 100, -100, 100);
        Region nether = new Region("world_nether", -20, 20, -20, 20);
        Cache.borders.put("world", overworld);
        Cache.borders.put("world_nether", nether);

        assertSame(overworld, Cache.borderForWorld("world"));
        assertSame(nether, Cache.borderForWorld("world_nether"));
        assertNull(Cache.borderForWorld("unconfigured"));
    }

    @Test
    void nonemptyAllowListRequiresBothMembershipAndABorder() {
        Region allowed = new Region("allowed", 0, 100, 0, 100);
        Cache.worlds.add("allowed");
        Cache.worlds.add("missing-border");
        Cache.borders.put("allowed", allowed);
        Cache.borders.put("excluded", new Region("excluded", 0, 100, 0, 100));

        assertSame(allowed, Cache.borderForWorld("allowed"));
        assertNull(Cache.borderForWorld("missing-border"));
        assertNull(Cache.borderForWorld("excluded"));
    }

    @Test
    void worldNamesMatchExactly() {
        Region border = new Region("World", 0, 100, 0, 100);
        Cache.borders.put("World", border);

        assertSame(border, Cache.borderForWorld("World"));
        assertNull(Cache.borderForWorld("world"));
        Cache.worlds.add("world");
        assertNull(Cache.borderForWorld("World"));
    }

    @Test
    void lookupsUseTheCurrentConfigurationAfterReplacement() {
        Cache.worlds.add("old");
        Cache.borders.put("old", new Region("old", 0, 100, 0, 100));
        Region replacement = new Region("new", -30, 30, -30, 30);
        Cache.worlds = new ArrayList<>(List.of("new"));
        Cache.borders = new LinkedHashMap<>(Map.of("new", replacement));

        assertNull(Cache.borderForWorld("old"));
        assertSame(replacement, Cache.borderForWorld("new"));
    }

    @Test
    void utilityConstructorIsPrivateAndDoesNotReplaceSharedState() throws Exception {
        Constructor<Cache> constructor = Cache.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        List<String> worlds = Cache.worlds;
        Map<String, Region> borders = Cache.borders;

        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());

        assertSame(worlds, Cache.worlds);
        assertSame(borders, Cache.borders);
    }
}
