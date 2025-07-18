package me.nakilex.levelplugin.cutscene.editor;

import me.nakilex.levelplugin.cutscene.CutsceneManager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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

        if (name.contains("Add Frame")) {
            manager.addFrame(event.getPlayer(), 2000L);
            event.getPlayer().sendMessage("Added frame");
            event.setCancelled(true);
        } else if (name.contains("Save")) {
            manager.finishRecording(event.getPlayer());
            event.getPlayer().sendMessage("Cutscene saved");
            event.setCancelled(true);
        } else if (name.contains("Cancel")) {
            manager.cancelRecording(event.getPlayer());
            event.getPlayer().sendMessage("Recording cancelled");
            event.setCancelled(true);
        }
    }
}
