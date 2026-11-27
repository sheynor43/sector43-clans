package io.github.sheynor43.clans;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ColorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ClanCommands {
    private static final Pattern NAME_PATTERN = Pattern.compile("[\\p{L}\\p{N}_ ]+");
    private static final Pattern TAG_PATTERN = Pattern.compile("[\\p{L}\\p{N}]+");

    private ClanCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("clan")
                .executes(ClanCommands::help)
                .then(Commands.literal("help").executes(ClanCommands::help))
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.greedyString()).executes(ClanCommands::create)))
                .then(Commands.literal("tag")
                        .then(Commands.argument("tag", StringArgumentType.greedyString()).executes(ClanCommands::setTag)))
                .then(Commands.literal("color")
                        .then(Commands.argument("color", ColorArgument.color()).executes(ClanCommands::setColor)))
                .then(Commands.literal("invite")
                        .then(Commands.argument("player", EntityArgument.player()).executes(ClanCommands::invite)))
                .then(Commands.literal("accept").executes(ClanCommands::accept))
                .then(Commands.literal("decline").executes(ClanCommands::decline))
                .then(Commands.literal("leave").executes(ClanCommands::leave))
                .then(Commands.literal("kick")
                        .then(Commands.argument("player", StringArgumentType.word()).suggests(OWN_MEMBERS).executes(ClanCommands::kick)))
                .then(Commands.literal("transfer")
                        .then(Commands.argument("player", StringArgumentType.word()).suggests(OWN_MEMBERS).executes(ClanCommands::transfer)))
                .then(Commands.literal("disband")
                        .executes(ctx -> disband(ctx, false))
                        .then(Commands.literal("confirm").executes(ctx -> disband(ctx, true))))
                .then(Commands.literal("info")
                        .executes(ctx -> info(ctx, null))
                        .then(Commands.argument("name", StringArgumentType.greedyString()).suggests(CLAN_NAMES)
                                .executes(ctx -> info(ctx, StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("list").executes(ClanCommands::list))
                .then(Commands.literal("admin")
                        .requires(ClanCommands::isAdmin)
                        .then(Commands.literal("disband")
                                .then(Commands.argument("name", StringArgumentType.greedyString()).suggests(CLAN_NAMES)
                                        .executes(ClanCommands::adminDisband)))));
    }

    private static final SuggestionProvider<CommandSourceStack> CLAN_NAMES = (ctx, builder) ->
            SharedSuggestionProvider.suggest(ClanData.get(ctx.getSource().getServer()).all().stream().map(c -> c.name), builder);

    private static final SuggestionProvider<CommandSourceStack> OWN_MEMBERS = (ctx, builder) -> {
        ServerPlayer player = ctx.getSource().getPlayer();
        Clan clan = player == null ? null : ClanData.get(ctx.getSource().getServer()).byMember(player.getUUID());
        return SharedSuggestionProvider.suggest(clan == null ? List.of() : clan.members.values(), builder);
    };

    private static boolean isAdmin(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        return player == null || ClanPermissions.has(player, ClanPermissions.ADMIN);
    }

    // ---- команды ----

    private static int help(CommandContext<CommandSourceStack> ctx) {
        String text = String.join("\n",
                "/clan create <название> — создать клан",
                "/clan tag <тег> — сменить тег (лидер)",
                "/clan color <цвет> — сменить цвет (лидер)",
                "/clan invite <игрок> — пригласить (лидер)",
                "/clan accept | decline — принять или отклонить приглашение",
                "/clan leave — выйти из клана",
                "/clan kick <игрок> — выгнать (лидер)",
                "/clan transfer <игрок> — передать лидерство",
                "/clan disband — распустить клан (лидер)",
                "/clan info [название] — о клане",
                "/clan list — все кланы");
        ctx.getSource().sendSuccess(() -> Component.empty().append(ClanManager.prefix()).append(Component.literal("Команды:\n" + text)), false);
        return 1;
    }

    private static int create(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        MinecraftServer server = ctx.getSource().getServer();
        ClanData data = ClanData.get(server);

        if (!ClanPermissions.has(player, ClanPermissions.CREATE)) {
            return fail(ctx, "Создавать кланы могут только VIP и выше.");
        }
        if (data.byMember(player.getUUID()) != null) {
            return fail(ctx, "Ты уже состоишь в клане. Сначала выйди из него: /clan leave");
        }
        String name = StringArgumentType.getString(ctx, "name").trim().replaceAll("\\s+", " ");
        String error = validateName(name);
        if (error != null) {
            return fail(ctx, error);
        }
        if (data.byName(name) != null) {
            return fail(ctx, "Клан с таким названием уже есть.");
        }

        Map<UUID, String> members = new LinkedHashMap<>();
        members.put(player.getUUID(), player.getScoreboardName());
        Clan clan = new Clan(data.nextTeamName(), name, defaultTag(name), ChatFormatting.WHITE, player.getUUID(), members);
        data.add(clan);
        ClanManager.applyTeam(server, clan);

        return ok(ctx, Component.literal("Клан «" + name + "» создан с тегом ").append(ClanManager.tagComponent(clan))
                .append(". Сменить тег: /clan tag <тег>, цвет: /clan color <цвет>, позвать игроков: /clan invite <игрок>"));
    }

    private static int setTag(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Clan clan = ledClan(ctx);
        if (clan == null) {
            return 0;
        }
        String tag = StringArgumentType.getString(ctx, "tag").trim();
        int min = ClanConfig.TAG_MIN_LENGTH.getAsInt();
        int max = ClanConfig.TAG_MAX_LENGTH.getAsInt();
        if (!TAG_PATTERN.matcher(tag).matches()) {
            return fail(ctx, "В теге можно использовать только буквы и цифры, без пробелов.");
        }
        if (tag.length() < min || tag.length() > max) {
            return fail(ctx, "Тег должен быть от " + min + " до " + max + " символов.");
        }
        clan.tag = tag;
        save(ctx, clan);
        ClanManager.notifyMembers(ctx.getSource().getServer(), clan, Component.literal("Новый тег клана: ").append(ClanManager.tagComponent(clan)));
        return 1;
    }

    private static int setColor(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Clan clan = ledClan(ctx);
        if (clan == null) {
            return 0;
        }
        clan.color = ColorArgument.getColor(ctx, "color");
        save(ctx, clan);
        ClanManager.notifyMembers(ctx.getSource().getServer(), clan, Component.literal("Новый цвет клана: ").append(ClanManager.tagComponent(clan)));
        return 1;
    }

    private static int invite(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Clan clan = ledClan(ctx);
        if (clan == null) {
            return 0;
        }
        if (!ClanPermissions.has(ctx.getSource().getPlayerOrException(), ClanPermissions.INVITE)) {
            return fail(ctx, "Приглашать в клан могут только VIP и выше.");
        }
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        MinecraftServer server = ctx.getSource().getServer();
        if (ClanData.get(server).byMember(target.getUUID()) != null) {
            return fail(ctx, target.getScoreboardName() + " уже состоит в клане.");
        }
        int maxMembers = ClanConfig.MAX_MEMBERS.getAsInt();
        if (maxMembers > 0 && clan.members.size() >= maxMembers) {
            return fail(ctx, "В клане уже максимум участников (" + maxMembers + ").");
        }
        ClanManager.invite(target.getUUID(), clan);
        target.sendSystemMessage(Component.empty().append(ClanManager.prefix())
                .append(Component.literal(ctx.getSource().getTextName() + " зовёт тебя в клан "))
                .append(ClanManager.tagComponent(clan))
                .append(Component.literal(" " + clan.name + ". Принять: /clan accept, отказаться: /clan decline")));
        return ok(ctx, Component.literal("Приглашение отправлено игроку " + target.getScoreboardName() + "."));
    }

    private static int accept(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        MinecraftServer server = ctx.getSource().getServer();
        ClanData data = ClanData.get(server);
        Clan clan = ClanManager.takeInvite(server, player.getUUID());
        if (clan == null) {
            return fail(ctx, "У тебя нет действующих приглашений.");
        }
        if (data.byMember(player.getUUID()) != null) {
            return fail(ctx, "Ты уже состоишь в клане.");
        }
        int maxMembers = ClanConfig.MAX_MEMBERS.getAsInt();
        if (maxMembers > 0 && clan.members.size() >= maxMembers) {
            return fail(ctx, "В клане уже нет мест.");
        }
        clan.members.put(player.getUUID(), player.getScoreboardName());
        data.setDirty();
        ClanManager.applyTeam(server, clan);
        ClanManager.notifyMembers(server, clan, Component.literal(player.getScoreboardName() + " вступил в клан."));
        return 1;
    }

    private static int decline(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (ClanManager.takeInvite(ctx.getSource().getServer(), player.getUUID()) == null) {
            return fail(ctx, "У тебя нет действующих приглашений.");
        }
        return ok(ctx, Component.literal("Приглашение отклонено."));
    }

    private static int leave(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        MinecraftServer server = ctx.getSource().getServer();
        ClanData data = ClanData.get(server);
        Clan clan = data.byMember(player.getUUID());
        if (clan == null) {
            return fail(ctx, "Ты не состоишь в клане.");
        }
        if (clan.isLeader(player.getUUID())) {
            if (clan.members.size() > 1) {
                return fail(ctx, "Ты лидер. Сначала передай лидерство (/clan transfer <игрок>) или распусти клан (/clan disband).");
            }
            return disbandClan(ctx, clan, "Клан «" + clan.name + "» распущен.");
        }
        clan.members.remove(player.getUUID());
        data.setDirty();
        ClanManager.removeFromTeam(server, clan, player.getScoreboardName());
        ClanManager.syncTag(player.getUUID(), null);
        ClanManager.notifyMembers(server, clan, Component.literal(player.getScoreboardName() + " покинул клан."));
        return ok(ctx, Component.literal("Ты вышел из клана «" + clan.name + "»."));
    }

    private static int kick(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Clan clan = ledClan(ctx);
        if (clan == null) {
            return 0;
        }
        UUID target = memberByName(clan, StringArgumentType.getString(ctx, "player"));
        if (target == null) {
            return fail(ctx, "Такого игрока нет в клане.");
        }
        if (clan.isLeader(target)) {
            return fail(ctx, "Нельзя выгнать самого себя.");
        }
        MinecraftServer server = ctx.getSource().getServer();
        String name = clan.members.remove(target);
        ClanData.get(server).setDirty();
        ClanManager.removeFromTeam(server, clan, name);
        ClanManager.syncTag(target, null);
        ClanManager.notifyMembers(server, clan, Component.literal(name + " исключён из клана."));
        ServerPlayer online = server.getPlayerList().getPlayer(target);
        if (online != null) {
            online.sendSystemMessage(Component.empty().append(ClanManager.prefix())
                    .append(Component.literal("Тебя исключили из клана «" + clan.name + "».")));
        }
        return 1;
    }

    private static int transfer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Clan clan = ledClan(ctx);
        if (clan == null) {
            return 0;
        }
        UUID target = memberByName(clan, StringArgumentType.getString(ctx, "player"));
        if (target == null) {
            return fail(ctx, "Такого игрока нет в клане.");
        }
        if (clan.isLeader(target)) {
            return fail(ctx, "Ты и так лидер.");
        }
        clan.leader = target;
        MinecraftServer server = ctx.getSource().getServer();
        ClanData.get(server).setDirty();
        ClanManager.notifyMembers(server, clan, Component.literal("Новый лидер клана: " + clan.leaderName() + "."));
        return 1;
    }

    private static int disband(CommandContext<CommandSourceStack> ctx, boolean confirmed) throws CommandSyntaxException {
        Clan clan = ledClan(ctx);
        if (clan == null) {
            return 0;
        }
        if (!confirmed) {
            return ok(ctx, Component.literal("Точно распустить клан «" + clan.name + "»? Это нельзя отменить. Подтвердить: /clan disband confirm")
                    .withStyle(ChatFormatting.RED));
        }
        return disbandClan(ctx, clan, "Клан «" + clan.name + "» распущен лидером.");
    }

    private static int adminDisband(CommandContext<CommandSourceStack> ctx) {
        Clan clan = ClanData.get(ctx.getSource().getServer()).byName(StringArgumentType.getString(ctx, "name").trim());
        if (clan == null) {
            return fail(ctx, "Клан не найден.");
        }
        disbandClan(ctx, clan, "Клан «" + clan.name + "» распущен администрацией.");
        return ok(ctx, Component.literal("Клан «" + clan.name + "» распущен."));
    }

    private static int info(CommandContext<CommandSourceStack> ctx, String name) {
        MinecraftServer server = ctx.getSource().getServer();
        ClanData data = ClanData.get(server);
        Clan clan;
        if (name != null) {
            clan = data.byName(name.trim());
        } else {
            ServerPlayer player = ctx.getSource().getPlayer();
            clan = player == null ? null : data.byMember(player.getUUID());
        }
        if (clan == null) {
            return fail(ctx, name != null ? "Клан не найден." : "Ты не состоишь в клане. Посмотреть другой: /clan info <название>");
        }
        String members = clan.members.entrySet().stream()
                .map(e -> e.getValue() + (server.getPlayerList().getPlayer(e.getKey()) != null ? " (в сети)" : ""))
                .collect(Collectors.joining(", "));
        MutableComponent text = Component.empty()
                .append(ClanManager.tagComponent(clan))
                .append(Component.literal(" " + clan.name + "\nЛидер: " + clan.leaderName()
                        + "\nУчастники (" + clan.members.size() + "): " + members));
        return ok(ctx, text);
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        var clans = ClanData.get(ctx.getSource().getServer()).all();
        if (clans.isEmpty()) {
            return ok(ctx, Component.literal("Кланов пока нет."));
        }
        MutableComponent text = Component.literal("Кланы (" + clans.size() + "):");
        for (Clan clan : clans) {
            text.append(Component.literal("\n")).append(ClanManager.tagComponent(clan))
                    .append(Component.literal(" " + clan.name + " — " + clan.members.size() + " чел."));
        }
        return ok(ctx, text);
    }

    // ---- вспомогательное ----

    /** Клан, которым руководит отправитель; иначе сообщает об ошибке и возвращает null. */
    private static Clan ledClan(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Clan clan = ClanData.get(ctx.getSource().getServer()).byMember(player.getUUID());
        if (clan == null) {
            fail(ctx, "Ты не состоишь в клане.");
            return null;
        }
        if (!clan.isLeader(player.getUUID())) {
            fail(ctx, "Это может сделать только лидер клана (" + clan.leaderName() + ").");
            return null;
        }
        return clan;
    }

    private static int disbandClan(CommandContext<CommandSourceStack> ctx, Clan clan, String message) {
        MinecraftServer server = ctx.getSource().getServer();
        ClanManager.notifyMembers(server, clan, Component.literal(message));
        ClanManager.removeTeam(server, clan);
        ClanData.get(server).remove(clan);
        return 1;
    }

    private static void save(CommandContext<CommandSourceStack> ctx, Clan clan) {
        MinecraftServer server = ctx.getSource().getServer();
        ClanData.get(server).setDirty();
        ClanManager.applyTeam(server, clan);
    }

    private static UUID memberByName(Clan clan, String name) {
        for (Map.Entry<UUID, String> entry : clan.members.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(name)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static String validateName(String name) {
        int min = ClanConfig.NAME_MIN_LENGTH.getAsInt();
        int max = ClanConfig.NAME_MAX_LENGTH.getAsInt();
        if (!NAME_PATTERN.matcher(name).matches()) {
            return "В названии можно использовать только буквы, цифры, пробел и _.";
        }
        if (name.length() < min || name.length() > max) {
            return "Название должно быть от " + min + " до " + max + " символов.";
        }
        return null;
    }

    /** Тег по умолчанию — первые буквы/цифры названия в верхнем регистре. */
    private static String defaultTag(String name) {
        String letters = name.replaceAll("[^\\p{L}\\p{N}]", "").toUpperCase(Locale.ROOT);
        int max = ClanConfig.TAG_MAX_LENGTH.getAsInt();
        return letters.length() > max ? letters.substring(0, max) : letters;
    }

    private static int ok(CommandContext<CommandSourceStack> ctx, Component message) {
        ctx.getSource().sendSuccess(() -> Component.empty().append(ClanManager.prefix()).append(message), false);
        return 1;
    }

    private static int fail(CommandContext<CommandSourceStack> ctx, String message) {
        ctx.getSource().sendFailure(Component.empty().append(ClanManager.prefix()).append(Component.literal(message)));
        return 0;
    }
}
