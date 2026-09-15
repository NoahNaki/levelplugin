package me.nakilex.levelplugin.playerhead;

import net.kyori.adventure.resource.ResourcePackInfo;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Sends the player-model shader pack a couple seconds after join, once Nexo's own pack (dispatched
 * during the pre-join configuration phase) has finished applying. Minecraft stacks multiple
 * server-sent resource packs rather than replacing them, so sending ours after Nexo's lets it win
 * the {@code rendertype_text} override instead of being shadowed by Nexo's version overlay.
 */
public final class PlayerModelResourcePackDispatcher implements Listener {
    private static final UUID PACK_ID = UUID.nameUUIDFromBytes(
            "levelplugin:playermodel-shader".getBytes(StandardCharsets.UTF_8));
    private static final long SEND_DELAY_TICKS = 40L;

    private final Plugin plugin;
    private final ResourcePackInfo packInfo;

    public PlayerModelResourcePackDispatcher(Plugin plugin, String url, String sha1Hex) {
        this.plugin = plugin;
        this.packInfo = ResourcePackInfo.resourcePackInfo(PACK_ID, URI.create(url), sha1Hex);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) player.sendResourcePacks(packInfo);
        }, SEND_DELAY_TICKS);
    }
}
