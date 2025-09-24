package me.nakilex.levelplugin.cutscene.editor;

import me.nakilex.levelplugin.cutscene.CutsceneManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class EditorListener implements Listener {
    private final CutsceneManager manager;

    public EditorListener(CutsceneManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || !manager.isRecording(event.getPlayer())) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return;
        }
        String label = ChatColor.stripColor(meta.getDisplayName());
        if (label == null) {
            return;
        }

        Action action = event.getAction();
        var player = event.getPlayer();
        boolean sneaking = player.isSneaking();

        if (label.startsWith("Add Frame")) {
            manager.addFrame(player);
            event.setCancelled(true);
        } else if (label.startsWith("Save")) {
            manager.finishRecording(player);
            event.setCancelled(true);
        } else if (label.startsWith("Cancel")) {
            manager.cancelRecording(player);
            event.setCancelled(true);
        } else if (label.startsWith("Mode:")) {
            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                manager.cycleMode(player, -1);
            } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                manager.cycleMode(player, 1);
            }
            event.setCancelled(true);
        } else if (label.startsWith("Duration:")) {
            long step = sneaking ? 1000L : 500L;
            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                manager.adjustDuration(player, -step);
            } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                manager.adjustDuration(player, step);
            }
            event.setCancelled(true);
        } else if (label.startsWith("Speed:")) {
            double step = sneaking ? 0.5 : 1.0;
            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                manager.adjustTeleportSpeed(player, -step);
            } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                manager.adjustTeleportSpeed(player, step);
            }
            event.setCancelled(true);
        } else if (label.startsWith("Look Target")) {
            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                manager.clearLookTarget(player);
            } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                manager.captureLookTarget(player);
            }
            event.setCancelled(true);
        } else if (label.startsWith("Effects")) {
            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                if (sneaking) {
                    manager.promptBundles(player);
                } else {
                    manager.promptActionBar(player);
                }
            } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                manager.promptTitle(player);
            }
            event.setCancelled(true);
        } else if (label.startsWith("Metadata")) {
            if (sneaking) {
                manager.toggleAutoStart(player);
            } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                manager.promptTags(player);
            } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                manager.promptDescription(player);
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (!manager.isRecording(event.getPlayer())) {
            return;
        }
        if (manager.handleChat(event.getPlayer(), event.getMessage())) {
            event.setCancelled(true);
        }
    }
}
