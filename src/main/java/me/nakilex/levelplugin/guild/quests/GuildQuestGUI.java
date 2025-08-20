package me.nakilex.levelplugin.guild.quests;

import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Basic GUI showing a set of guild quests.  This class demonstrates how
 * {@link GuiBuilder} and {@link TooltipUtil} can be combined to quickly build a
 * consistent user interface.
 */
public final class GuildQuestGUI {

    private GuildQuestGUI() {}

    public static Inventory create(Player viewer, Map<String, GuildQuest> quests) {
        GuiBuilder builder = GuiBuilder.create(27, ChatColor.BLACK + "Guild Quests")
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border();

        int slot = 10;
        for (GuildQuest quest : quests.values()) {
            ItemStack icon = GuiUtil.getNexoItem("book", ChatColor.GOLD + quest.getName());
            ItemMeta meta = icon.getItemMeta();
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Difficulty: " + GuiUtil.generateStars(quest.getStars(), 5));
            lore.add(ChatColor.GRAY + "Total Contributions: " + ChatColor.YELLOW + quest.getTotalContribution());
            lore.add(TooltipUtil.progressBar(quest.getTotalContribution(), quest.getRewardTiers().size(), 10));
            meta.setLore(lore);
            icon.setItemMeta(meta);
            builder.setItem(slot, icon);
            slot++;
            if (slot % 9 == 8) slot += 2; // move to next row skipping borders
        }

        return builder.build();
    }
}
