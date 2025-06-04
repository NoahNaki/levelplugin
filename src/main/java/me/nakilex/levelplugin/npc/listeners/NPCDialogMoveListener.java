package me.nakilex.levelplugin.npc.listeners;

import me.nakilex.levelplugin.npc.dialog.NPCDialogManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class NPCDialogMoveListener implements Listener {
    private final NPCDialogManager dialogManager;

    public NPCDialogMoveListener(NPCDialogManager dialogManager) {
        this.dialogManager = dialogManager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        dialogManager.checkDistance(event.getPlayer(), 25); // 5 blocks squared
    }
}
