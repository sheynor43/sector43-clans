package io.github.sheynor43.clans;

import net.neoforged.neoforge.common.ModConfigSpec;

// Лежит в world/serverconfig/sector43clans-server.toml
public class ClanConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue NAME_MIN_LENGTH = BUILDER
            .comment("Минимальная длина названия клана")
            .defineInRange("nameMinLength", 3, 1, 32);
    public static final ModConfigSpec.IntValue NAME_MAX_LENGTH = BUILDER
            .comment("Максимальная длина названия клана")
            .defineInRange("nameMaxLength", 16, 1, 32);
    public static final ModConfigSpec.IntValue TAG_MIN_LENGTH = BUILDER
            .comment("Минимальная длина тега (префикса) клана")
            .defineInRange("tagMinLength", 2, 1, 16);
    public static final ModConfigSpec.IntValue TAG_MAX_LENGTH = BUILDER
            .comment("Максимальная длина тега (префикса) клана")
            .defineInRange("tagMaxLength", 5, 1, 16);
    public static final ModConfigSpec.IntValue MAX_MEMBERS = BUILDER
            .comment("Максимум участников в клане, включая лидера. 0 — без ограничений")
            .defineInRange("maxMembers", 0, 0, 1000);
    public static final ModConfigSpec.BooleanValue COLOR_NAMES = BUILDER
            .comment("Красить ник участника в цвет клана. Если false — цветным будет только тег")
            .define("colorNames", true);
    public static final ModConfigSpec.BooleanValue FRIENDLY_FIRE = BUILDER
            .comment("Могут ли участники одного клана бить друг друга")
            .define("friendlyFire", true);
    public static final ModConfigSpec.IntValue INVITE_TIMEOUT_SECONDS = BUILDER
            .comment("Сколько секунд действует приглашение в клан")
            .defineInRange("inviteTimeoutSeconds", 300, 10, 86400);

    public static final ModConfigSpec.DoubleValue RESIZE_MIN_HEIGHT = BUILDER
            .comment("/resize: минимальный рост в блоках (обычный рост — 1.8)")
            .defineInRange("resizeMinHeight", 1.3, 0.1, 16.0);
    public static final ModConfigSpec.DoubleValue RESIZE_MAX_HEIGHT = BUILDER
            .comment("/resize: максимальный рост в блоках")
            .defineInRange("resizeMaxHeight", 2.5, 0.1, 16.0);

    public static final ModConfigSpec.BooleanValue LUCKPERMS_SUFFIX = BUILDER
            .comment("Записывать тег клана в суффикс LuckPerms, чтобы его показывали FlectonePulse и другие моды чата/таба")
            .define("luckpermsSuffix", true);

    public static final ModConfigSpec.IntValue SLEEP_PERCENTAGE = BUILDER
            .comment("Сколько процентов игроков должно лечь спать, чтобы пропустить ночь (ставится при старте сервера). -1 — не трогать")
            .defineInRange("sleepPercentage", 50, -1, 100);

    public static final ModConfigSpec.BooleanValue NO_ELYTRA_IN_END = BUILDER
            .comment("Снимать элитры с игроков в Энде")
            .define("noElytraInEnd", true);

    public static final ModConfigSpec.BooleanValue HEAT_ENABLED = BUILDER
            .comment("Перегрев в Аду: шкала жары убывает, на нуле игрок горит. Спасают огнестойкость, бутылка воды и уход из Ада")
            .define("netherHeat", true);
    public static final ModConfigSpec.IntValue HEAT_MAX = BUILDER
            .comment("Запас прохлады (как запас воздуха)")
            .defineInRange("netherHeatMax", 300, 1, 100000);
    public static final ModConfigSpec.IntValue HEAT_DEPLETION_INTERVAL = BUILDER
            .comment("Раз во сколько тиков убывает прохлада")
            .defineInRange("netherHeatDepletionInterval", 40, 1, 100000);
    public static final ModConfigSpec.IntValue HEAT_DEPLETION_AMOUNT = BUILDER
            .comment("На сколько убывает прохлада за раз (300 / 10 * 40 тиков = 60 секунд)")
            .defineInRange("netherHeatDepletionAmount", 10, 1, 100000);
    public static final ModConfigSpec.DoubleValue HEAT_FIRE_PROTECTION_BONUS = BUILDER
            .comment("Каждый уровень «Защиты от огня» на броне замедляет перегрев на эту долю (0.5 = +50%)")
            .defineInRange("netherHeatFireProtectionBonus", 0.5, 0.0, 100.0);
    public static final ModConfigSpec.IntValue HEAT_DAMAGE_INTERVAL = BUILDER
            .comment("Раз во сколько тиков урон при перегреве")
            .defineInRange("netherHeatDamageInterval", 20, 1, 100000);
    public static final ModConfigSpec.DoubleValue HEAT_DAMAGE = BUILDER
            .comment("Урон при перегреве")
            .defineInRange("netherHeatDamage", 2.0, 0.0, 1000.0);
    public static final ModConfigSpec.IntValue HEAT_FIRE_TICKS = BUILDER
            .comment("На сколько тиков поджигать при перегреве (0 — не поджигать)")
            .defineInRange("netherHeatFireTicks", 60, 0, 100000);

    static final ModConfigSpec SPEC = BUILDER.build();
}
