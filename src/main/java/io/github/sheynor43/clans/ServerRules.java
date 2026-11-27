package io.github.sheynor43.clans;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Правила сервера из 1-го сезона: элитры запрещены в Энде (NoElytraInEnd),
 * перегрев в Аду (NetherOverheat) и процент спящих для пропуска ночи (SleepVote).
 */
public final class ServerRules {
    private static final int HEARTBEAT = 5;
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Map<UUID, Heat> HEAT = new HashMap<>();

    private ServerRules() {}

    private static final class Heat {
        double air = ClanConfig.HEAT_MAX.getAsInt();
        int sinceDamage;
    }

    public static void onServerStarted(MinecraftServer server) {
        int percent = ClanConfig.SLEEP_PERCENTAGE.getAsInt();
        if (percent >= 0) {
            server.getGameRules().set(GameRules.PLAYERS_SLEEPING_PERCENTAGE, percent, server);
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % HEARTBEAT != 0) {
            return;
        }
        if (player.level().dimension() == Level.END && player.tickCount % 20 == 0 && ClanConfig.NO_ELYTRA_IN_END.getAsBoolean()) {
            takeOffElytra(player);
        }
        if (ClanConfig.HEAT_ENABLED.getAsBoolean()) {
            tickHeat(player);
        }
    }

    private static void takeOffElytra(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.isEmpty() || !chest.has(DataComponents.GLIDER)) {
            return;
        }
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        if (player.getInventory().add(chest)) {
            player.sendSystemMessage(Component.literal("С тебя сняты элитры: в Энде они запрещены.").withStyle(ChatFormatting.RED));
        } else {
            player.drop(chest, false);
            player.sendSystemMessage(Component.literal("Элитры в Энде запрещены. В инвентаре не было места, они выброшены на землю.").withStyle(ChatFormatting.RED));
        }
    }

    private static void tickHeat(ServerPlayer player) {
        int max = ClanConfig.HEAT_MAX.getAsInt();
        boolean exempt = player.isCreative() || player.isSpectator() || player.hasEffect(MobEffects.FIRE_RESISTANCE);
        if (player.level().dimension() != Level.NETHER || exempt) {
            Heat heat = HEAT.get(player.getUUID());
            if (heat != null && player.level().dimension() == Level.NETHER) {
                heat.air = max;
            } else {
                HEAT.remove(player.getUUID());
            }
            return;
        }

        Heat heat = HEAT.computeIfAbsent(player.getUUID(), id -> new Heat());
        double interval = ClanConfig.HEAT_DEPLETION_INTERVAL.getAsInt() * (1 + ClanConfig.HEAT_FIRE_PROTECTION_BONUS.getAsDouble() * fireProtection(player));
        heat.air = Math.max(0, heat.air - ClanConfig.HEAT_DEPLETION_AMOUNT.getAsInt() * HEARTBEAT / interval);

        if (heat.air <= 0) {
            heat.sinceDamage += HEARTBEAT;
            if (heat.sinceDamage >= ClanConfig.HEAT_DAMAGE_INTERVAL.getAsInt()) {
                heat.sinceDamage = 0;
                player.hurtServer((ServerLevel) player.level(), player.damageSources().onFire(), (float) ClanConfig.HEAT_DAMAGE.getAsDouble());
                player.igniteForTicks(ClanConfig.HEAT_FIRE_TICKS.getAsInt());
            }
        }
        player.sendOverlayMessage(gauge(heat.air, max));
    }

    private static int fireProtection(ServerPlayer player) {
        Holder<Enchantment> fireProtection = player.level().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FIRE_PROTECTION);
        int levels = 0;
        for (EquipmentSlot slot : ARMOR) {
            levels += EnchantmentHelper.getItemEnchantmentLevel(fireProtection, player.getItemBySlot(slot));
        }
        return levels;
    }

    private static Component gauge(double air, int max) {
        int filled = (int) Math.ceil(air * 10 / max);
        ChatFormatting color = filled > 5 ? ChatFormatting.GOLD : filled > 2 ? ChatFormatting.RED : ChatFormatting.DARK_RED;
        String bar = "▮".repeat(filled) + "▯".repeat(10 - filled);
        String text = filled == 0 ? "🔥 Ты перегрелся! Выпей воды или уходи из Ада" : "🔥 Жара " + bar;
        return Component.literal(text).withStyle(color);
    }

    /** Бутылка воды остужает: шкала перегрева снова полная. */
    public static void onItemUsed(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PotionContents potion = event.getItem().get(DataComponents.POTION_CONTENTS);
        if (potion != null && potion.is(Potions.WATER)) {
            Heat heat = HEAT.get(player.getUUID());
            if (heat != null) {
                heat.air = ClanConfig.HEAT_MAX.getAsInt();
                heat.sinceDamage = 0;
            }
        }
    }

    public static void onLogout(ServerPlayer player) {
        HEAT.remove(player.getUUID());
    }
}
