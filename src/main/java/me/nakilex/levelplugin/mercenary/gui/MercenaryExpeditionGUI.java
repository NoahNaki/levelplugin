package me.nakilex.levelplugin.mercenary.gui;

import me.nakilex.levelplugin.mercenary.ExpeditionDefinition;
import me.nakilex.levelplugin.mercenary.MercenaryAffinityManager;
import me.nakilex.levelplugin.mercenary.MercenaryExpeditionManager;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Locale;
import java.util.List;

/**
 * Primary UI for sending unlocked mercenaries to dungeon expeditions. The GUI
 * is intentionally compact and uses the shared tooltip utilities for
 * consistent styling.
 */
public class MercenaryExpeditionGUI implements Listener {
    private static final int SIZE = 45;
    private static final String TITLE = ChatColor.DARK_GREEN + "Mercenary Expeditions";

    private final Plugin plugin;
    private final MercenaryAffinityManager affinityManager;
    private final MercenaryExpeditionManager expeditionManager;
    private final java.util.Map<java.util.UUID, Integer> currentNpc = new java.util.HashMap<>();

    public MercenaryExpeditionGUI(Plugin plugin, MercenaryAffinityManager affinityManager,
                                  MercenaryExpeditionManager expeditionManager) {
        this.plugin = plugin;
        this.affinityManager = affinityManager;
        this.expeditionManager = expeditionManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player, int npcId) {
        GuiBuilder builder = GuiBuilder.create(SIZE, TITLE).border();
        currentNpc.put(player.getUniqueId(), npcId);
        int friendship = affinityManager.getFriendship(player.getUniqueId(), npcId).getLevel();
        int slot = 10;
        for (ExpeditionDefinition definition : expeditionManager.getExpeditions()) {
            ItemStack item = new ItemStack(Material.MAP);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + definition.displayName());
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Threat: " + ChatColor.RED + definition.threat());
                int gs = affinityManager.getGearScore(npcId);
                double success = expeditionManager.successChance(gs, definition.threat());
                int seconds = expeditionManager.adjustedDuration(gs, definition.threat(),
                        definition.baseDurationSeconds(), friendship);
                int reward = expeditionManager.rewardFor(definition.threat(), friendship);
                lore.add(ChatColor.GRAY + "Est. Duration: " + ChatColor.AQUA + seconds / 60 + "m");
                lore.add(ChatColor.GRAY + "Mercenary GS: " + ChatColor.GREEN + gs);
                lore.add(ChatColor.WHITE + TooltipUtil.progressBar(Math.min(gs, definition.threat()),
                        Math.max(definition.threat(), 1), 12));
                lore.add(ChatColor.GRAY + "Success Chance: " + ChatColor.GREEN + String.format(Locale.ENGLISH, "%.1f%%", success));
                lore.add(ChatColor.GRAY + "Reward: " + ChatColor.GOLD + reward + " coins");
                lore.add(" ");
                lore.add(ChatColor.YELLOW + "Left-click to send mercenary");
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            builder.setItem(slot, item);
            if ((slot + 1) % 9 == 8) {
                slot += 3;
            } else {
                slot++;
            }
        }
        player.openInventory(builder.build());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!TITLE.equals(event.getView().getTitle())) {
            return;
        }
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.MAP) {
            return;
        }
        String display = clicked.getItemMeta() != null ? ChatColor.stripColor(clicked.getItemMeta().getDisplayName()) : "";
        ExpeditionDefinition definition = expeditionManager.getExpeditions().stream()
                .filter(def -> ChatColor.stripColor(def.displayName()).equals(display))
                .findFirst()
                .orElse(null);
        if (definition == null) {
            return;
        }
        Integer npcId = currentNpc.get(player.getUniqueId());
        if (npcId == null) {
            player.sendMessage(ChatColor.RED + "Reopen the expedition menu to choose a mercenary.");
            return;
        }
        expeditionManager.startExpedition(player, npcId, definition);
        player.closeInventory();
    }
}
