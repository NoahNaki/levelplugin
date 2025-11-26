package me.nakilex.levelplugin.mercenary.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.mercenary.FriendshipMilestone;
import me.nakilex.levelplugin.mercenary.MercenaryAffinityManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TextUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Displays total friendship milestones and allows players to claim collection rewards.
 */
public class FriendshipMilestoneGUI implements Listener {

    private static final int SIZE = 45;
    private static final int[] MILESTONE_SLOTS = {10, 12, 14, 16, 22};
    private static final int SUMMARY_SLOT = 4;

    private final MercenaryAffinityManager affinityManager;
    private final Map<UUID, Map<Integer, FriendshipMilestone>> openMenus = new HashMap<>();

    public FriendshipMilestoneGUI(MercenaryAffinityManager affinityManager) {
        this.affinityManager = affinityManager;
        Main.getInstance().getServer().getPluginManager().registerEvents(this, Main.getInstance());
    }

    public void open(Player player) {
        Inventory inv = buildInventory(player);
        player.openInventory(inv);
    }

    private Inventory buildInventory(Player player) {
        GuiBuilder builder = GuiBuilder.create(SIZE, TextUtil.centerInventoryTitle(ChatColor.LIGHT_PURPLE + "Friendship Collection"))
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border();

        int total = affinityManager.getTotalFriendshipLevel(player.getUniqueId());
        builder.setItem(SUMMARY_SLOT, buildSummaryItem(total));

        Map<Integer, FriendshipMilestone> slotMap = new HashMap<>();
        List<FriendshipMilestone> milestones = affinityManager.getMilestones();
        for (int i = 0; i < milestones.size() && i < MILESTONE_SLOTS.length; i++) {
            FriendshipMilestone milestone = milestones.get(i);
            int slot = MILESTONE_SLOTS[i];
            builder.setItem(slot, buildMilestoneItem(player, milestone, total));
            slotMap.put(slot, milestone);
        }
        openMenus.put(player.getUniqueId(), slotMap);
        return builder.build();
    }

    private ItemStack buildSummaryItem(int total) {
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta meta = book.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Total Friendship");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Combined Levels: " + ChatColor.WHITE + total);
            FriendshipMilestone next = affinityManager.getMilestones().stream()
                    .filter(m -> m.requiredTotalLevel() > total)
                    .findFirst().orElse(null);
            if (next != null) {
                lore.add(ChatColor.GRAY + "Next Reward: " + ChatColor.GOLD + next.label());
                lore.add(ChatColor.DARK_GRAY + "Progress: " + TooltipUtil.progressBar(total, next.requiredTotalLevel(), 16));
                lore.add(ChatColor.DARK_GRAY + "• " + ChatColor.GRAY + total + ChatColor.DARK_GRAY + "/" + ChatColor.GRAY + next.requiredTotalLevel());
            } else {
                lore.add(ChatColor.GREEN + "All current milestones unlocked!");
            }
            meta.setLore(lore);
            book.setItemMeta(meta);
        }
        return book;
    }

    private ItemStack buildMilestoneItem(Player player, FriendshipMilestone milestone, int total) {
        boolean claimed = affinityManager.isMilestoneClaimed(player.getUniqueId(), milestone.requiredTotalLevel());
        boolean unlocked = total >= milestone.requiredTotalLevel();
        boolean claimable = unlocked && !claimed;

        ItemStack chest = GuiUtil.getNexoItem(claimed ? "check" : "reward", milestone.label());
        ItemMeta meta = chest.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Requirement: " + ChatColor.WHITE + "Total Lv " + milestone.requiredTotalLevel());
            lore.add(ChatColor.GRAY + "Reward:");
            lore.add(ChatColor.DARK_GRAY + "• " + ChatColor.GOLD + milestone.reward().coins() + ChatColor.GRAY + " coins");
            lore.add(ChatColor.DARK_GRAY + "• " + ChatColor.AQUA + milestone.reward().experience() + ChatColor.GRAY + " XP");
            if (!milestone.reward().items().isEmpty()) {
                lore.add(ChatColor.DARK_GRAY + "• " + ChatColor.GRAY + "Bonus items");
            }
            lore.add(" ");
            lore.addAll(milestone.flavor());
            lore.add(" ");
            if (claimed) {
                lore.add(ChatColor.GREEN + "Already claimed");
            } else if (claimable) {
                lore.add(ChatColor.GREEN + "Click to claim");
            } else {
                lore.add(ChatColor.RED + "Locked" + ChatColor.GRAY + " (" + total + "/" + milestone.requiredTotalLevel() + ")");
            }
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            chest.setItemMeta(meta);
        }
        return chest;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Map<Integer, FriendshipMilestone> slots = openMenus.get(player.getUniqueId());
        if (slots == null || slots.isEmpty()) {
            return;
        }
        if (!event.getView().getTitle().contains("Friendship Collection")) {
            return;
        }
        event.setCancelled(true);

        FriendshipMilestone milestone = slots.get(event.getRawSlot());
        if (milestone == null) {
            return;
        }
        if (!affinityManager.claimMilestone(player, milestone)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "You need more total friendship to claim this reward.");
            return;
        }
        // Refresh state so claim indicator updates immediately
        open(player);
    }
}
