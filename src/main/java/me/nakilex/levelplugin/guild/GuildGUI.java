package me.nakilex.levelplugin.guild;

import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.HeadUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class GuildGUI {

    private final GuildManager manager;
    private static final int SIZE = 54;
    private static final String TITLE = "Guilds";

    public GuildGUI(GuildManager manager) {
        this.manager = manager;
    }

    public void open(Player player) {
        Inventory inv = GuiBuilder.create(SIZE, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .fillEmptySlots(false)
                .border()
                .build();
        boolean noGuild = manager.getGuild(player.getUniqueId()) == null;
        for (Guild g : manager.getGuilds()) {
            OfflinePlayer lp = Bukkit.getOfflinePlayer(g.getLeader());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "Leader: " + g.getLeaderName());
            lore.add(ChatColor.WHITE + "Members: " + g.getMembers().size());
            lore.add(ChatColor.GRAY + "Level: " + ChatColor.YELLOW + g.getLevel());
            int need = g.getExpNeeded();
            if (need > 0) {
                lore.add(ChatColor.GRAY + "XP: " + ChatColor.YELLOW + g.getExp() + ChatColor.GRAY + "/" + ChatColor.YELLOW + need);
            }
            lore.add(ChatColor.GREEN + "Allies: " + String.join(", ", g.getAllies()));
            lore.add(ChatColor.RED + "Hostile: " + String.join(", ", g.getHostiles()));
            if (noGuild) {
                if (g.getApplicants().containsKey(player.getUniqueId())) {
                    lore.add(ChatColor.GRAY + "Status: " + ChatColor.YELLOW + "Pending");
                } else {
                    lore.addAll(TooltipUtil.clickInstructions("to apply", null));
                }
            }
            ItemStack head = HeadUtil.createPlayerHead(lp, ChatColor.GOLD + g.getName(), lore);
            inv.addItem(head);
        }
        player.openInventory(inv);
    }

}
