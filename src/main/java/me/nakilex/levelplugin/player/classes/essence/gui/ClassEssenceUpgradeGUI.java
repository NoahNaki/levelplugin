package me.nakilex.levelplugin.player.classes.essence.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.player.classes.essence.ClassEssence;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TextUtil;
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

import java.util.*;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.send;

/**
 * Blacksmith-style GUI for investing duplicate essences and upgrading stars.
 */
public class ClassEssenceUpgradeGUI implements Listener {

    private static final String INVEST_TITLE = ChatColor.BLACK + "Essence: Invest";
    private static final String STAR_TITLE = ChatColor.BLACK + "Essence: Star Upgrade";

    private static final int SACRIFICE_SLOT = 11;
    private static final int TARGET_SLOT = 15;
    private static final int STAR_SLOT = 13;
    private static final int CONFIRM_SLOT = 22;
    private static final int LEFT_ARROW_SLOT = 9;
    private static final int RIGHT_ARROW_SLOT = 17;

    private static final Set<UUID> SWITCHING = new HashSet<>();

    public static void openInvest(Player player, ItemStack target) {
        Inventory gui = GuiBuilder.create(27, INVEST_TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .build();
        gui.setItem(LEFT_ARROW_SLOT, GuiUtil.getNexoItem("arrow_left", ChatColor.GRAY + "Star Upgrade"));
        gui.setItem(RIGHT_ARROW_SLOT, GuiUtil.getNexoItem("arrow_right", ChatColor.GRAY + "Star Upgrade"));
        gui.setItem(SACRIFICE_SLOT, null);
        if (target != null) gui.setItem(TARGET_SLOT, target);
        else gui.setItem(TARGET_SLOT, null);
        gui.setItem(CONFIRM_SLOT, GuiUtil.getNexoItem("check", ChatColor.GREEN + "Invest"));
        player.openInventory(gui);
    }

    public static void openStar(Player player, ItemStack target) {
        Inventory gui = GuiBuilder.create(27, STAR_TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .build();
        gui.setItem(LEFT_ARROW_SLOT, GuiUtil.getNexoItem("arrow_left", ChatColor.GRAY + "Invest"));
        gui.setItem(RIGHT_ARROW_SLOT, GuiUtil.getNexoItem("arrow_right", ChatColor.GRAY + "Invest"));
        if (target != null) gui.setItem(STAR_SLOT, target);
        else gui.setItem(STAR_SLOT, null);
        updateStarButton(gui);
        player.openInventory(gui);
    }

    private static void updateStarButton(Inventory gui) {
        ItemStack essence = gui.getItem(STAR_SLOT);
        ItemStack button;
        if (essence != null && ClassEssence.isEssence(essence)) {
            int star = ClassEssence.getStar(essence);
            if (star >= 5) {
                button = GuiUtil.getNexoItem("cross", ChatColor.RED + "Max Star");
            } else {
                int cost = (star + 1) * 1000;
                int[] chances = {33, 15, 10, 5, 2};
                int chance = chances[Math.min(star, chances.length - 1)];
                button = GuiUtil.getNexoItem("check", ChatColor.GREEN + "Upgrade");
                ItemMeta meta = button.getItemMeta();
                if (meta != null) {
                    meta.setLore(Arrays.asList(
                            ChatColor.GRAY + "Cost: " + ChatColor.GOLD + "<glyph:coins_icon> " + cost,
                            ChatColor.GRAY + "Success Chance: " + ChatColor.GOLD + chance + "%"
                    ));
                    button.setItemMeta(meta);
                }
            }
        } else {
            button = GuiUtil.getNexoItem("check", ChatColor.GREEN + "Upgrade");
            ItemMeta meta = button.getItemMeta();
            if (meta != null) {
                meta.setLore(List.of(ChatColor.GRAY + "Place an essence to upgrade."));
                button.setItemMeta(meta);
            }
        }
        gui.setItem(CONFIRM_SLOT, button);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (!INVEST_TITLE.equals(title) && !STAR_TITLE.equals(title)) return;
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;

        e.setCancelled(true);
        Player player = (Player) e.getWhoClicked();
        Inventory inv = e.getInventory();
        int slot = e.getRawSlot();

        if (slot == LEFT_ARROW_SLOT || slot == RIGHT_ARROW_SLOT) {
            ItemStack carry = INVEST_TITLE.equals(title) ? inv.getItem(TARGET_SLOT) : inv.getItem(STAR_SLOT);
            if (INVEST_TITLE.equals(title)) {
                ItemStack sacrifice = inv.getItem(SACRIFICE_SLOT);
                if (sacrifice != null) player.getInventory().addItem(sacrifice);
            }
            SWITCHING.add(player.getUniqueId());
            if (INVEST_TITLE.equals(title)) {
                openStar(player, carry);
            } else {
                openInvest(player, carry);
            }
            return;
        }

        if (INVEST_TITLE.equals(title)) {
            if (slot == SACRIFICE_SLOT || slot == TARGET_SLOT) {
                e.setCancelled(false);
                return;
            }
            if (slot == CONFIRM_SLOT) {
                ItemStack target = inv.getItem(TARGET_SLOT);
                ItemStack sacrifice = inv.getItem(SACRIFICE_SLOT);
                if (target != null && sacrifice != null &&
                        ClassEssence.isEssence(target) && ClassEssence.isEssence(sacrifice) &&
                        ClassEssence.getClass(target) == ClassEssence.getClass(sacrifice)) {
                    ItemRarity sacRarity = ClassEssence.getRarity(sacrifice);
                    int amount = ClassEssence.getInvestExp(sacRarity) + ClassEssence.getExp(sacrifice);
                    ItemRarity upgraded = ClassEssence.addExp(target, amount);
                    inv.setItem(TARGET_SLOT, target);
                    inv.setItem(SACRIFICE_SLOT, null);
                    send(player, MessageType.SUCCESS, "Invested essence (+" + amount + " EXP)");
                    if (upgraded != null) {
                        send(player, MessageType.SUCCESS, "Essence rarity increased to " + upgraded.getColor() + TextUtil.beautifyWords(upgraded.name()));
                    }
                } else {
                    send(player, MessageType.ERROR, "Essences must match class.");
                }
            }
            return;
        }

        if (STAR_TITLE.equals(title)) {
            if (slot == STAR_SLOT) {
                e.setCancelled(false);
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> updateStarButton(inv));
                return;
            }
            if (slot == CONFIRM_SLOT) {
                ItemStack essence = inv.getItem(STAR_SLOT);
                if (essence != null && ClassEssence.isEssence(essence)) {
                    int star = ClassEssence.getStar(essence);
                    if (star < 5) {
                        int cost = (star + 1) * 1000;
                        int[] chances = {33, 15, 10, 5, 2};
                        int chance = chances[Math.min(star, chances.length - 1)];
                        EconomyManager econ = Main.getInstance().getEconomyManager();
                        if (econ.getBalance(player) >= cost) {
                            econ.deductCoins(player, cost);
                            if (new Random().nextInt(100) < chance) {
                                ClassEssence.upgradeStar(essence);
                                send(player, MessageType.SUCCESS, "Star upgrade succeeded!");
                            } else {
                                send(player, MessageType.ERROR, "Star upgrade failed!");
                            }
                            inv.setItem(STAR_SLOT, essence);
                            updateStarButton(inv);
                        } else {
                            send(player, MessageType.ERROR, "You need " + cost + " coins.");
                        }
                    } else {
                        send(player, MessageType.ERROR, "Essence is already max star.");
                    }
                } else {
                    send(player, MessageType.ERROR, "Place an essence to upgrade.");
                }
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        String title = e.getView().getTitle();
        Player player = (Player) e.getPlayer();
        if (SWITCHING.remove(player.getUniqueId())) return;

        Inventory inv = e.getInventory();
        if (INVEST_TITLE.equals(title)) {
            ItemStack target = inv.getItem(TARGET_SLOT);
            ItemStack sacrifice = inv.getItem(SACRIFICE_SLOT);
            if (target != null) player.getInventory().addItem(target);
            if (sacrifice != null) player.getInventory().addItem(sacrifice);
        } else if (STAR_TITLE.equals(title)) {
            ItemStack target = inv.getItem(STAR_SLOT);
            if (target != null) player.getInventory().addItem(target);
        }
    }
}
