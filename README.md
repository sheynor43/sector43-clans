# Sector 43 Clans

**English** | [Русский](README.ru.md)

A lightweight server-side clan mod for **NeoForge 26.1.2**, made for the Sector 43 Minecraft server.

Players can create a clan, pick a tag and a color, and invite friends. The clan tag appears in front of the player's name, and the name takes on the clan color. You see it above heads, in the tab list, and in chat. The mod is built on vanilla scoreboard teams, so **players don't need to install anything**.

```
[SEC] Alice    ← tag in the clan color, name colored too
```

## Features

- Create a clan with a single command. A default tag is generated from the clan name.
- Custom tag (letters and digits, Cyrillic supported) and any of the 16 vanilla colors.
- Invitations that expire, plus leave, kick, leadership transfer and disband (with confirmation).
- `/clan info` and `/clan list` to look up clans.
- Who can create clans is controlled by permissions, so it works with **LuckPerms**. For example, only VIP and above can create clans.
- Admin command to disband any clan.
- Clans are saved with the world and survive restarts.
- A server config for name and tag length, member limit, name coloring, friendly fire and invite timeout.
- `/resize`: players with permission can change their height (vanilla `minecraft:scale` attribute), like the ResizePlayers plugin.

## Requirements

| | |
|---|---|
| Minecraft | 26.1.2 |
| NeoForge | 26.1.2.126 or newer |
| Java | 25 |
| Side | server only (clients may have it, but don't need it) |
| Optional | [LuckPerms](https://luckperms.net/) to manage permissions |

## Installation

1. Download `sector43clans-<version>.jar` from [Releases](https://github.com/sheynor43/sector43-clans/releases).
2. Put it in your server's `mods/` folder.
3. Start the server. The config `world/serverconfig/sector43clans-server.toml` is created on first launch.
4. Grant the permission to create clans (see [Permissions](#permissions)).

## Commands

| Command | Who | Description |
|---|---|---|
| `/clan` or `/clan help` | everyone | Show the list of commands |
| `/clan create <name>` | `sector43clans.create` | Create a clan. You become its leader |
| `/clan tag <tag>` | leader | Change the clan tag |
| `/clan color <color>` | leader | Change the clan color (`red`, `gold`, `aqua`, `dark_purple`…) |
| `/clan invite <player>` | leader with `sector43clans.invite` | Invite an online player |
| `/clan accept` | invited player | Accept the latest invitation |
| `/clan decline` | invited player | Decline the invitation |
| `/clan leave` | member | Leave the clan. A leader who is the only member disbands the clan this way |
| `/clan kick <player>` | leader | Remove a member |
| `/clan transfer <player>` | leader | Make another member the leader |
| `/clan disband` | leader | Asks for confirmation. Run `/clan disband confirm` to disband |
| `/clan info [name]` | everyone | Info about your clan or the named one |
| `/clan list` | everyone | All clans on the server |
| `/clan admin disband <name>` | `sector43clans.admin` | Disband any clan |
| `/resize` | `sector43clans.resize` | Show your height |
| `/resize <height>` | `sector43clans.resize` | Set your height in blocks (default 1.8, limits from config) |
| `/resize reset` | `sector43clans.resize` | Back to normal height |
| `/resize <height> <player>` | `sector43clans.resize.others` | Set another player's height |

A player can be in only one clan at a time. A leader can't leave while other members remain: transfer leadership or disband the clan first.

## Permissions

The mod registers its permissions through the NeoForge Permission API, so LuckPerms (or any other permission mod) manages them. If a permission is not set, only server operators have it.

| Permission | Default | Allows |
|---|---|---|
| `sector43clans.create` | operators | Creating clans |
| `sector43clans.invite` | operators | Inviting players to your clan |
| `sector43clans.admin` | operators | `/clan admin` commands |
| `sector43clans.resize` | operators | `/resize` for yourself |
| `sector43clans.resize.others` | operators | `/resize` for other players |

Example with LuckPerms. Only VIP and higher groups can create clans:

```
/lp group vip permission set sector43clans.create true
/lp group vip permission set sector43clans.invite true
/lp group vip permission set sector43clans.resize true
/lp group admin permission set sector43clans.admin true
```

Groups that inherit from `vip` get the permission automatically.

## Configuration

`world/serverconfig/sector43clans-server.toml`:

| Option | Default | Description |
|---|---|---|
| `nameMinLength` / `nameMaxLength` | 3 / 16 | Clan name length |
| `tagMinLength` / `tagMaxLength` | 2 / 5 | Tag length |
| `maxMembers` | 0 | Member limit, including the leader. `0` means no limit |
| `colorNames` | true | Color player names in the clan color. If `false`, only the tag is colored |
| `friendlyFire` | true | Whether clan members can damage each other |
| `inviteTimeoutSeconds` | 300 | How long an invitation stays valid |
| `resizeMinHeight` / `resizeMaxHeight` | 1.3 / 2.5 | `/resize` height limits in blocks |

## How it works

- Clan data is stored in `world/data/sector43clans/clans.dat`.
- Each clan is mirrored to a vanilla scoreboard team named `s43clan_N`. The team prefix shows the tag and the team color colors the name. Teams are re-synced on every server start.
- Don't edit `s43clan_*` teams with `/team`: the mod overwrites them on the next sync.
- Chat formatting mods (for example, FlectonePulse) may replace the vanilla chat format. If the tag doesn't show up in chat, add the player's team prefix to that mod's message format.

## Building from source

```bash
git clone https://github.com/sheynor43/sector43-clans.git
cd sector43-clans
./gradlew build
```

The jar ends up in `build/libs/`. Gradle downloads Java 25 automatically through toolchains.

To start a local test server with the mod: `./gradlew runServer`. You'll have to accept the Minecraft EULA in `run/eula.txt` first.

## License

[MIT](LICENSE) © sheynor43
