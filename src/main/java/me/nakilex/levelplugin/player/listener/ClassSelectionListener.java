package me.nakilex.levelplugin.player.listener;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Ensures players pick a starting class before playing.
 * Players in the pending set cannot move and the menu will
 * reopen if they try to close it without picking.
 */
public class ClassSelectionListener implements Listener {

    private static final Set<UUID> PENDING = new HashSet<>();
    private final Main plugin;

    public ClassSelectionListener(Main plugin) {
        this.plugin = plugin;
    }

    /** Mark a player as needing to choose a class. */
    public static void addPending(Player p) {
        PENDING.add(p.getUniqueId());
    }

    /** Called after a player successfully chooses a class. */
    public static void completeSelection(Player p) {
        PENDING.remove(p.getUniqueId());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!PENDING.contains(event.getPlayer().getUniqueId())) return;
        if (event.getFrom().getX() != event.getTo().getX()
                || event.getFrom().getY() != event.getTo().getY()
                || event.getFrom().getZ() != event.getTo().getZ()) {
            event.setTo(event.getFrom());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        if (!PENDING.contains(player.getUniqueId())) return;

        StatsManager.PlayerStats ps = StatsManager.getInstance()
                .getPlayerStats(player.getUniqueId());
        if (ps.playerClass == PlayerClass.VILLAGER) {
            player.sendMessage(ChatColor.RED +
                    "You must select a starting class before continuing!");
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            "dm open mmocore_class_warrior -p:" + player.getName()),
                    1L);
        } else {
            completeSelection(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        PENDING.remove(event.getPlayer().getUniqueId());
    }
}
