package io.github.sheynor43.clans;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;

/**
 * Ад и Энд закрыты, пока администратор их не откроет (как WorldToggle в 1-м сезоне).
 * /worldtoggle — статус, /worldtoggle nether|end open|close — открыть или закрыть.
 */
public final class WorldGate extends SavedData {
    private static final Codec<WorldGate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("nether_open").forGetter(g -> g.netherOpen),
            Codec.BOOL.fieldOf("end_open").forGetter(g -> g.endOpen)
    ).apply(instance, WorldGate::new));

    private static final SavedDataType<WorldGate> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Sector43Clans.MODID, "world_gate"), WorldGate::new, CODEC, null);

    private boolean netherOpen;
    private boolean endOpen;

    public WorldGate() {
        this(false, false);
    }

    private WorldGate(boolean netherOpen, boolean endOpen) {
        this.netherOpen = netherOpen;
        this.endOpen = endOpen;
    }

    static WorldGate get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    private boolean isOpen(ResourceKey<Level> dimension) {
        if (dimension == Level.NETHER) {
            return netherOpen;
        }
        if (dimension == Level.END) {
            return endOpen;
        }
        return true;
    }

    public static void onTravel(EntityTravelToDimensionEvent event) {
        if (event.getEntity().level().getServer() == null) {
            return;
        }
        ResourceKey<Level> target = event.getDimension();
        if (get(event.getEntity().level().getServer()).isOpen(target)) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player && player.isCreative()) {
            return;
        }
        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer player) {
            String name = target == Level.NETHER ? "Ад" : "Энд";
            player.sendOverlayMessage(Component.literal(name + " пока закрыт. Следи за объявлениями!").withStyle(ChatFormatting.RED));
        }
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("worldtoggle")
                .requires(WorldGate::canToggle)
                .executes(WorldGate::status)
                .then(Commands.literal("nether")
                        .then(Commands.literal("open").executes(ctx -> set(ctx, true, true)))
                        .then(Commands.literal("close").executes(ctx -> set(ctx, true, false))))
                .then(Commands.literal("end")
                        .then(Commands.literal("open").executes(ctx -> set(ctx, false, true)))
                        .then(Commands.literal("close").executes(ctx -> set(ctx, false, false)))));
    }

    private static boolean canToggle(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        return player == null || ClanPermissions.has(player, ClanPermissions.WORLDTOGGLE);
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        WorldGate gate = get(ctx.getSource().getServer());
        ctx.getSource().sendSuccess(() -> Component.literal("Ад: " + state(gate.netherOpen) + ", Энд: " + state(gate.endOpen)), false);
        return 1;
    }

    private static int set(CommandContext<CommandSourceStack> ctx, boolean nether, boolean open) {
        MinecraftServer server = ctx.getSource().getServer();
        WorldGate gate = get(server);
        if (nether) {
            gate.netherOpen = open;
        } else {
            gate.endOpen = open;
        }
        gate.setDirty();
        String name = nether ? "Ад" : "Энд";
        server.getPlayerList().broadcastSystemMessage(
                Component.literal(name + (open ? " открыт!" : " закрыт.")).withStyle(open ? ChatFormatting.GOLD : ChatFormatting.RED), false);
        return 1;
    }

    private static String state(boolean open) {
        return open ? "открыт" : "закрыт";
    }
}
