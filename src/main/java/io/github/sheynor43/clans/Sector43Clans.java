package io.github.sheynor43.clans;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import org.slf4j.Logger;

@Mod(Sector43Clans.MODID)
public class Sector43Clans {
    public static final String MODID = "sector43clans";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Sector43Clans(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, ClanConfig.SPEC);

        NeoForge.EVENT_BUS.addListener(PermissionGatherEvent.Nodes.class, event -> event.addNodes(ClanPermissions.CREATE, ClanPermissions.ADMIN));
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event -> ClanCommands.register(event.getDispatcher()));
        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, event -> ClanManager.syncAll(event.getServer()));
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, event -> ClanManager.clearInvites());
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                ClanManager.onLogin(player);
            }
        });
    }
}
