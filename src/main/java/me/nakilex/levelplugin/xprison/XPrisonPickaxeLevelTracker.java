package me.nakilex.levelplugin.xprison;

import dev.drawethree.xprison.api.XPrisonAPI;
import dev.drawethree.xprison.api.pickaxelevels.event.PlayerPickaxeLevelUpEvent;
import dev.drawethree.xprison.api.pickaxelevels.model.PickaxeLevel;
import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Optional;

/**
 * Snapshots each player's X-Prison pickaxe level into LevelPlugin's own player data.
 *
 * <p>X-Prison stores pickaxe level/exp on the pickaxe item itself (NBT), not per-account, so its API has
 * no offline lookup - {@code getTopByPickaxeLevel} only scans currently online players' held pickaxe. This
 * bridge keeps a persisted snapshot per player so the animated "PICKAXE LEVEL" leaderboard can also show
 * players who are offline or not currently holding their pickaxe.</p>
 */
public final class XPrisonPickaxeLevelTracker implements Listener {

    private final Main plugin;
    private final PickaxeLevelUpTotemAnimator levelUpTotemAnimator;
    private boolean enabled;

    public XPrisonPickaxeLevelTracker(Main plugin) {
        this.plugin = plugin;
        this.levelUpTotemAnimator = new PickaxeLevelUpTotemAnimator(plugin);
    }

    public void enable() {
        if (enabled) {
            return;
        }
        if (!Bukkit.getPluginManager().isPluginEnabled("X-Prison")) {
            plugin.getLogger().info("X-Prison is not enabled; pickaxe-level leaderboard tracking is disabled.");
            return;
        }
        try {
            XPrisonAPI.getInstance();
        } catch (RuntimeException | LinkageError ex) {
            plugin.getLogger().warning("Could not access X-Prison API; pickaxe-level leaderboard tracking was not enabled: "
                    + ex.getMessage());
            return;
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);
        enabled = true;

        for (Player online : Bukkit.getOnlinePlayers()) {
            snapshot(online);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLevelUp(PlayerPickaxeLevelUpEvent event) {
        PickaxeLevel newLevel = event.getNewLevel();
        if (newLevel == null) {
            return;
        }
        plugin.getPlayerConfig().setXPrisonPickaxeLevel(event.getPlayer().getUniqueId(), newLevel.getLevel());
        levelUpTotemAnimator.play(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> snapshot(player), 20L);
    }

    /** Final accurate snapshot as the player leaves, so offline leaderboard reads stay fresh without polling. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        snapshot(event.getPlayer());
    }

    private void snapshot(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        try {
            Optional<PickaxeLevel> level = XPrisonAPI.getInstance().getPickaxeLevelsApi().getPickaxeLevel(player);
            level.ifPresent(pickaxeLevel ->
                    plugin.getPlayerConfig().setXPrisonPickaxeLevel(player.getUniqueId(), pickaxeLevel.getLevel()));
        } catch (RuntimeException | LinkageError ex) {
            plugin.getLogger().warning("Could not snapshot X-Prison pickaxe level for " + player.getName()
                    + ": " + ex.getMessage());
        }
    }
}
