# I'm Telling

Paper plugin that snitches on staff. It watches for operator commands like `/give`, `/gamemode`, `/op`, and `/tp`, and relays GrimAC anticheat flags. Alerts are broadcast in Minecraft chat and, optionally, to a Discord channel.

Built for **Paper 26.2**. Requires **Java 25** to build and run.

## Features

- **Command snitching**: reports when anyone runs a watched command, including from the server console and RCON. Namespaced forms like `/minecraft:give` are caught too.
- **GrimAC relay**: forwards Grim anticheat flags to chat and Discord. Activates automatically when GrimAC is installed; no extra setup. Each player and check combination is reported once per session so a flapping check does not flood the channel.
- **Discord alerts**: posts through the Discord bot REST API directly. No webhook and no Discord library required, just a bot token and a channel ID.
- **Spam control**: a configurable cooldown batches alerts together and drops duplicates.
- **Hard to dodge**: the permission-based exemption is off by default, so an admin cannot quietly exempt themselves.

## Alert format

Alerts are prefixed with their source so you can tell them apart at a glance:

```
SNITCH >> Steve used /gamemode creative
GRIM >> Alex failed Simulation (VL 12.0)
```

Both formats are fully customizable in `config.yml` using MiniMessage for chat and Markdown for Discord.

## Installation

1. Run `mvn package` (or grab a release jar).
2. Copy `target/ImTelling-1.0.0.jar` into your server's `plugins/` folder.
3. Restart the server. The default `plugins/ImTelling/config.yml` is generated on first start.

## Configuration

Everything lives in `config.yml`:

| Key | Description |
|---|---|
| `watched-commands` | Command names to snitch on, without the leading slash. Aliases must be listed explicitly (both `tp` and `teleport`). |
| `watch-console` | Also report commands run from the console or RCON. |
| `alert-cooldown-seconds` | Minimum seconds between alert bursts. Alerts arriving during the cooldown are held and sent together; duplicates are dropped. `0` disables. |
| `exempt-players` | Player names that are never reported. |
| `exempt-permission-enabled` | If `true`, players with `imtelling.exempt` are never reported. Off by default. |
| `minecraft.enabled` | Broadcast alerts in game. |
| `minecraft.audience` | `everyone`, or `permission` to only notify holders of `imtelling.notify`. |
| `minecraft.format` | Chat message template (MiniMessage). Placeholders: `{player}`, `{command}`. |
| `discord.enabled` | Send alerts to Discord. |
| `discord.bot-token`, `discord.channel-id` | Bot credentials, see setup below. |
| `discord.format` | Discord message template. Placeholders: `{player}`, `{command}`. |
| `grim.enabled` | Relay GrimAC flags (only when GrimAC is installed). |
| `grim.minecraft-format`, `grim.discord-format` | Grim alert templates. Placeholders: `{player}`, `{check}`, `{violations}`, `{verbose}`. |

Apply changes with `/imtelling reload`.

### Discord setup

1. Create an application and bot at <https://discord.com/developers/applications> and copy the bot token.
2. Invite the bot to your Discord server with the **View Channel** and **Send Messages** permissions.
3. Enable Developer Mode in Discord, right-click the alert channel, and choose **Copy Channel ID**.
4. In `config.yml`, set `discord.enabled: true`, paste the `bot-token` and `channel-id`, then run `/imtelling reload`.

Keep the bot token secret. Anyone who has it controls your bot.

## Commands and permissions

| Command | Permission | Description |
|---|---|---|
| `/imtelling reload` | `imtelling.admin` (op) | Reload the config |

| Permission | Default | Description |
|---|---|---|
| `imtelling.notify` | op | Receive alerts when `minecraft.audience` is `permission` |
| `imtelling.exempt` | false | Never reported (only when `exempt-permission-enabled` is `true`) |

## License

See [LICENSE](LICENSE).
