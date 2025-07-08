package me.nakilex.levelplugin.guild;

import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.HeadUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Simple GUI listing members of a player's guild using their heads. */
public class GuildMemberGUI {
    private final GuildManager manager;
    private static final int SIZE = 54;
    private static final String TITLE = ChatColor.AQUA + "Guild Members";

    public GuildMemberGUI(GuildManager manager) {
        this.manager = manager;
    }

    public void open(Player player) {
        Guild g = manager.getGuild(player.getUniqueId());
        if (g == null) {
            player.sendMessage(ChatColor.RED + "You are not in a guild.");
            return;
        }
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, filler);
            }
        }
        for (UUID id : g.getMembers()) {
            OfflinePlayer p = Bukkit.getOfflinePlayer(id);
            List<String> lore = new ArrayList<>();
            if (id.equals(g.getLeader())) lore.add(ChatColor.GOLD + "Leader");
            ItemStack head = HeadUtil.createPlayerHead(p, ChatColor.YELLOW + p.getName(), lore);
            inv.addItem(head);
        }
        player.openInventory(inv);
    }
}
