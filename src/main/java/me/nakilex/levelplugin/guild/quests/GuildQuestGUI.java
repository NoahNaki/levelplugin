package me.nakilex.levelplugin.guild.quests;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.quests.data.QuestReward;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Basic GUI showing a set of guild quests.  This class demonstrates how
 * {@link GuiBuilder} and {@link TooltipUtil} can be combined to quickly build a
 * consistent user interface.
 */
public final class GuildQuestGUI {

    private GuildQuestGUI() {}

    public static final String TITLE = "Guild Quests";
    private static final int[] QUEST_SLOTS = {11, 13, 15};

    public static Inventory create(Player viewer, Map<String, GuildQuest> quests) {
        GuiBuilder builder = GuiBuilder.create(27, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border();
        Inventory inventory = builder.build();
        renderWidgets(inventory, viewer, quests);
        return inventory;
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

    public static boolean handleWidgetClick(InventoryClickEvent event, Player player, Map<String, GuildQuest> quests) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return false;
        }
        List<GuiWidget> widgets = buildWidgets(player, quests);
        Optional<GuiWidget> match = widgets.stream()
                .filter(widget -> widget.handlesSlot(slot))
                .findFirst();
        if (match.isEmpty()) {
            return false;
        }
        match.get().onClick(slot, event.getClick(), new GuiContext(player, event.getView().getTopInventory()));
        return true;
    }

    private static void renderWidgets(Inventory inventory, Player viewer, Map<String, GuildQuest> quests) {
        GuiLayout layout = new GuiLayout(inventory);
        GuiContext context = new GuiContext(viewer, inventory);
        for (GuiWidget widget : buildWidgets(viewer, quests)) {
            widget.contribute(layout, context);
        }
    }

    private static List<GuiWidget> buildWidgets(Player viewer, Map<String, GuildQuest> quests) {
        List<GuiWidget> widgets = new ArrayList<>();
        for (int i = 0; i < QUEST_SLOTS.length; i++) {
            int slot = QUEST_SLOTS[i];
            String key = String.valueOf(i);
            widgets.add(new ActionWidget(slot,
                    context -> createQuestItem(context.player(), quests.get(key)),
                    (click, context) -> handleQuestClick(context.player(), click, key)));
        }
        return widgets;
    }

    private static ItemStack createQuestItem(Player viewer, GuildQuest quest) {
        if (quest == null) {
            return null;
        }
        String tracked = GuildQuestManager.getInstance().getTrackedQuest(viewer.getUniqueId());
        String iconId;
        String name;
        if (quest.isCompleted()) {
            iconId = "check";
            name = ChatColor.DARK_GREEN + quest.getName();
        } else {
            iconId = "pack1_scroll2";
            if (quest.isAccepted() && tracked != null && tracked.equals(quest.getId())) {
                iconId = "pack1_scroll4";
            }
            name = ChatColor.GOLD + quest.getName();
        }
        ItemStack icon = GuiUtil.getNexoItem(iconId, name);
        ItemMeta meta = icon.getItemMeta();
        List<String> lore = new ArrayList<>();

        String desc = Main.getInstance().getQuestManager().describeObjective(quest.getObjective());
        int total = quest.getTotalContribution();
        int need = quest.getTargetAmount();
        lore.add(ChatColor.GRAY + desc);
        if (!quest.isCompleted()) {
            lore.add(ChatColor.GRAY + "Progress: " + ChatColor.YELLOW + total + ChatColor.GRAY + "/" + ChatColor.YELLOW + need);
            lore.add(TooltipUtil.progressBar(total, need, 10));
            lore.add(ChatColor.GRAY + "Difficulty: " + ChatColor.YELLOW + GuiUtil.glyphStars(quest.getStars()));
            lore.add(" ");
        } else {
            lore.add(" ");
        }
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
        if (quest.isCompleted()) {
            lore.add(ChatColor.GREEN + "Completed");
        } else if (quest.isAccepted()) {
            if (tracked != null && tracked.equals(quest.getId())) {
                lore.add(ChatColor.YELLOW + "Tracking");
                lore.addAll(TooltipUtil.clickInstructions("to untrack", null));
            } else {
                lore.add(ChatColor.GREEN + "Accepted");
                lore.addAll(TooltipUtil.clickInstructions("to track", null));
            }
        } else {
            lore.addAll(TooltipUtil.clickInstructions("to accept", quest.isRerolled() ? null : "to reroll"));
            if (quest.isRerolled()) {
                lore.add(ChatColor.RED + "Reroll used");
            }
        }

        if (meta != null) {
            meta.setLore(lore);
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private static void handleQuestClick(Player player, ClickType click, String key) {
        var guild = me.nakilex.levelplugin.guild.GuildManager.getInstance().getGuild(player.getUniqueId());
        if (guild == null) {
            return;
        }
        GuildQuest quest = guild.getQuests().get(key);
        if (quest == null || quest.isCompleted()) {
            return;
        }

        if (click == ClickType.LEFT) {
            if (!quest.isAccepted()) {
                quest.setAccepted(true);
                player.sendMessage(ChatColor.GREEN + "Accepted guild quest: " + quest.getName());
            } else {
                boolean tracked = GuildQuestManager.getInstance().toggleTracking(player, quest);
                if (tracked) {
                    player.sendMessage(ChatColor.GREEN + "Tracking guild quest: " + quest.getName());
                } else {
                    player.sendMessage(ChatColor.YELLOW + "Stopped tracking guild quest: " + quest.getName());
                }
            }
        } else if (click == ClickType.RIGHT) {
            if (!quest.isAccepted() && !quest.isRerolled()) {
                GuildQuestManager.getInstance().rerollQuest(guild, key);
                player.sendMessage(ChatColor.YELLOW + "Guild quest rerolled.");
            }
        }

        player.openInventory(GuildQuestGUI.create(player, guild.getQuests()));
    }
}
