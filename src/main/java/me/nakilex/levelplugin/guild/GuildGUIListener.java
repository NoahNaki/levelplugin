package me.nakilex.levelplugin.guild;

import me.nakilex.levelplugin.utils.GuiUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GuildGUIListener implements Listener {
    private final GuildGUI guildGUI;
    private final GuildManager manager = GuildManager.getInstance();

    public GuildGUIListener(GuildGUI guildGUI) {
        this.guildGUI = guildGUI;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!GuiUtil.titleMatches(event.getView().getTitle(), "Guilds")) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (guildGUI.handleWidgetClick(event, player)) {
            return;
        }
        event.setCancelled(true);
        if (manager.getGuild(player.getUniqueId()) != null) return;
    }
}
