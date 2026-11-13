package io.github.sheynor43.clans;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Хранится в world/data/sector43clans/clans.dat. */
public class ClanData extends SavedData {
    private static final Codec<ClanData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("next_id").forGetter(d -> d.nextId),
            Clan.CODEC.listOf().fieldOf("clans").forGetter(d -> d.clans)
    ).apply(instance, ClanData::new));

    private static final SavedDataType<ClanData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Sector43Clans.MODID, "clans"), ClanData::new, CODEC, null);

    private int nextId;
    private final List<Clan> clans;

    public ClanData() {
        this(1, List.of());
    }

    private ClanData(int nextId, List<Clan> clans) {
        this.nextId = nextId;
        this.clans = new ArrayList<>(clans);
    }

    public static ClanData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public List<Clan> all() {
        return clans;
    }

    public Clan byName(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        for (Clan clan : clans) {
            if (clan.name.toLowerCase(Locale.ROOT).equals(key)) {
                return clan;
            }
        }
        return null;
    }

    public Clan byMember(UUID player) {
        for (Clan clan : clans) {
            if (clan.members.containsKey(player)) {
                return clan;
            }
        }
        return null;
    }

    public String nextTeamName() {
        setDirty();
        return "s43clan_" + nextId++;
    }

    public void add(Clan clan) {
        clans.add(clan);
        setDirty();
    }

    public void remove(Clan clan) {
        clans.remove(clan);
        setDirty();
    }
}
