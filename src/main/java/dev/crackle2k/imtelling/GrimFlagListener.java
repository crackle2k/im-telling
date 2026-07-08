package dev.crackle2k.imtelling;

import ac.grim.grimac.api.GrimAPIProvider;
import ac.grim.grimac.api.event.events.FlagEvent;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Locale;

public final class GrimFlagListener {

    private final ImTellingPlugin plugin;

    public GrimFlagListener(ImTellingPlugin plugin) {
        this.plugin = plugin;
    }

    public void hook() {
        GrimAPIProvider.getAsync().thenAccept(api -> {
            api.getEventBus().subscribe(plugin, FlagEvent.class, this::onFlag);
            plugin.getLogger().info("Hooked into GrimAC " + api.getGrimVersion()
                    + ": relaying flags to chat and Discord.");
        }).exceptionally(error -> {
            plugin.getLogger().warning("Failed to hook into GrimAC: " + error.getMessage());
            return null;
        });
    }

    private void onFlag(FlagEvent event) {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("grim.enabled", true)) {
            return;
        }

        String player = event.getUser().getName();
        String check = event.getCheck().getCheckName();

        if (!plugin.markDetection(player + "|" + check)) {
            return;
        }
        String violations = String.format(Locale.ROOT, "%.1f", event.getViolations());
        String verbose = event.getVerbose() == null ? "" : event.getVerbose();

        String minecraftMessage = config.getString("grim.minecraft-format",
                        "<dark_red><bold>GRIM</bold></dark_red> <dark_gray>>></dark_gray> <yellow>{player}</yellow> <gray>failed</gray> <white>{check}</white> <gray>(VL {violations})</gray>")
                .replace("{player}", player)
                .replace("{check}", check)
                .replace("{violations}", violations)
                .replace("{verbose}", verbose);

        String discordMessage = config.getString("grim.discord-format",
                        ":no_entry: **GRIM** >> **{player}** failed **{check}** (VL {violations}) `{verbose}`")
                .replace("{player}", player)
                .replace("{check}", check)
                .replace("{violations}", violations)
                .replace("{verbose}", verbose);

        plugin.alert(minecraftMessage, discordMessage);
    }
}
