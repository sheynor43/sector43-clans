package io.github.sheynor43.clans;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;

public class Clan {
    public static final Codec<Clan> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("team").forGetter(c -> c.teamName),
            Codec.STRING.fieldOf("name").forGetter(c -> c.name),
            Codec.STRING.fieldOf("tag").forGetter(c -> c.tag),
            Codec.STRING.fieldOf("color").forGetter(c -> c.color.getName()),
            UUIDUtil.CODEC.fieldOf("leader").forGetter(c -> c.leader),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.STRING).fieldOf("members").forGetter(c -> c.members)
    ).apply(instance, Clan::new));

    /** Имя команды scoreboard, только ASCII, не меняется после создания. */
    public final String teamName;
    public String name;
    public String tag;
    public ChatFormatting color;
    public UUID leader;
    /** UUID участника → последний известный ник (включая лидера). */
    public final Map<UUID, String> members;

    public Clan(String teamName, String name, String tag, ChatFormatting color, UUID leader, Map<UUID, String> members) {
        this.teamName = teamName;
        this.name = name;
        this.tag = tag;
        this.color = color;
        this.leader = leader;
        this.members = new LinkedHashMap<>(members);
    }

    private Clan(String teamName, String name, String tag, String color, UUID leader, Map<UUID, String> members) {
        this(teamName, name, tag, parseColor(color), leader, members);
    }

    private static ChatFormatting parseColor(String color) {
        ChatFormatting parsed = ChatFormatting.getByName(color);
        return parsed != null && parsed.isColor() ? parsed : ChatFormatting.WHITE;
    }

    public boolean isLeader(UUID player) {
        return leader.equals(player);
    }

    public String leaderName() {
        return members.getOrDefault(leader, "?");
    }
}
