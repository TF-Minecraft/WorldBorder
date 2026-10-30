package net.tfminecraft.worldborder.border;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import net.tfminecraft.worldborder.WorldBorder;
import net.tfminecraft.worldborder.cache.Cache;

class TeleportGuardTest {

    private final WorldBorder savedPlugin = WorldBorder.plugin;
    private final int savedInset = Cache.graceInset;
    private final List<String> savedWorlds = Cache.worlds;
    private final Map<String, Region> savedBorders = Cache.borders;

    private final TeleportGuard guard = new TeleportGuard();
    private Logger logger;
    private World world;
    private Player player;

    @BeforeEach
    void setUp() {
        WorldBorder.plugin = mock(WorldBorder.class);
        logger = mock(Logger.class);
        when(WorldBorder.plugin.getLogger()).thenReturn(logger);
        Cache.graceInset = 10;
        Cache.worlds = new ArrayList<>();
        Cache.borders = new LinkedHashMap<>();
        // Lethal from 50 to 500 blocks past x/z 0-100.
        Cache.borders.put("map", new Region("map", 0, 100, 0, 100, 50, 500));
        world = mock(World.class);
        when(world.getName()).thenReturn("map");
        player = mock(Player.class);
        when(player.getName()).thenReturn("Jumper");
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
    }

    @AfterEach
    void tearDown() {
        WorldBorder.plugin = savedPlugin;
        Cache.graceInset = savedInset;
        Cache.worlds = savedWorlds;
        Cache.borders = savedBorders;
    }

    private PlayerTeleportEvent teleport(double fromX, double toX, TeleportCause cause) {
        return new PlayerTeleportEvent(player, new Location(world, fromX, 64, 50),
                new Location(world, toX, 64, 50), cause);
    }

    @ParameterizedTest
    @EnumSource(value = TeleportCause.class,
            names = {"ENDER_PEARL", "CONSUMABLE_EFFECT", "NETHER_PORTAL", "END_PORTAL", "END_GATEWAY"})
    void playerTeleportsCannotLandInTheLethalBandOrPastTheBuffer(TeleportCause cause) {
        PlayerTeleportEvent intoBand = teleport(50, 300, cause);
        guard.onTeleport(intoBand);
        assertTrue(intoBand.isCancelled());

        PlayerTeleportEvent overBand = teleport(50, 2000, cause);
        guard.onTeleport(overBand);
        assertTrue(overBand.isCancelled());
        verify(logger).info("Stopped Jumper's " + cause + " teleport to BEYOND (2000, 50) in map.");
    }

    @Test
    void portalEventsAreCheckedToo() {
        PlayerPortalEvent portal = new PlayerPortalEvent(player, new Location(world, 50, 64, 50),
                new Location(world, 2000, 64, 50), TeleportCause.NETHER_PORTAL);
        guard.onPortal(portal);
        assertTrue(portal.isCancelled());
    }

    @ParameterizedTest
    @EnumSource(value = TeleportCause.class,
            names = {"COMMAND", "PLUGIN", "SPECTATE", "DISMOUNT", "EXIT_BED", "UNKNOWN"})
    void staffAndPluginTeleportsReachFarBuilds(TeleportCause cause) {
        PlayerTeleportEvent event = teleport(50, 2000, cause);
        guard.onTeleport(event);
        assertFalse(event.isCancelled());
    }

    @Test
    void pearlsInsideTheMapOrIntoTheDamageBandAreAllowed() {
        PlayerTeleportEvent inside = teleport(50, 60, TeleportCause.ENDER_PEARL);
        guard.onTeleport(inside);
        assertFalse(inside.isCancelled());

        PlayerTeleportEvent damageBand = teleport(90, 130, TeleportCause.ENDER_PEARL);
        guard.onTeleport(damageBand);
        assertFalse(damageBand.isCancelled());
        verify(logger, never()).info(anyString());
    }

    @Test
    void playersAlreadyPastTheBufferCanPearlAroundThere() {
        PlayerTeleportEvent aroundBuild = teleport(2000, 2030, TeleportCause.ENDER_PEARL);
        guard.onTeleport(aroundBuild);
        assertFalse(aroundBuild.isCancelled());
    }

    @Test
    void comingFromAnotherWorldStillCounts() {
        World other = mock(World.class);
        when(other.getName()).thenReturn("nether");
        PlayerTeleportEvent portal = new PlayerTeleportEvent(player, new Location(other, 2000, 64, 50),
                new Location(world, 2000, 64, 50), TeleportCause.NETHER_PORTAL);
        guard.onTeleport(portal);
        assertTrue(portal.isCancelled());
    }

    @Test
    void exemptPlayersAndUnborderedOrMissingDestinationsAreLeftAlone() {
        when(player.getGameMode()).thenReturn(GameMode.CREATIVE);
        PlayerTeleportEvent creative = teleport(50, 2000, TeleportCause.ENDER_PEARL);
        guard.onTeleport(creative);
        assertFalse(creative.isCancelled());
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);

        World other = mock(World.class);
        when(other.getName()).thenReturn("elsewhere");
        PlayerTeleportEvent unbordered = new PlayerTeleportEvent(player, new Location(world, 50, 64, 50),
                new Location(other, 2000, 64, 50), TeleportCause.ENDER_PEARL);
        guard.onTeleport(unbordered);
        assertFalse(unbordered.isCancelled());

        PlayerTeleportEvent worldless = new PlayerTeleportEvent(player, new Location(world, 50, 64, 50),
                new Location(null, 2000, 64, 50), TeleportCause.ENDER_PEARL);
        guard.onTeleport(worldless);
        assertFalse(worldless.isCancelled());

        PlayerPortalEvent nowhere = new PlayerPortalEvent(player, new Location(world, 50, 64, 50), null,
                TeleportCause.NETHER_PORTAL);
        guard.onPortal(nowhere);
        assertFalse(nowhere.isCancelled());
    }
}
