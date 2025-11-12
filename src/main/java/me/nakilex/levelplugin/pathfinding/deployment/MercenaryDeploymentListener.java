package me.nakilex.levelplugin.pathfinding.deployment;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.pathfinding.deployment.gui.MercenaryDeploymentBoard;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/** Listener responsible for contract board interactions and player lifecycle hooks. */
class MercenaryDeploymentListener implements Listener {
    private final MercenaryDeploymentManager manager;
    private final Main plugin;

    MercenaryDeploymentListener(MercenaryDeploymentManager manager) {
        this.manager = manager;
        this.plugin = Main.getInstance();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        manager.handleJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.handleQuit(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!MercenaryDeploymentBoard.TITLE.equals(event.getView().getTitle())) {
            return;
        }
        if (event.getClickedInventory() == null) {
            event.setCancelled(true);
            return;
        }
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) {
            event.setCancelled(true);
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            event.setCancelled(true);
            return;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String action = container.get(MercenaryDeploymentBoard.KEY_ACTION, PersistentDataType.STRING);
        String id = container.get(MercenaryDeploymentBoard.KEY_ID, PersistentDataType.STRING);
        if (action == null || id == null) {
            event.setCancelled(true);
            return;
        }
        event.setCancelled(true);
        switch (action) {
            case MercenaryDeploymentBoard.ACTION_START -> handleStart(player, id, event.getClick());
            case MercenaryDeploymentBoard.ACTION_CLAIM -> handleClaim(player, id, event.getClick());
            case MercenaryDeploymentBoard.ACTION_CANCEL -> handleCancel(player, id, event.getClick());
            default -> { }
        }
    }

    private void handleStart(Player player, String deploymentId, ClickType click) {
        if (!click.isLeftClick()) {
            return;
        }
        var defOpt = manager.getDefinition(deploymentId);
        if (defOpt.isEmpty()) {
            player.sendMessage(ChatColor.RED + "That contract has rotated out.");
            return;
        }
        boolean started = manager.startDeployment(player, deploymentId, defOpt.get().recommended());
        if (started) {
            reopenBoard(player);
        }
    }

    private void handleClaim(Player player, String deploymentId, ClickType click) {
        if (!click.isLeftClick()) {
            return;
        }
        if (manager.claim(player, deploymentId)) {
            reopenBoard(player);
        }
    }

    private void handleCancel(Player player, String deploymentId, ClickType click) {
        if (!click.isRightClick()) {
            return;
        }
        if (manager.cancelDeployment(player, deploymentId)) {
            reopenBoard(player);
        }
    }

    private void reopenBoard(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> manager.openBoard(player));
    }
}
