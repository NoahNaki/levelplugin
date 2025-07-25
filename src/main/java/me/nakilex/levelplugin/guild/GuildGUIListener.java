package me.nakilex.levelplugin.guild;

import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GuildGUIListener implements Listener {
    private final GuildManager manager = GuildManager.getInstance();
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = ChatColor.stripColor(event.getView().getTitle());
        if (!"Guilds".equals(title)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;
        String name = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
        if (name.isEmpty()) return;
        if (player.isOp()) {
            // ops might shift-click etc - ignore
        }
        if (manager.getGuild(player.getUniqueId()) != null) return;
        if (!event.isLeftClick()) return;
        if (manager.apply(player.getUniqueId(), name)) {
            player.sendMessage(ChatColor.GREEN + "Applied to guild " + name + ".");
            Guild g = manager.getGuild(name);
            if (g != null) {
                OfflinePlayer leader = Bukkit.getOfflinePlayer(g.getLeader());
                if (leader.isOnline()) {
                    ((Player) leader.getPlayer()).sendMessage(ChatColor.YELLOW + player.getName() + " applied to join your guild.");
                }
            }
        } else {
            player.sendMessage(ChatColor.RED + "Could not apply to guild.");
        }
    }
}
