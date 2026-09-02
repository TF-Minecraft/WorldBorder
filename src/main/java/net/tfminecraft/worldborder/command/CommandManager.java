package net.tfminecraft.worldborder.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import net.tfminecraft.worldborder.WorldBorder;
import net.tfminecraft.worldborder.border.Region;
import net.tfminecraft.worldborder.cache.Cache;

public final class CommandManager implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("reload", "debug");

    public String cmd = "worldborder";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("worldborder.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("§e/worldborder reload");
            sender.sendMessage("§e/worldborder debug");
            return true;
        }
        if ("reload".equalsIgnoreCase(args[0])) {
            WorldBorder.plugin.reloadAll();
            sender.sendMessage("§aWorldBorder reloaded. Loaded " + Cache.borders.size() + " world border(s).");
            return true;
        }
        if ("debug".equalsIgnoreCase(args[0])) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cPlayers only.");
                return true;
            }
            sendDebug(player);
            return true;
        }
        sender.sendMessage("§cUnknown subcommand. Use §e/worldborder reload §cor §e/worldborder debug");
        return true;
    }

    private void sendDebug(Player player) {
        String worldName = player.getWorld().getName();
        double x = player.getLocation().getX();
        double z = player.getLocation().getZ();

        player.sendMessage("§7World: §f" + worldName);
        player.sendMessage("§7Position: §f" + formatCoord(x) + ", " + formatCoord(z));

        Region region = Cache.borderForWorld(worldName);
        if (region == null) {
            player.sendMessage("§7Zone: §f(none)");
            player.sendMessage("§7Border: §fno border for this world");
            return;
        }

        Region.Zone zone = region.zoneAt(x, z, Cache.graceInset);
        player.sendMessage("§7Zone: " + formatZone(zone));
        player.sendMessage("§7Border: §fx " + region.minX + "-" + region.maxX
                + ", z " + region.minZ + "-" + region.maxZ);
    }

    private static String formatCoord(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String formatZone(Region.Zone zone) {
        return switch (zone) {
            case SAFE -> "§aSAFE";
            case WARNING -> "§cWARNING";
            case OUTSIDE -> "§cOUTSIDE";
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("worldborder.admin") || args.length != 1) {
            return Collections.emptyList();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String subcommand : SUBCOMMANDS) {
            if (subcommand.startsWith(prefix)) {
                matches.add(subcommand);
            }
        }
        return matches;
    }
}
