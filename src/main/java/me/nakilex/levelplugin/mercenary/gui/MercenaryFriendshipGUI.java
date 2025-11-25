package me.nakilex.levelplugin.mercenary.gui;

import me.nakilex.levelplugin.mercenary.MercenaryAffinityManager;
import me.nakilex.levelplugin.mercenary.MercenaryFriendship;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/** Displays friendship progress for a specific mercenary NPC. */
public class MercenaryFriendshipGUI implements Listener {
    private static final int SIZE = 27;

    private final Plugin plugin;
    private final MercenaryAffinityManager affinityManager;

    public MercenaryFriendshipGUI(Plugin plugin, MercenaryAffinityManager affinityManager) {
        this.plugin = plugin;
        this.affinityManager = affinityManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player, int npcId, String npcName) {
        String title = ChatColor.DARK_GREEN + npcName + ChatColor.GRAY + " Affinity";
        GuiBuilder builder = GuiBuilder.create(SIZE, title).border();
        MercenaryFriendship friendship = affinityManager.getFriendship(player.getUniqueId(), npcId);

        ItemStack portrait = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = portrait.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + npcName);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Friendship Level: " + ChatColor.GREEN + friendship.getLevel());
            int current = friendship.getPoints();
            int nextThreshold = affinityManager.thresholdForLevel(Math.min(5, friendship.getLevel() + 1));
            lore.add(ChatColor.GRAY + "Progress: " + TooltipUtil.progressBar(current, Math.max(nextThreshold, 1), 12));
            lore.add(ChatColor.DARK_GRAY + "• " + ChatColor.GRAY + current + ChatColor.DARK_GRAY + "/" + ChatColor.GRAY + nextThreshold);
            meta.setLore(lore);
            portrait.setItemMeta(meta);
        }
        builder.setItem(11, portrait);

        ItemStack benefits = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta bMeta = benefits.getItemMeta();
        if (bMeta != null) {
            bMeta.setDisplayName(ChatColor.AQUA + "Level Benefits");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.WHITE + "Current perks:");
            lore.addAll(affinityManager.getBenefits(friendship.getLevel()));
            lore.add(" ");
            lore.add(ChatColor.GRAY + "Higher levels unlock new perks.");
            bMeta.setLore(lore);
            benefits.setItemMeta(bMeta);
        }
        builder.setItem(15, benefits);

        player.openInventory(builder.build());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!event.getView().getTitle().contains("Affinity")) {
            return;
        }
        event.setCancelled(true);
        if (event.getCurrentItem() != null) {
            player.sendMessage(ChatColor.GRAY + "Interact with gifts to raise friendship.");
        }
    }
}
