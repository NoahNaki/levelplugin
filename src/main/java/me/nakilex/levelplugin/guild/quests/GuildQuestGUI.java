package me.nakilex.levelplugin.guild.quests;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.quests.data.QuestReward;
import me.nakilex.levelplugin.utils.ChatFormatter;
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

    public static final String TITLE = ChatColor.BLACK + "Guild Quests";

    public static Inventory create(Player viewer, Map<String, GuildQuest> quests) {
        GuiBuilder builder = GuiBuilder.create(27, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border();

        int slot = 10;
        for (GuildQuest quest : quests.values()) {
            ItemStack icon = GuiUtil.getNexoItem("pack1_scroll2", ChatColor.GOLD + quest.getName());
            ItemMeta meta = icon.getItemMeta();
            List<String> lore = new ArrayList<>();

            // Objective and progress
            String desc = Main.getInstance().getQuestManager().describeObjective(quest.getObjective());
            int total = quest.getTotalContribution();
            int need = quest.getTargetAmount();
            lore.add(ChatColor.GRAY + desc);
            lore.add(ChatColor.GRAY + "Progress: " + ChatColor.YELLOW + total + ChatColor.GRAY + "/" + ChatColor.YELLOW + need);
            lore.add(TooltipUtil.progressBar(total, need, 10));

            // Difficulty
            lore.add(ChatColor.GRAY + "Difficulty: " + GuiUtil.generateStars(quest.getStars(), 3));

            // Rewards
            lore.add(" ");
            lore.add(ChatColor.GOLD + "Guild Rewards:");
            String expLabel = ChatFormatter.experienceLabel();
            String expColor = ChatFormatter.experienceColor();
            lore.add(ChatColor.YELLOW + "- " + expColor + quest.getGuildExpReward() + ChatColor.RESET + " " + expLabel);
            lore.add(ChatColor.YELLOW + "- " + quest.getGuildCoinReward() + " coins");

            QuestReward pr = quest.getPersonalReward();
            if (pr != null && (pr.getXp() > 0 || pr.getCoins() > 0)) {
                lore.add(" ");
                lore.add(ChatColor.GOLD + "Personal Rewards:");
                if (pr.getXp() > 0) {
                    lore.add(ChatColor.YELLOW + "- " + expColor + pr.getXp() + ChatColor.RESET + " " + expLabel);
                }
                if (pr.getCoins() > 0) {
                    lore.add(ChatColor.YELLOW + "- " + pr.getCoins() + " coins");
                }
            }

            meta.setLore(lore);
            icon.setItemMeta(meta);
            builder.setItem(slot, icon);
            slot++;
            if (slot % 9 == 8) slot += 2; // move to next row skipping borders
        }

        return builder.build();
    }
}
