package io.github.sheynor43.clans;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

/**
 * Права читаются через PermissionAPI NeoForge, поэтому ими управляет LuckPerms.
 * Без LuckPerms (или если право не задано) они есть только у операторов.
 */
public final class ClanPermissions {
    // sector43clans.create — создавать кланы (выдать группе vip)
    public static final PermissionNode<Boolean> CREATE = new PermissionNode<>(
            Sector43Clans.MODID, "create", PermissionTypes.BOOLEAN,
            (player, uuid, context) -> isOperator(player));

    // sector43clans.invite — приглашать в клан (выдать группе vip)
    public static final PermissionNode<Boolean> INVITE = new PermissionNode<>(
            Sector43Clans.MODID, "invite", PermissionTypes.BOOLEAN,
            (player, uuid, context) -> isOperator(player));

    // sector43clans.admin — распускать чужие кланы
    public static final PermissionNode<Boolean> ADMIN = new PermissionNode<>(
            Sector43Clans.MODID, "admin", PermissionTypes.BOOLEAN,
            (player, uuid, context) -> isOperator(player));

    // sector43clans.worldtoggle — открывать и закрывать Ад и Энд
    public static final PermissionNode<Boolean> WORLDTOGGLE = new PermissionNode<>(
            Sector43Clans.MODID, "worldtoggle", PermissionTypes.BOOLEAN,
            (player, uuid, context) -> isOperator(player));

    // sector43clans.resize — менять свой рост командой /resize (выдать группе vip)
    public static final PermissionNode<Boolean> RESIZE = new PermissionNode<>(
            Sector43Clans.MODID, "resize", PermissionTypes.BOOLEAN,
            (player, uuid, context) -> isOperator(player));

    // sector43clans.resize.others — менять рост другим игрокам
    public static final PermissionNode<Boolean> RESIZE_OTHERS = new PermissionNode<>(
            Sector43Clans.MODID, "resize.others", PermissionTypes.BOOLEAN,
            (player, uuid, context) -> isOperator(player));

    private ClanPermissions() {}

    public static boolean has(ServerPlayer player, PermissionNode<Boolean> node) {
        return PermissionAPI.getPermission(player, node);
    }

    private static boolean isOperator(ServerPlayer player) {
        return player != null && player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }
}
