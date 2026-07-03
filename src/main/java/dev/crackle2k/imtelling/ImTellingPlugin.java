package dev.crackle2k.imtelling;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class ImTellingPlugin extends JavaPlugin {

    private record PendingAlert(String minecraftRaw, String discordMessage) {}

    private DiscordNotifier discordNotifier;

    private final Object alertLock = new Object();
    private long lastAlertMillis;
    private boolean flushScheduled;
    // Distinct alerts that arrived during the cooldown; duplicates are dropped.
    private final java.util.LinkedHashSet<PendingAlert> pendingAlerts = new java.util.LinkedHashSet<>();
    // Alerts already sent in the current cooldown window, so a repeat of the
    // message that started the window is dropped too.
    private final java.util.HashSet<PendingAlert> sentThisWindow = new java.util.HashSet<>();
    // Cheat detections (player|check) already reported this session.
    private final java.util.Set<String> reportedDetections = java.util.concurrent.ConcurrentHashMap.newKeySet();

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
        // Cooldown between alerts so repeated triggers (e.g. Grim flags)
        // don't spam chat. Alerts during the cooldown are held and sent
        // together once it ends; duplicate messages are dropped.
        long cooldownMillis = getConfig().getLong("alert-cooldown-seconds", 10) * 1000L;
        PendingAlert alert = new PendingAlert(minecraftRaw, discordMessage);
        synchronized (alertLock) {
            long now = System.currentTimeMillis();
            if (cooldownMillis > 0 && (now - lastAlertMillis < cooldownMillis || !pendingAlerts.isEmpty())) {
                if (!sentThisWindow.contains(alert)) {
                    pendingAlerts.add(alert);
                    if (!flushScheduled) {
                        flushScheduled = true;
                        long delayTicks = Math.max(1, (lastAlertMillis + cooldownMillis - now) / 50);
                        getServer().getScheduler().runTaskLater(this, this::flushPendingAlerts, delayTicks);
                    }
                }
                return;
            }
            lastAlertMillis = now;
            sentThisWindow.clear();
            sentThisWindow.add(alert);
        }
        dispatch(minecraftRaw, discordMessage);
    }

    /** Sends the distinct alerts held back during the cooldown. */
    private void flushPendingAlerts() {
        java.util.LinkedHashSet<PendingAlert> toSend;
        synchronized (alertLock) {
            flushScheduled = false;
            lastAlertMillis = System.currentTimeMillis();
            toSend = new java.util.LinkedHashSet<>(pendingAlerts);
            pendingAlerts.clear();
            // The flush opens a new cooldown window; repeats of what was just
            // sent stay suppressed for that window.
            sentThisWindow.clear();
            sentThisWindow.addAll(toSend);
        }
        toSend.forEach(pending -> dispatch(pending.minecraftRaw(), pending.discordMessage()));
    }

    /**
     * Records a cheat detection key ("player|check"). Returns true the first
     * time a key is seen, false on repeats, so each detection is only
     * reported once. Cleared for a player when they disconnect.
     */
    public boolean markDetection(String key) {
        return reportedDetections.add(key);
    }

    /** Forgets a player's reported detections so a rejoin alerts again. */
    public void clearDetections(String playerName) {
        reportedDetections.removeIf(key -> key.startsWith(playerName + "|"));
    }

    private void dispatch(String minecraftRaw, String discordMessage) {
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
