package net.tfminecraft.worldborder.border;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import net.tfminecraft.worldborder.WorldBorder;
import net.tfminecraft.worldborder.cache.Cache;

@SuppressWarnings("deprecation")
class BorderManagerTest {

    private final int savedInterval = Cache.tickIntervalTicks;
    private final double savedDamage = Cache.damage;
    private final int savedInset = Cache.graceInset;
    private final String savedTitle = Cache.titleWarning;
    private final String savedSubtitle = Cache.subtitleWarning;
    private final List<String> savedWorlds = Cache.worlds;
    private final Map<String, Region> savedBorders = Cache.borders;

    private final AtomicReference<Runnable> scheduledTick = new AtomicReference<>();
    private final List<Player> onlinePlayers = new ArrayList<>();
    private MockedStatic<Bukkit> bukkit;
    private WorldBorder plugin;
    private BukkitScheduler scheduler;
    private BukkitTask task;
    private BorderManager manager;
    private World world;

    @BeforeEach
    void setUp() {
        Cache.tickIntervalTicks = 7;
        Cache.damage = 3.5;
        Cache.graceInset = 10;
        Cache.titleWarning = "Border warning";
        Cache.subtitleWarning = "Return to safety";
        Cache.worlds = new ArrayList<>();
        Cache.borders = new LinkedHashMap<>();
        Cache.borders.put("overworld", new Region("overworld", 0, 100, 0, 100));

        bukkit = mockStatic(Bukkit.class);
        scheduler = mock(BukkitScheduler.class);
        task = mock(BukkitTask.class);
        plugin = mock(WorldBorder.class);
        when(plugin.getLogger()).thenReturn(mock(Logger.class));
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        bukkit.when(Bukkit::getOnlinePlayers).thenAnswer(ignored -> onlinePlayers);
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong()))
                .thenAnswer(invocation -> {
                    scheduledTick.set(invocation.getArgument(1));
                    return task;
                });
        world = mock(World.class);
        when(world.getName()).thenReturn("overworld");
        manager = new BorderManager(plugin);
        manager.start();
    }

    @AfterEach
    void tearDown() {
        if (manager != null) {
            manager.stop();
        }
        if (bukkit != null) {
            bukkit.close();
        }
        Cache.tickIntervalTicks = savedInterval;
        Cache.damage = savedDamage;
        Cache.graceInset = savedInset;
        Cache.titleWarning = savedTitle;
        Cache.subtitleWarning = savedSubtitle;
        Cache.worlds = savedWorlds;
        Cache.borders = savedBorders;
    }

    @Test
    void startUsesConfiguredIntervalAndRestartCancelsPreviousTask() {
        verify(scheduler).runTaskTimer(eq(plugin), any(Runnable.class), eq(7L), eq(7L));
        verify(plugin.getLogger()).info("Border tick started (interval=7 ticks).");
        assertNotNull(scheduledTick.get());

        BukkitTask previousTask = task;
        task = mock(BukkitTask.class);
        Cache.tickIntervalTicks = 25;
        manager.start();

        verify(previousTask).cancel();
        verify(scheduler).runTaskTimer(eq(plugin), any(Runnable.class), eq(25L), eq(25L));
        manager.stop();
        manager.stop();
        verify(task, times(1)).cancel();
        verify(previousTask, times(1)).cancel();
    }

    @ParameterizedTest
    @EnumSource(value = GameMode.class, names = {"SURVIVAL", "ADVENTURE"})
    void warningRepeatsUntilSafeAndOnlyResetsTitleOnce(GameMode mode) {
        Player player = playerAt(5, 50);
        when(player.getGameMode()).thenReturn(mode);

        tick();
        tick();
        verify(player, times(2)).sendTitle("Border warning", "Return to safety", 0, 15, 0);
        verify(player, never()).resetTitle();
        verify(player, never()).damage(anyDouble());

        move(player, 50, 50);
        tick();
        tick();
        verify(player, times(1)).resetTitle();
        verify(player, never()).damage(anyDouble());
    }

    @ParameterizedTest
    @EnumSource(value = GameMode.class, names = {"SURVIVAL", "ADVENTURE"})
    void outsideClearsWarningAndAppliesOnlyPositiveDamage(GameMode mode) {
        Player player = playerAt(5, 50);
        when(player.getGameMode()).thenReturn(mode);
        tick();

        move(player, 101, 50);
        tick();
        verify(player).resetTitle();
        verify(player).damage(3.5);

        Cache.damage = 0;
        tick();
        Cache.damage = -2;
        tick();
        verify(player, times(1)).damage(anyDouble());
        verify(player, times(1)).resetTitle();
    }

    @ParameterizedTest
    @EnumSource(value = GameMode.class, names = {"CREATIVE", "SPECTATOR"})
    void exemptModesClearAnExistingWarningAndPreventDamage(GameMode mode) {
        Player player = playerAt(5, 50);
        tick();
        when(player.getGameMode()).thenReturn(mode);
        move(player, -1, 50);

        tick();
        tick();
        verify(player, times(1)).resetTitle();
        verify(player, times(1)).sendTitle(anyString(), anyString(), anyInt(), anyInt(), anyInt());
        verify(player, never()).damage(anyDouble());
    }

    @Test
    void bypassPermissionClearsAnExistingWarningAndPreventsDamage() {
        Player player = playerAt(5, 50);
        tick();
        when(player.hasPermission("worldborder.bypass")).thenReturn(true);
        move(player, 101, 50);

        tick();
        tick();
        verify(player, times(1)).resetTitle();
        verify(player, times(1)).sendTitle(anyString(), anyString(), anyInt(), anyInt(), anyInt());
        verify(player, never()).damage(anyDouble());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void missingOrFilteredWorldClearsWarningAndPreventsDamage(boolean filterWorld) {
        Player player = playerAt(5, 50);
        tick();
        if (filterWorld) {
            Cache.worlds.add("another-world");
        } else {
            Cache.borders.clear();
        }
        move(player, 101, 50);

        tick();
        tick();
        verify(player, times(1)).resetTitle();
        verify(player, never()).damage(anyDouble());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void offlineWarningsArePrunedBeforeThePlayerReturns(boolean missingPlayer) {
        Player player = playerAt(5, 50);
        tick();
        onlinePlayers.clear();
        if (missingPlayer) {
            bukkit.when(() -> Bukkit.getPlayer(player.getUniqueId())).thenReturn(null);
        } else {
            when(player.isOnline()).thenReturn(false);
        }
        tick();

        onlinePlayers.add(player);
        when(player.isOnline()).thenReturn(true);
        bukkit.when(() -> Bukkit.getPlayer(player.getUniqueId())).thenReturn(player);
        move(player, 50, 50);
        tick();
        verify(player, never()).resetTitle();
        verify(player, times(1)).sendTitle(anyString(), anyString(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void stoppingClearsWarningsBeforeTheNextStart() {
        Player player = playerAt(5, 50);
        tick();
        manager.stop();
        manager.start();
        move(player, 50, 50);

        tick();
        verify(player, never()).resetTitle();
        verify(player, times(1)).sendTitle(anyString(), anyString(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void deadPlayersAreSkippedWithoutBorderChecksOrDamage() {
        Player dead = mock(Player.class);
        when(dead.isDead()).thenReturn(true);
        onlinePlayers.add(dead);
        Player alive = playerAt(101, 50);

        tick();
        verify(dead, never()).getGameMode();
        verify(dead, never()).getWorld();
        verify(dead, never()).getLocation();
        verify(dead, never()).sendTitle(anyString(), anyString(), anyInt(), anyInt(), anyInt());
        verify(dead, never()).resetTitle();
        verify(dead, never()).damage(anyDouble());
        verify(alive).damage(3.5);
    }

    private Player playerAt(double x, double z) {
        Player player = mock(Player.class);
        UUID id = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(id);
        when(player.isOnline()).thenReturn(true);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        when(player.getWorld()).thenReturn(world);
        move(player, x, z);
        bukkit.when(() -> Bukkit.getPlayer(id)).thenReturn(player);
        onlinePlayers.add(player);
        return player;
    }

    private void move(Player player, double x, double z) {
        when(player.getLocation()).thenReturn(new Location(world, x, 64, z));
    }

    private void tick() {
        scheduledTick.get().run();
    }
}
