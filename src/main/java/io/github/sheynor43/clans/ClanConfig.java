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

    static final ModConfigSpec SPEC = BUILDER.build();
}
