package io.github.sheynor43.clans;

import java.util.UUID;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.SuffixNode;

/**
 * Пишет тег клана в LuckPerms как суффикс игрока, чтобы его показывали моды чата и таба
 * (FlectonePulse берёт префикс и суффикс из LuckPerms). Класс трогаем только если LuckPerms загружен.
 */
final class LuckPermsBridge {
    /** Приоритет суффикса клана: выше обычных суффиксов групп, чтобы показывался именно он. */
    private static final int PRIORITY = 1043;

    private LuckPermsBridge() {}

    static void setSuffix(UUID player, String suffix) {
        LuckPermsProvider.get().getUserManager().modifyUser(player, user -> {
            user.data().clear(NodeType.SUFFIX.predicate(node -> node.getPriority() == PRIORITY));
            if (suffix != null) {
                user.data().add(SuffixNode.builder(suffix, PRIORITY).build());
            }
        });
    }
}
