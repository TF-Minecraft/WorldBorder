package net.tfminecraft.worldborder.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;

import net.tfminecraft.worldborder.WorldBorder;
import net.tfminecraft.worldborder.border.Region;
import net.tfminecraft.worldborder.cache.Cache;

class CommandManagerTest {
    private int originalInterval;
    private double originalDamage;
    private int originalInset;
    private String originalTitle;
    private String originalSubtitle;
    private List<String> originalWorlds;
    private Map<String, Region> originalBorders;
    private WorldBorder originalPlugin;
    private Locale originalLocale;
    private CommandManager manager;
    private Command command;
    private WorldBorder plugin;

    @BeforeEach
    void setUp() {
        originalInterval = Cache.tickIntervalTicks;
        originalDamage = Cache.damage;
        originalInset = Cache.graceInset;
        originalTitle = Cache.titleWarning;
        originalSubtitle = Cache.subtitleWarning;
        originalWorlds = Cache.worlds;
        originalBorders = Cache.borders;
        originalPlugin = WorldBorder.plugin;
        originalLocale = Locale.getDefault();
        Cache.tickIntervalTicks = 10;
        Cache.damage = 8.0;
        Cache.graceInset = 20;
        Cache.titleWarning = "§cTurn back";
        Cache.subtitleWarning = "§cYou feel continuing further would be very dangerous...";
        Cache.worlds = new ArrayList<>();
        Cache.borders = new LinkedHashMap<>();
        plugin = mock(WorldBorder.class);
        WorldBorder.plugin = plugin;
        manager = new CommandManager();
        command = mock(Command.class);
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
        WorldBorder.plugin = originalPlugin;
        Locale.setDefault(originalLocale);
    }

    @ParameterizedTest
    @ValueSource(strings = {"reload", "debug", "unknown"})
    void permissionIsRequiredBeforeDispatch(String subcommand) {
        CommandSender sender = mock(CommandSender.class);

        assertTrue(execute(sender, subcommand));

        verify(sender).hasPermission("worldborder.admin");
        verify(sender).sendMessage("§cNo permission.");
        verifyNoMoreInteractions(sender);
        verifyNoInteractions(plugin);
    }

    @Test
    void noArgumentsListsBothCommands() {
        CommandSender sender = admin();

        assertEquals("worldborder", manager.cmd);
        assertTrue(execute(sender));

        InOrder messages = inOrder(sender);
        messages.verify(sender).sendMessage("§e/worldborder reload");
        messages.verify(sender).sendMessage("§e/worldborder debug");
        verifyNoInteractions(plugin);
    }

    @ParameterizedTest
    @ValueSource(strings = {"reload", "ReLoAd"})
    void reloadReportsTheNewBorderCount(String subcommand) {
        CommandSender sender = admin();
        Cache.borders.put("old", new Region("old", 0, 1, 0, 1));
        doAnswer(invocation -> {
            Cache.borders.clear();
            Cache.borders.put("world", new Region("world", -100, 100, -100, 100));
            Cache.borders.put("nether", new Region("nether", -50, 50, -50, 50));
            return null;
        }).when(plugin).reloadAll();

        assertTrue(execute(sender, subcommand));

        InOrder reload = inOrder(plugin, sender);
        reload.verify(plugin).reloadAll();
        reload.verify(sender).sendMessage("§aWorldBorder reloaded. Loaded 2 world border(s).");
    }

    @Test
    void reloadReportsAnEmptyConfiguration() {
        CommandSender sender = admin();

        assertTrue(execute(sender, "reload"));

        verify(plugin).reloadAll();
        verify(sender).sendMessage("§aWorldBorder reloaded. Loaded 0 world border(s).");
    }

    @Test
    void debugRejectsConsoleSenders() {
        CommandSender sender = admin();

        assertTrue(execute(sender, "DeBuG"));

        verify(sender).sendMessage("§cPlayers only.");
        verifyNoInteractions(plugin);
    }

