package me.nakilex.levelplugin.guild;

import me.nakilex.levelplugin.utils.GuiUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import me.nakilex.levelplugin.utils.HeadUtil;

import java.util.ArrayList;
import java.util.List;

public class GuildGUI {

    private final GuildManager manager;
    private static final int SIZE = 54;
    private static final String TITLE = ChatColor.BLACK + "Guilds";

    public GuildGUI(GuildManager manager) {
        this.manager = manager;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, filler);
            }
        }
        for (Guild g : manager.getGuilds()) {
            OfflinePlayer lp = Bukkit.getOfflinePlayer(g.getLeader());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "Leader: " + g.getLeaderName());
            lore.add(ChatColor.WHITE + "Members: " + g.getMembers().size());
            lore.add(ChatColor.GREEN + "Allies: " + String.join(", ", g.getAllies()));
            lore.add(ChatColor.RED + "Hostile: " + String.join(", ", g.getHostiles()));
            ItemStack head = HeadUtil.createPlayerHead(lp, ChatColor.GOLD + g.getName(), lore);
            inv.addItem(head);
        }
        player.openInventory(inv);
    }

}
