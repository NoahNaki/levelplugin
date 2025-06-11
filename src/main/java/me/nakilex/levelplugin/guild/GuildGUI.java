package me.nakilex.levelplugin.guild;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class GuildGUI {

    private final GuildManager manager;
    private static final int SIZE = 54;
    private static final String TITLE = ChatColor.AQUA + "Guilds";

    public GuildGUI(GuildManager manager) {
        this.manager = manager;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        ItemStack filler = createFiller();
        for (int i = 0; i < SIZE; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, filler);
            }
        }
        for (Guild g : manager.getGuilds()) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            OfflinePlayer lp = Bukkit.getOfflinePlayer(g.getLeader());
            meta.setOwningPlayer(lp);
            meta.setDisplayName(ChatColor.GOLD + g.getName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "Leader: " + g.getLeaderName());
            lore.add(ChatColor.WHITE + "Members: " + g.getMembers().size());
            lore.add(ChatColor.GREEN + "Allies: " + String.join(", ", g.getAllies()));
            lore.add(ChatColor.RED + "Hostile: " + String.join(", ", g.getHostiles()));
            meta.setLore(lore);
            head.setItemMeta(meta);
            inv.addItem(head);
        }
        player.openInventory(inv);
    }

    private static ItemStack createFiller() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            glass.setItemMeta(meta);
        }
        return glass;
    }

    private static ItemStack getNexoItem(String id, String name) {
        ItemBuilder b = NexoItems.itemFromId(id);
        if (b == null) return new ItemStack(Material.BARRIER);
        ItemStack it = b.build();
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            it.setItemMeta(meta);
        }
        return it;
    }
}
