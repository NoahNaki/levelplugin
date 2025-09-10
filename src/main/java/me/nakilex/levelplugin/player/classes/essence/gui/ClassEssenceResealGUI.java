package me.nakilex.levelplugin.player.classes.essence.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.classes.essence.ClassEssence;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
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

import java.util.ArrayList;
import java.util.List;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.send;

/**
 * GUI allowing players to reseal (unbind) soulbound essences using sealing charms.
 */
public class ClassEssenceResealGUI implements Listener {

    private static final String TITLE = ChatColor.BLACK + "Essence: Reseal";
    private static final int ESSENCE_SLOT = 11;
    private static final int CHARM_SLOT = 15;
    private static final int CONFIRM_SLOT = 22;

    public static void open(Player player) {
        Inventory gui = GuiBuilder.create(27, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .build();
        gui.setItem(ESSENCE_SLOT, null);
        gui.setItem(CHARM_SLOT, null);
        updateButton(gui);
        player.openInventory(gui);
    }

    private static void updateButton(Inventory gui) {
        ItemStack essence = gui.getItem(ESSENCE_SLOT);
        ItemStack charms = gui.getItem(CHARM_SLOT);
        ItemStack button;
        if (essence != null && ClassEssence.isEssence(essence) && ClassEssence.isSoulbound(essence)) {
            int cost = ClassEssence.getResealCost(essence);
            int have = ItemUtil.isSealingCharm(charms) ? charms.getAmount() : 0;
            boolean enough = have >= cost;
            button = GuiUtil.getNexoItem(enough ? "check" : "cross", (enough ? ChatColor.GREEN : ChatColor.RED) + "Reseal");
            ItemMeta meta = button.getItemMeta();
            if (meta != null) {
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Cost: " + ChatColor.AQUA + cost + " Sealing Charm" + (cost == 1 ? "" : "s"));
                lore.addAll(TooltipUtil.clickInstructions("to reseal", null));
                meta.setLore(lore);
                button.setItemMeta(meta);
            }
        } else {
            button = GuiUtil.getNexoItem("cross", ChatColor.RED + "Reseal");
            ItemMeta meta = button.getItemMeta();
            if (meta != null) {
                meta.setLore(List.of(ChatColor.GRAY + "Place a soulbound essence."));
                button.setItemMeta(meta);
            }
        }
        gui.setItem(CONFIRM_SLOT, button);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!TITLE.equals(e.getView().getTitle())) return;
        Inventory top = e.getView().getTopInventory();
        int slot = e.getRawSlot();
        Player player = (Player) e.getWhoClicked();

        if (e.getClickedInventory() != top) {
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> updateButton(top));
            return;
        }

        if (slot == ESSENCE_SLOT || slot == CHARM_SLOT) {
            e.setCancelled(false);
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> updateButton(top));
            return;
        }

        e.setCancelled(true);
        if (slot == CONFIRM_SLOT) {
            ItemStack essence = top.getItem(ESSENCE_SLOT);
            ItemStack charms = top.getItem(CHARM_SLOT);
            if (essence != null && ClassEssence.isEssence(essence) && ClassEssence.isSoulbound(essence)) {
                int cost = ClassEssence.getResealCost(essence);
                int have = ItemUtil.isSealingCharm(charms) ? charms.getAmount() : 0;
                if (have >= cost) {
                    if (have == cost) top.setItem(CHARM_SLOT, null);
                    else charms.setAmount(have - cost);
                    ClassEssence.setSoulbound(essence, false);
                    player.getInventory().addItem(essence);
                    top.setItem(ESSENCE_SLOT, null);
                    send(player, MessageType.SUCCESS, "Essence resealed.");
                } else {
                    send(player, MessageType.ERROR, "Not enough sealing charms.");
                }
            } else {
                send(player, MessageType.ERROR, "Place a soulbound essence.");
            }
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> updateButton(top));
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!TITLE.equals(e.getView().getTitle())) return;
        Inventory inv = e.getInventory();
        Player player = (Player) e.getPlayer();
        ItemStack essence = inv.getItem(ESSENCE_SLOT);
        ItemStack charms = inv.getItem(CHARM_SLOT);
        if (essence != null) player.getInventory().addItem(essence);
        if (charms != null) player.getInventory().addItem(charms);
    }
}
