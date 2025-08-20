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
    private static final int[] QUEST_SLOTS = {11, 13, 15};

    public static Inventory create(Player viewer, Map<String, GuildQuest> quests) {
        GuiBuilder builder = GuiBuilder.create(27, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border();

        for (int i = 0; i < QUEST_SLOTS.length; i++) {
            GuildQuest quest = quests.get(String.valueOf(i));
            if (quest == null) continue;
            int slot = QUEST_SLOTS[i];
            String tracked = GuildQuestManager.getInstance().getTrackedQuest(viewer.getUniqueId());
            String iconId = "pack1_scroll2";
            if (quest.isAccepted() && tracked != null && tracked.equals(quest.getId())) {
                iconId = "pack1_scroll4";
            }
            ItemStack icon = GuiUtil.getNexoItem(iconId, ChatColor.GOLD + quest.getName());
            ItemMeta meta = icon.getItemMeta();
            List<String> lore = new ArrayList<>();

            String desc = Main.getInstance().getQuestManager().describeObjective(quest.getObjective());
            int total = quest.getTotalContribution();
            int need = quest.getTargetAmount();
            lore.add(ChatColor.GRAY + desc);
            lore.add(ChatColor.GRAY + "Progress: " + ChatColor.YELLOW + total + ChatColor.GRAY + "/" + ChatColor.YELLOW + need);
            lore.add(TooltipUtil.progressBar(total, need, 10));

            lore.add(ChatColor.GRAY + "Difficulty: " + ChatColor.YELLOW + GuiUtil.glyphStars(quest.getStars()));

            lore.add(" ");
            lore.add(ChatColor.GREEN + "Guild Rewards:");
            String expLabel = ChatFormatter.experienceLabel();
            String expColor = ChatFormatter.experienceColor();
            lore.add(ChatColor.GREEN + "- " + expColor + quest.getGuildExpReward() + ChatColor.RESET + " <glyph:experience_orb_icon> " + expLabel);
            lore.add(ChatColor.GREEN + "- " + ChatColor.GRAY + quest.getGuildCoinReward() + " <glyph:coins_icon>");

            QuestReward pr = quest.getPersonalReward();
            if (pr != null && (pr.getXp() > 0 || pr.getCoins() > 0)) {
                lore.add(" ");
                lore.add(ChatColor.GREEN + "Personal Rewards:");
                if (pr.getXp() > 0) {
                    lore.add(ChatColor.GREEN + "- " + expColor + pr.getXp() + ChatColor.RESET + " <glyph:experience_orb_icon> " + expLabel);
                }
                if (pr.getCoins() > 0) {
                    lore.add(ChatColor.GREEN + "- " + ChatColor.GRAY + pr.getCoins() + " <glyph:coins_icon>");
                }
            }

            lore.add(" ");
            if (quest.isAccepted()) {
                if (tracked != null && tracked.equals(quest.getId())) {
                    lore.add(ChatColor.YELLOW + "Tracking");
                    lore.add(ChatColor.WHITE + "Left-click " + ChatColor.GRAY + "to untrack");
                } else {
                    lore.add(ChatColor.GREEN + "Accepted");
                    lore.add(ChatColor.WHITE + "Left-click " + ChatColor.GRAY + "to track");
                }
            } else {
                GuiUtil.addClickInstructions(lore, "to accept", quest.isRerolled() ? null : "to reroll");
                if (quest.isRerolled()) {
                    lore.add(ChatColor.RED + "Reroll used");
                }
            }

            meta.setLore(lore);
            icon.setItemMeta(meta);
            builder.setItem(slot, icon);
        }

        return builder.build();
    }

    public static int indexFromSlot(int rawSlot) {
        for (int i = 0; i < QUEST_SLOTS.length; i++) {
            if (QUEST_SLOTS[i] == rawSlot) return i;
        }
        return -1;
    }

    public static int slotFromIndex(int index) {
        return QUEST_SLOTS[index];
    }
}
