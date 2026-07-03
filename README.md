# ImTelling

A Paper plugin that snitches on admins who use operator commands (`/give`, `/gamemode`, `/op`, `/tp`, and more). Alerts are broadcast in Minecraft chat and, optionally, to a Discord channel.

Built for **Paper 26.2** (requires Java 25 to build and run).

## Build

```
mvn package
```

The jar lands in `target/ImTelling-1.0.0.jar`. Drop it into your server's `plugins/` folder and restart. On first start it generates `plugins/ImTelling/config.yml`.

## Configuration

Everything lives in `config.yml`:

- **`watched-commands`** — the command names to snitch on (no leading slash). Namespaced forms like `/minecraft:give` are caught too.
- **`watch-console`** — also report commands run from the server console.
- **`exempt-players`** / **`exempt-permission-enabled`** — ways to exclude trusted users. The permission-based exemption is off by default so admins can't quietly exempt themselves.
- **`minecraft`** — in-game alerts: toggle, audience (`everyone` or `permission` for `imtelling.notify` holders only), and a MiniMessage format string.
- **`discord`** — Discord alerts: toggle, bot token, channel ID, and message format.

### Discord setup

1. Create an application + bot at <https://discord.com/developers/applications> and copy the bot token.
2. Invite the bot to your Discord server with the **View Channel** and **Send Messages** permissions.
3. Enable Developer Mode in Discord, right-click the alert channel, and **Copy Channel ID**.
4. In `config.yml`, set `discord.enabled: true`, paste the `bot-token` and `channel-id`, then run `/imtelling reload`.

Keep the bot token secret — anyone who has it controls your bot.

## Commands & permissions

| Command | Permission | Description |
|---|---|---|
| `/imtelling reload` | `imtelling.admin` (op) | Reload the config |

| Permission | Default | Description |
|---|---|---|
| `imtelling.notify` | op | Receive alerts when audience is `permission` |
| `imtelling.exempt` | false | Never reported (only if enabled in config) |