    @Test
    void unknownSubcommandExplainsValidChoices() {
        CommandSender sender = admin();

        assertTrue(execute(sender, "missing"));

        verify(sender).sendMessage("§cUnknown subcommand. Use §e/worldborder reload §cor §e/worldborder debug");
        verifyNoInteractions(plugin);
    }

    @Test
    void debugReportsWorldAndCoordinatesWithoutABorder() {
        Player player = player("unconfigured", -12, 34.26);
        Locale.setDefault(Locale.GERMANY);

        assertTrue(execute(player, "debug"));

        InOrder messages = inOrder(player);
        messages.verify(player).sendMessage("§7World: §funconfigured");
        messages.verify(player).sendMessage("§7Position: §f-12, 34.3");
        messages.verify(player).sendMessage("§7Zone: §f(none)");
        messages.verify(player).sendMessage("§7Border: §fno border for this world");
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0, '§aSAFE', '0, 0'",
            "80, 0, '§cWARNING', '80, 0'",
            "100.5, -0.25, '§cOUTSIDE', '100.5, -0.3'"
    })
    void debugReportsTheCurrentZoneAndBorder(double x, double z, String zone, String position) {
        Cache.borders.put("world", new Region("world", -100, 100, -100, 100));
        Player player = player("world", x, z);

        assertTrue(execute(player, "DeBuG"));

        InOrder messages = inOrder(player);
        messages.verify(player).sendMessage("§7World: §fworld");
        messages.verify(player).sendMessage("§7Position: §f" + position);
        messages.verify(player).sendMessage("§7Zone: " + zone);
        messages.verify(player).sendMessage("§7Border: §fx -100-100, z -100-100");
    }

    @Test
    void debugHonorsTheAllowedWorldList() {
        Cache.worlds.add("another-world");
        Cache.borders.put("world", new Region("world", -100, 100, -100, 100));
        Player player = player("world", 0, 0);

        assertTrue(execute(player, "debug"));

        verify(player).sendMessage("§7Zone: §f(none)");
        verify(player).sendMessage("§7Border: §fno border for this world");
    }

    @Test
    void completionRequiresPermissionAndExactlyOneArgument() {
        CommandSender denied = mock(CommandSender.class);
        CommandSender allowed = admin();

        assertEquals(List.of(), complete(denied, ""));
        assertEquals(List.of(), complete(allowed));
        assertEquals(List.of(), complete(allowed, "reload", ""));
        verifyNoInteractions(plugin);
    }

    @ParameterizedTest
    @MethodSource("prefixMatches")
    void completionFiltersCaseInsensitively(String prefix, List<String> matches) {
        assertEquals(matches, complete(admin(), prefix));
    }

    private static Stream<Arguments> prefixMatches() {
        return Stream.of(
                Arguments.of("", List.of("reload", "debug")),
                Arguments.of("r", List.of("reload")),
                Arguments.of("RE", List.of("reload")),
                Arguments.of("reload", List.of("reload")),
                Arguments.of("d", List.of("debug")),
                Arguments.of("DeBuG", List.of("debug")),
                Arguments.of("unknown", List.of()),
                Arguments.of("reload-more", List.of()));
    }

    private CommandSender admin() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("worldborder.admin")).thenReturn(true);
        return sender;
    }

    private Player player(String worldName, double x, double z) {
        Player player = mock(Player.class);
        World world = mock(World.class);
        when(player.hasPermission("worldborder.admin")).thenReturn(true);
        when(world.getName()).thenReturn(worldName);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(new Location(world, x, 64, z));
        return player;
    }

    private boolean execute(CommandSender sender, String... args) {
        return manager.onCommand(sender, command, "worldborder", args);
    }

    private List<String> complete(CommandSender sender, String... args) {
        return manager.onTabComplete(sender, command, "worldborder", args);
    }
}
