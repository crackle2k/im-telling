package dev.crackle2k.imtelling;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class ImTellingPlugin extends JavaPlugin {

    private DiscordNotifier discordNotifier;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.discordNotifier = new DiscordNotifier(this);
        getServer().getPluginManager().registerEvents(new CommandSnitchListener(this), this);
        getLogger().info("ImTelling enabled. Watching " + getConfig().getStringList("watched-commands").size() + " commands.");
        if (getConfig().getBoolean("discord.enabled") && getConfig().getString("discord.bot-token", "").isBlank()) {
            getLogger().warning("Discord alerts are enabled but no bot-token is set in config.yml.");
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            sender.sendMessage("ImTelling config reloaded.");
            return true;
        }
        return false;
    }

    public DiscordNotifier discordNotifier() {
        return discordNotifier;
    }
}
