package io.github.sheynor43.clans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/**
 * Связывает кланы с ванильными командами scoreboard: префикс и цвет команды
 * игра сама показывает над головой, в табе и в чате.
 */
public final class ClanManager {
    private static final String TEAM_PREFIX = "s43clan_";

    private record Invite(String teamName, long expiresAt) {}

    private static final Map<UUID, Invite> INVITES = new HashMap<>();

    private ClanManager() {}

    public static Component prefix() {
        return Component.literal("[Кланы] ").withStyle(ChatFormatting.GOLD);
    }

    public static Component tagComponent(Clan clan) {
        return Component.literal("[" + clan.tag + "]").withStyle(clan.color);
    }

    // ---- scoreboard ----

    public static void applyTeam(MinecraftServer server, Clan clan) {
        Scoreboard scoreboard = server.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(clan.teamName);
        if (team == null) {
            team = scoreboard.addPlayerTeam(clan.teamName);
        }
        team.setDisplayName(Component.literal(clan.name));
        team.setPlayerPrefix(Component.literal("[" + clan.tag + "] ").withStyle(clan.color));
        team.setColor(ClanConfig.COLOR_NAMES.getAsBoolean() ? clan.color : ChatFormatting.RESET);
        team.setAllowFriendlyFire(ClanConfig.FRIENDLY_FIRE.getAsBoolean());

        for (String name : new ArrayList<>(team.getPlayers())) {
            if (!clan.members.containsValue(name)) {
                scoreboard.removePlayerFromTeam(name, team);
            }
        }
        for (String name : clan.members.values()) {
            if (scoreboard.getPlayersTeam(name) != team) {
                scoreboard.addPlayerToTeam(name, team);
            }
        }
    }

    public static void removeTeam(MinecraftServer server, Clan clan) {
        Scoreboard scoreboard = server.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(clan.teamName);
        if (team != null) {
            scoreboard.removePlayerTeam(team);
        }
    }

    public static void removeFromTeam(MinecraftServer server, Clan clan, String playerName) {
        Scoreboard scoreboard = server.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(clan.teamName);
        if (team != null && scoreboard.getPlayersTeam(playerName) == team) {
            scoreboard.removePlayerFromTeam(playerName, team);
        }
    }

    /** При старте сервера: создать недостающие команды и удалить команды распущенных кланов. */
    public static void syncAll(MinecraftServer server) {
        ClanData data = ClanData.get(server);
        Scoreboard scoreboard = server.getScoreboard();
        List<String> known = new ArrayList<>();
        for (Clan clan : data.all()) {
            applyTeam(server, clan);
            known.add(clan.teamName);
        }
        for (PlayerTeam team : new ArrayList<>(scoreboard.getPlayerTeams())) {
            if (team.getName().startsWith(TEAM_PREFIX) && !known.contains(team.getName())) {
                scoreboard.removePlayerTeam(team);
            }
        }
        Sector43Clans.LOGGER.info("Loaded {} clans", data.all().size());
    }

    /** Обновить ник участника, если он сменился, и проверить, что игрок в нужной команде. */
    public static void onLogin(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        ClanData data = ClanData.get(server);
        Clan clan = data.byMember(player.getUUID());
        String name = player.getScoreboardName();
        if (clan != null) {
            String oldName = clan.members.get(player.getUUID());
            if (!name.equals(oldName)) {
                removeFromTeam(server, clan, oldName);
                clan.members.put(player.getUUID(), name);
                data.setDirty();
            }
            applyTeam(server, clan);
            return;
        }
        PlayerTeam team = server.getScoreboard().getPlayersTeam(name);
        if (team != null && team.getName().startsWith(TEAM_PREFIX)) {
            server.getScoreboard().removePlayerFromTeam(name, team);
        }
    }

    /** Сообщение всем участникам клана, кто сейчас онлайн. */
    public static void notifyMembers(MinecraftServer server, Clan clan, Component message) {
        for (UUID member : clan.members.keySet()) {
            ServerPlayer online = server.getPlayerList().getPlayer(member);
            if (online != null) {
                online.sendSystemMessage(Component.empty().append(prefix()).append(message));
            }
        }
    }

    // ---- приглашения ----

    public static void invite(UUID target, Clan clan) {
        long timeout = ClanConfig.INVITE_TIMEOUT_SECONDS.getAsInt() * 1000L;
        INVITES.put(target, new Invite(clan.teamName, System.currentTimeMillis() + timeout));
    }

    /** Забирает действующее приглашение игрока; null, если его нет или оно истекло. */
    public static Clan takeInvite(MinecraftServer server, UUID target) {
        Invite invite = INVITES.remove(target);
        if (invite == null || invite.expiresAt() < System.currentTimeMillis()) {
            return null;
        }
        for (Clan clan : ClanData.get(server).all()) {
            if (clan.teamName.equals(invite.teamName())) {
                return clan;
            }
        }
        return null;
    }

    public static void clearInvites() {
        INVITES.clear();
    }
}
