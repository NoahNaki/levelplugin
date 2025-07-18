package me.nakilex.levelplugin.cutscene.editor;

import me.nakilex.levelplugin.cutscene.CutsceneManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
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
        if (item == null || !manager.isRecording(event.getPlayer())) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;
        String name = meta.getDisplayName();

        Action action = event.getAction();

        if (name.contains("Add Frame")) {
            manager.addFrame(event.getPlayer());
            event.getPlayer().sendMessage(ChatColor.GRAY + "Added frame");
            event.setCancelled(true);
        } else if (name.contains("Save")) {
            manager.finishRecording(event.getPlayer());
            event.getPlayer().sendMessage(ChatColor.GREEN + "Cutscene saved");
            event.setCancelled(true);
        } else if (name.contains("Cancel")) {
            manager.cancelRecording(event.getPlayer());
            event.getPlayer().sendMessage(ChatColor.RED + "Recording cancelled");
            event.setCancelled(true);
        } else if (name.contains("Speed:")) {
            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                manager.changeSpeed(event.getPlayer(), 1);
            } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                manager.changeSpeed(event.getPlayer(), -1);
            }
            event.setCancelled(true);
        } else if (name.contains("Mode:")) {
            manager.toggleMovement(event.getPlayer());
            event.setCancelled(true);
        } else if (name.contains("Pause:")) {
            if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                manager.changePause(event.getPlayer(), 500L);
            } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                manager.changePause(event.getPlayer(), -500L);
            }
            event.setCancelled(true);
        }
    }
}
