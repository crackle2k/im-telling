package dev.crackle2k.imtelling;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Posts messages to a Discord channel through the bot REST API.
 * No Discord library needed: a bot token plus a channel ID is enough
 * to call POST /channels/{id}/messages.
 */
public final class DiscordNotifier {

    private static final String API_URL = "https://discord.com/api/v10/channels/%s/messages";

    private final ImTellingPlugin plugin;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public DiscordNotifier(ImTellingPlugin plugin) {
        this.plugin = plugin;
    }

    /** Sends asynchronously; never blocks the server thread. */
    public void send(String message) {
        String token = plugin.getConfig().getString("discord.bot-token", "");
        String channelId = plugin.getConfig().getString("discord.channel-id", "");
        if (token.isBlank() || channelId.isBlank()) {
            return;
        }

        String body = "{\"content\":\"" + escapeJson(message) + "\"}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL.formatted(channelId)))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bot " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, error) -> {
                    if (error != null) {
                        plugin.getLogger().warning("Failed to send Discord alert: " + error.getMessage());
                    } else if (response.statusCode() / 100 != 2) {
                        plugin.getLogger().warning("Discord API returned " + response.statusCode()
                                + " (check bot-token, channel-id, and the bot's channel permissions): "
                                + response.body());
                    }
                });
    }

    private static String escapeJson(String text) {
        StringBuilder out = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
