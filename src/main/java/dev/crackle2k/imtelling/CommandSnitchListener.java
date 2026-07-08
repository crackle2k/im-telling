package dev.crackle2k.imtelling;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.RemoteServerCommandEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.Locale;

public final class CommandSnitchListener implements Listener {

    private final ImTellingPlugin plugin;

    public CommandSnitchListener(ImTellingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (isExempt(player)) {
            return;
        }
        String command = event.getMessage();
        if (isWatched(command)) {
            snitch(player.getName(), command);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsoleCommand(ServerCommandEvent event) {
        if (!plugin.getConfig().getBoolean("watch-console", true)) {
            return;
        }
        String command = "/" + event.getCommand();
        if (isWatched(command)) {
            snitch(event.getSender().getName(), command);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.clearDetections(event.getPlayer().getName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRemoteCommand(RemoteServerCommandEvent event) {
        onConsoleCommand(event);
    }

    private boolean isExempt(Player player) {
        FileConfiguration config = plugin.getConfig();
        if (config.getBoolean("exempt-permission-enabled", false) && player.hasPermission("imtelling.exempt")) {
            return true;
        }
        return config.getStringList("exempt-players").stream()
                .anyMatch(name -> name.equalsIgnoreCase(player.getName()));
    }

    private boolean isWatched(String commandLine) {
        String root = commandLine.substring(1).stripLeading();
        int space = root.indexOf(' ');
        if (space != -1) {
            root = root.substring(0, space);
        }
        int colon = root.indexOf(':');
        if (colon != -1) {
            root = root.substring(colon + 1);
        }
        return plugin.getConfig().getStringList("watched-commands")
                .contains(root.toLowerCase(Locale.ROOT));
    }

    private void snitch(String playerName, String command) {
        FileConfiguration config = plugin.getConfig();

        String minecraftMessage = config.getString("minecraft.format",
                        "<red><bold>SNITCH</bold></red> <dark_gray>>></dark_gray> <yellow>{player}</yellow> <gray>used</gray> <white>{command}</white>")
                .replace("{player}", playerName)
                .replace("{command}", command);
        String discordMessage = config.getString("discord.format", ":rotating_light: **SNITCH** >> **{player}** used `{command}`")
                .replace("{player}", playerName)
                .replace("{command}", command);

        plugin.alert(minecraftMessage, discordMessage);
    }
}
