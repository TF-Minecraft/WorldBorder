package net.tfminecraft.worldborder.border;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import net.tfminecraft.worldborder.WorldBorder;
import net.tfminecraft.worldborder.cache.Cache;

class RespawnGuardTest {

    private final WorldBorder savedPlugin = WorldBorder.plugin;
    private final int savedInset = Cache.graceInset;
    private final List<String> savedWorlds = Cache.worlds;
    private final Map<String, Region> savedBorders = Cache.borders;

    private final RespawnGuard guard = new RespawnGuard();
    private Logger logger;
    private World world;
    private Location spawn;
    private Player player;

    @BeforeEach
    void setUp() {
        WorldBorder.plugin = mock(WorldBorder.class);
        logger = mock(Logger.class);
        when(WorldBorder.plugin.getLogger()).thenReturn(logger);
        Cache.graceInset = 10;
        Cache.worlds = new ArrayList<>();
        Cache.borders = new LinkedHashMap<>();
        Cache.borders.put("map", new Region("map", 0, 100, 0, 100, 50, 500));
        world = mock(World.class);
        when(world.getName()).thenReturn("map");
        spawn = new Location(world, 50, 64, 50);
        when(world.getSpawnLocation()).thenReturn(spawn);
        player = mock(Player.class);
        when(player.getName()).thenReturn("Wanderer");
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
    }

    @AfterEach
    void tearDown() {
        WorldBorder.plugin = savedPlugin;
        Cache.graceInset = savedInset;
        Cache.worlds = savedWorlds;
        Cache.borders = savedBorders;
    }

    private PlayerRespawnEvent respawnAt(Location location) {
        PlayerRespawnEvent event = mock(PlayerRespawnEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getRespawnLocation()).thenReturn(location);
        return event;
    }

    @Test
    void respawnInTheLethalBandMovesToTheWorldSpawn() {
        PlayerRespawnEvent event = respawnAt(new Location(world, 300, 64, 50));

        guard.onRespawn(event);

        verify(event).setRespawnLocation(spawn);
        verify(logger).info("Moved Wanderer's respawn out of the lethal band (300, 50) to the spawn of map.");
    }

    @ParameterizedTest
    @ValueSource(doubles = {50, 5, 120, 700})
    void respawnOutsideTheLethalBandIsLeftAlone(double x) {
        PlayerRespawnEvent event = respawnAt(new Location(world, x, 64, 50));

        guard.onRespawn(event);

        verify(event, never()).setRespawnLocation(any());
        verify(logger, never()).info(anyString());
    }

    @Test
    void exemptPlayersKeepTheirRespawn() {
        when(player.getGameMode()).thenReturn(GameMode.CREATIVE);
        PlayerRespawnEvent creative = respawnAt(new Location(world, 300, 64, 50));
        guard.onRespawn(creative);
        verify(creative, never()).setRespawnLocation(any());

        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        when(player.hasPermission("worldborder.bypass")).thenReturn(true);
        PlayerRespawnEvent bypassed = respawnAt(new Location(world, 300, 64, 50));
        guard.onRespawn(bypassed);
        verify(bypassed, never()).setRespawnLocation(any());
    }

    @Test
    void worldsWithoutABorderOrWithoutAWorldAreLeftAlone() {
        World other = mock(World.class);
        when(other.getName()).thenReturn("elsewhere");
        PlayerRespawnEvent unbordered = respawnAt(new Location(other, 300, 64, 50));
        guard.onRespawn(unbordered);
        verify(unbordered, never()).setRespawnLocation(any());

        PlayerRespawnEvent worldless = respawnAt(new Location(null, 300, 64, 50));
        guard.onRespawn(worldless);
        verify(worldless, never()).setRespawnLocation(any());
    }
}
