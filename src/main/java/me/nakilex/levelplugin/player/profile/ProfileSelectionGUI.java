package me.nakilex.levelplugin.player.profile;

import me.nakilex.levelplugin.utils.GuiUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Simple GUI for selecting or creating character profiles.
 */
public class ProfileSelectionGUI implements Listener {
    private static final String TITLE = ChatColor.DARK_GREEN + "Select Profile";
    private static final int SIZE = 27;
    private static final int[] PROFILE_SLOTS = {10, 12, 14, 16};
    private static final ItemStack FILLER = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);

    private static final Map<UUID, Inventory> OPEN = new HashMap<>();

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        for (int i = 0; i < SIZE; i++) inv.setItem(i, FILLER);

        ProfileManager pm = ProfileManager.getInstance();
        int unlocked = pm.getUnlockedSlots(player.getUniqueId());
        List<PlayerProfile> list = pm.getProfiles(player.getUniqueId());

        for (int i = 0; i < PROFILE_SLOTS.length; i++) {
            int slot = PROFILE_SLOTS[i];
            if (i >= unlocked) {
                inv.setItem(slot, GuiUtil.getNexoItem("lock", ChatColor.RED + "Locked"));
                continue;
            }
            PlayerProfile prof = list.get(i);
            if (prof == null) {
                ItemStack star = new ItemStack(Material.FIREWORK_STAR);
                ItemMeta meta = star.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.GREEN + "[+] Create character");
                    star.setItemMeta(meta);
                }
                inv.setItem(slot, star);
            } else {
                inv.setItem(slot, createProfileItem(prof));
            }
        }

        OPEN.put(player.getUniqueId(), inv);
        player.openInventory(inv);
    }

    private static ItemStack createProfileItem(PlayerProfile profile) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + profile.getName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Level: " + ChatColor.WHITE + "1");
            lore.add(ChatColor.GRAY + "XP: " + ChatColor.WHITE + "0%");
            lore.add(ChatColor.GRAY + "Class: " + ChatColor.WHITE + "None");
            lore.add(ChatColor.GRAY + "Finished Quests: " + ChatColor.WHITE + "0/0");
            lore.add(ChatColor.GRAY + "Playtime: " + ChatColor.WHITE + "0m");
            lore.add(ChatColor.WHITE + "Click " + ChatColor.GRAY + "to select this character");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        Inventory open = OPEN.get(player.getUniqueId());
        if (open == null || !e.getView().getTopInventory().equals(open)) return;
        e.setCancelled(true);
        for (int i = 0; i < PROFILE_SLOTS.length; i++) {
            if (e.getRawSlot() == PROFILE_SLOTS[i]) {
                handleSelect(player, i);
                return;
            }
        }
    }

    private void handleSelect(Player player, int index) {
        ProfileManager pm = ProfileManager.getInstance();
        if (index >= pm.getUnlockedSlots(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "This profile is locked.");
            return;
        }
        PlayerProfile prof = pm.getProfile(player.getUniqueId(), index);
        if (prof == null) {
            prof = pm.createProfile(player.getUniqueId(), index);
            player.sendMessage(ChatColor.YELLOW + "Created new character " + prof.getName());
        } else {
            player.sendMessage(ChatColor.YELLOW + "Selected character " + prof.getName());
        }
        player.closeInventory();
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Inventory open = OPEN.get(e.getPlayer().getUniqueId());
        if (open != null && e.getInventory().equals(open)) {
            OPEN.remove(e.getPlayer().getUniqueId());
        }
    }
}
