package dev.crackle2k.imtelling;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class ImTellingPlugin extends JavaPlugin {

    private DiscordNotifier discordNotifier;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.discordNotifier = new DiscordNotifier(this);
        getServer().getPluginManager().registerEvents(new CommandSnitchListener(this), this);
        if (getServer().getPluginManager().getPlugin("GrimAC") != null) {
            new GrimFlagListener(this).hook();
        }
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

    /**
     * Sends an alert to Minecraft chat (MiniMessage format) and Discord,
     * honoring the minecraft.* and discord.* config toggles.
     * Safe to call from any thread.
     */
    public void alert(String minecraftRaw, String discordMessage) {
        FileConfiguration config = getConfig();

        if (config.getBoolean("minecraft.enabled", true)) {
            Component message = MiniMessage.miniMessage().deserialize(minecraftRaw);
            Runnable broadcast = () -> {
                if ("permission".equalsIgnoreCase(config.getString("minecraft.audience", "everyone"))) {
                    getServer().broadcast(message, "imtelling.notify");
                    getServer().getConsoleSender().sendMessage(message);
                } else {
                    getServer().broadcast(message);
                }
            };
            if (getServer().isPrimaryThread()) {
                broadcast.run();
            } else {
                // Grim fires flag events off the main thread.
                getServer().getScheduler().runTask(this, broadcast);
            }
        }

        if (config.getBoolean("discord.enabled", false)) {
            discordNotifier.send(discordMessage);
        }
    }
}
