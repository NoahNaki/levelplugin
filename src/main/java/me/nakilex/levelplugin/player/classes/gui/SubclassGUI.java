package me.nakilex.levelplugin.player.classes.gui;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class SubclassGUI implements Listener {
    private static final PlayerClass[] CLASSES = {
            PlayerClass.WARRIOR,
            PlayerClass.BARBARIAN,
            PlayerClass.DRAGONIAN,
            PlayerClass.GALEGLAIVE,
            PlayerClass.DEATHKNIGHT,
            PlayerClass.ARCTICKNIGHT,
            PlayerClass.DRAGONWARRIOR
    };
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16};

    private static final ItemStack LOCK_ITEM_BASE;
    private static final ItemStack FILLER;

    static {
        ItemBuilder b = NexoItems.itemFromId("lock");
        LOCK_ITEM_BASE = b == null ? new ItemStack(Material.BARRIER) : b.build();
        ItemMeta meta = LOCK_ITEM_BASE.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Locked");
            LOCK_ITEM_BASE.setItemMeta(meta);
        }
        FILLER = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = FILLER.getItemMeta();
        if (fm != null) { fm.setDisplayName(" "); FILLER.setItemMeta(fm); }
    }

    private static final Map<UUID, Inventory> OPEN = new HashMap<>();

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_AQUA + "Select Subclass");
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, FILLER);
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        for (int i = 0; i < CLASSES.length && i < SLOTS.length; i++) {
            PlayerClass pc = CLASSES[i];
            boolean unlocked = ps.unlockedClasses.contains(pc) && ps.awakeningStage >= stageOf(pc);
            ItemStack it = unlocked ? createItem(pc) : createLockedItem(pc);
            inv.setItem(SLOTS[i], it);
        }
        player.openInventory(inv);
        OPEN.put(player.getUniqueId(), inv);
    }

    private static int stageOf(PlayerClass pc) {
        return switch (pc) {
            case BARBARIAN, DRAGONIAN -> 1;
            case GALEGLAIVE -> 2;
            case DEATHKNIGHT, ARCTICKNIGHT -> 3;
            case DRAGONWARRIOR -> 4;
            case WARRIOR -> 0;
            default -> 0;
        };
    }

    private static ItemStack createItem(PlayerClass pc) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + pc.name());
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createLockedItem(PlayerClass pc) {
        ItemStack item = LOCK_ITEM_BASE.clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + pc.name());
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
        int slot = e.getSlot();
        for (int i = 0; i < SLOTS.length && i < CLASSES.length; i++) {
            if (slot == SLOTS[i]) {
                PlayerClass pc = CLASSES[i];
                StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
                if (ps.unlockedClasses.contains(pc) && ps.awakeningStage >= stageOf(pc)) {
                    ps.playerClass = pc;
                    player.sendMessage(ChatColor.GREEN + "Subclass changed to " + pc.name());
                }
                player.closeInventory();
                break;
            }
        }
        OPEN.remove(player.getUniqueId());
    }
}
