package io.github.sheynor43.clans;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * /resize <рост в блоках> — меняет рост игрока через ванильный атрибут minecraft:scale,
 * как ResizePlayers в 1-м сезоне. Рост по умолчанию — 1.8 блока.
 */
public final class ResizeCommand {
    private static final double DEFAULT_HEIGHT = 1.8;

    private ResizeCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("resize")
                .executes(ResizeCommand::info)
                .then(Commands.literal("reset").executes(ctx -> resizeSelf(ctx, DEFAULT_HEIGHT)))
                .then(Commands.argument("height", DoubleArgumentType.doubleArg(0.1, 16))
                        .executes(ctx -> resizeSelf(ctx, DoubleArgumentType.getDouble(ctx, "height")))
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(ResizeCommand::canResizeOthers)
                                .executes(ResizeCommand::resizeOther))));
    }

    /** Игрок без права на /resize (например, потерял VIP) возвращается к обычному росту. */
    public static void onLogin(ServerPlayer player) {
        AttributeInstance scale = player.getAttribute(Attributes.SCALE);
        if (scale != null && scale.getBaseValue() != 1.0 && !ClanPermissions.has(player, ClanPermissions.RESIZE)) {
            scale.setBaseValue(1.0);
        }
    }

    private static int info(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        return ok(ctx, Component.literal("Твой рост: " + format(heightOf(player)) + " блока. Изменить: /resize <рост> (от "
                + format(min()) + " до " + format(max()) + "), вернуть обычный: /resize reset"));
    }

    private static int resizeSelf(CommandContext<CommandSourceStack> ctx, double height) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (!ClanPermissions.has(player, ClanPermissions.RESIZE)) {
            return fail(ctx, "Менять рост могут только VIP и выше.");
        }
        if (height != DEFAULT_HEIGHT && (height < min() || height > max())) {
            return fail(ctx, "Рост должен быть от " + format(min()) + " до " + format(max()) + " блока.");
        }
        setHeight(player, height);
        return ok(ctx, Component.literal("Твой рост теперь " + format(height) + " блока."));
    }

    private static int resizeOther(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        double height = DoubleArgumentType.getDouble(ctx, "height");
        setHeight(target, height);
        return ok(ctx, Component.literal("Рост " + target.getScoreboardName() + " теперь " + format(height) + " блока."));
    }

    private static boolean canResizeOthers(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        return player == null || ClanPermissions.has(player, ClanPermissions.RESIZE_OTHERS);
    }

    private static void setHeight(ServerPlayer player, double height) {
        AttributeInstance scale = player.getAttribute(Attributes.SCALE);
        if (scale != null) {
            scale.setBaseValue(height / DEFAULT_HEIGHT);
        }
    }

    private static double heightOf(ServerPlayer player) {
        AttributeInstance scale = player.getAttribute(Attributes.SCALE);
        return (scale == null ? 1.0 : scale.getValue()) * DEFAULT_HEIGHT;
    }

    private static double min() {
        return ClanConfig.RESIZE_MIN_HEIGHT.getAsDouble();
    }

    private static double max() {
        return ClanConfig.RESIZE_MAX_HEIGHT.getAsDouble();
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static int ok(CommandContext<CommandSourceStack> ctx, Component message) {
        ctx.getSource().sendSuccess(() -> message.copy().withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int fail(CommandContext<CommandSourceStack> ctx, String message) {
        ctx.getSource().sendFailure(Component.literal(message));
        return 0;
    }
}
