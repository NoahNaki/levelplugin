package me.nakilex.levelplugin.listeners;

import me.nakilex.levelplugin.items.CustomItem;
import me.nakilex.levelplugin.items.ItemManager;
import me.nakilex.levelplugin.items.ItemUtil;
import me.nakilex.levelplugin.managers.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ArmorEquipListener implements Listener {

    private static final Map<UUID, Set<Integer>> equippedArmors = new HashMap<>();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        Bukkit.getLogger().info("[ArmorEquipListener] InventoryClickEvent fired. Slot="
            + event.getSlot() + ", SlotType=" + event.getSlotType()
            + ", Click=" + event.getClick().name());

        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            ItemStack oldArmor = event.getCurrentItem();
            ItemStack newArmor = event.getCursor();

            int oldId = ItemUtil.getCustomItemId(oldArmor);
            int newId = ItemUtil.getCustomItemId(newArmor);

            Bukkit.getLogger().info("[ArmorEquipListener] Player=" + player.getName()
                + ", oldID=" + oldId + ", newID=" + newId);

            // Remove old armor if it was equipped
            if (oldId != -1) {
                CustomItem oldItem = ItemManager.getInstance().getItemById(oldId);
                if (oldItem != null && isArmorMaterial(oldItem.getMaterial())) {
                    Set<Integer> eqSet = equippedArmors.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
                    if (eqSet.contains(oldId)) {
                        Bukkit.getLogger().info("[ArmorEquipListener] Removing old armor stats: " + oldItem.getName());
                        removeItemStats(player, oldItem);
                        eqSet.remove(oldId);
                    }
                }
            }

            // Attempt to equip new armor
            if (newId != -1) {
                CustomItem newItem = ItemManager.getInstance().getItemById(newId);
                if (newItem != null && isArmorMaterial(newItem.getMaterial())) {
                    int playerLevel = StatsManager.getInstance().getLevel(player);
                    if (playerLevel < newItem.getLevelRequirement()) {
                        event.setCancelled(true);
                        player.sendMessage("§cYou are not high enough level to equip this item!");
                        return;
                    }
                    applyItemStats(player, newItem);
                    equippedArmors.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(newId);
                }
            }
        }
        if (event.isShiftClick()) {
            Bukkit.getLogger().info("[ArmorEquipListener] Shift-click detected. Slot=" + event.getSlot());

            ItemStack shiftedItem = event.getCurrentItem();
            if (shiftedItem == null || shiftedItem.getType() == Material.AIR) return;

            int itemId = ItemUtil.getCustomItemId(shiftedItem);
            if (itemId == -1) return;

            CustomItem cItem = ItemManager.getInstance().getItemById(itemId);

            if (cItem != null && isArmorMaterial(cItem.getMaterial())) {
                int playerLevel = StatsManager.getInstance().getLevel(player);
                if (playerLevel < cItem.getLevelRequirement()) {
                    event.setCancelled(true);
                    player.sendMessage("§cYou are not high enough level to equip this armor (via shift-click)!");
                    return;
                }

                // Determine where the item will go
                EntityEquipment equipment = player.getEquipment();
                if (cItem.getMaterial().name().endsWith("_HELMET") && equipment.getHelmet() == null) {
                    equipment.setHelmet(shiftedItem);
                    applyItemStats(player, cItem);
                    equippedArmors.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(itemId);
                    player.getInventory().setItem(event.getSlot(), null); // Remove from current slot
                    event.setCancelled(true);
                } else if (cItem.getMaterial().name().endsWith("_CHESTPLATE") && equipment.getChestplate() == null) {
                    equipment.setChestplate(shiftedItem);
                    applyItemStats(player, cItem);
                    equippedArmors.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(itemId);
                    player.getInventory().setItem(event.getSlot(), null);
                    event.setCancelled(true);
                } else if (cItem.getMaterial().name().endsWith("_LEGGINGS") && equipment.getLeggings() == null) {
                    equipment.setLeggings(shiftedItem);
                    applyItemStats(player, cItem);
                    equippedArmors.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(itemId);
                    player.getInventory().setItem(event.getSlot(), null);
                    event.setCancelled(true);
                } else if (cItem.getMaterial().name().endsWith("_BOOTS") && equipment.getBoots() == null) {
                    equipment.setBoots(shiftedItem);
                    applyItemStats(player, cItem);
                    equippedArmors.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(itemId);
                    player.getInventory().setItem(event.getSlot(), null);
                    event.setCancelled(true);
                } else {
                    Bukkit.getLogger().info("[ArmorEquipListener] Shift-click: Armor slot already occupied.");
                }
            }
        }
    }

    @EventHandler
    public void onArmorRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack clickedItem = event.getItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        int itemId = ItemUtil.getCustomItemId(clickedItem);
        if (itemId == -1) return;

        CustomItem cItem = ItemManager.getInstance().getItemById(itemId);
        if (cItem == null) return;

        if (isArmorMaterial(cItem.getMaterial())) {
            int playerLevel = StatsManager.getInstance().getLevel(player);
            Bukkit.getLogger().info("[ArmorEquipListener] " + player.getName()
                + " right-clicked armor " + cItem.getName()
                + ", ReqLv=" + cItem.getLevelRequirement()
                + ", PlayerLv=" + playerLevel);

            if (playerLevel < cItem.getLevelRequirement()) {
                event.setCancelled(true);
                player.sendMessage("§cYou are not high enough level to equip this armor!");
                return;
            }
            // If they meet the level requirement, let's forcibly equip it
            // Put item in the appropriate armor slot
            equipArmor(player, clickedItem, cItem);
            // Remove from hotbar
            event.getPlayer().getInventory().setItemInMainHand(null);
            event.setCancelled(true);
        }
    }

    private void equipArmor(Player player, ItemStack armorStack, CustomItem cItem) {
        // We decide which slot: If it's a chestplate material, equip in chest slot, etc.
        Material mat = cItem.getMaterial();
        EntityEquipment eq = player.getEquipment();

        if (mat.name().endsWith("_HELMET")) {
            if (eq.getHelmet() != null && eq.getHelmet().getType() != Material.AIR) {
                // remove old stats if old helmet was custom
                int oldId = ItemUtil.getCustomItemId(eq.getHelmet());
                if (oldId != -1) {
                    CustomItem oldItem = ItemManager.getInstance().getItemById(oldId);
                    removeItemStats(player, oldItem);
                    equippedArmors.getOrDefault(player.getUniqueId(), new HashSet<>()).remove(oldId);
                }
            }
            eq.setHelmet(armorStack);
        } else if (mat.name().endsWith("_CHESTPLATE")) {
            if (eq.getChestplate() != null && eq.getChestplate().getType() != Material.AIR) {
                int oldId = ItemUtil.getCustomItemId(eq.getChestplate());
                if (oldId != -1) {
                    CustomItem oldItem = ItemManager.getInstance().getItemById(oldId);
                    removeItemStats(player, oldItem);
                    equippedArmors.getOrDefault(player.getUniqueId(), new HashSet<>()).remove(oldId);
                }
            }
            eq.setChestplate(armorStack);
        } else if (mat.name().endsWith("_LEGGINGS")) {
            if (eq.getLeggings() != null && eq.getLeggings().getType() != Material.AIR) {
                int oldId = ItemUtil.getCustomItemId(eq.getLeggings());
                if (oldId != -1) {
                    CustomItem oldItem = ItemManager.getInstance().getItemById(oldId);
                    removeItemStats(player, oldItem);
                    equippedArmors.getOrDefault(player.getUniqueId(), new HashSet<>()).remove(oldId);
                }
            }
            eq.setLeggings(armorStack);
        } else if (mat.name().endsWith("_BOOTS")) {
            if (eq.getBoots() != null && eq.getBoots().getType() != Material.AIR) {
                int oldId = ItemUtil.getCustomItemId(eq.getBoots());
                if (oldId != -1) {
                    CustomItem oldItem = ItemManager.getInstance().getItemById(oldId);
                    removeItemStats(player, oldItem);
                    equippedArmors.getOrDefault(player.getUniqueId(), new HashSet<>()).remove(oldId);
                }
            }
            eq.setBoots(armorStack);
        }

        // Now apply stats
        applyItemStats(player, cItem);
        equippedArmors.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(cItem.getId());
    }

    private boolean isArmorMaterial(Material mat) {
        return mat.name().endsWith("_HELMET")
            || mat.name().endsWith("_CHESTPLATE")
            || mat.name().endsWith("_LEGGINGS")
            || mat.name().endsWith("_BOOTS");
    }

    private void applyItemStats(Player player, CustomItem cItem) {
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player);
        Bukkit.getLogger().info("[ArmorEquipListener] Applying armor stats: " + cItem.getName());

        ps.healthStat += (cItem.getHp() / 2);
        ps.defenceStat += cItem.getDef();
        ps.strength += cItem.getStr();
        ps.agility += cItem.getAgi();
        ps.intelligence += cItem.getIntel();
        ps.dexterity += cItem.getDex();

        StatsManager.getInstance().recalcDerivedStats(player);
        Bukkit.getLogger().info("[ArmorEquipListener] Stats after equip => HPstat=" + ps.healthStat
            + ", STR=" + ps.strength + ", DEF=" + ps.defenceStat + ", AGI=" + ps.agility
            + ", INT=" + ps.intelligence + ", DEX=" + ps.dexterity);
    }

    private void removeItemStats(Player player, CustomItem cItem) {
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player);
        Bukkit.getLogger().info("[ArmorEquipListener] Removing armor stats: " + cItem.getName());

        ps.healthStat -= (cItem.getHp() / 2);
        ps.defenceStat -= cItem.getDef();
        ps.strength -= cItem.getStr();
        ps.agility -= cItem.getAgi();
        ps.intelligence -= cItem.getIntel();
        ps.dexterity -= cItem.getDex();

        StatsManager.getInstance().recalcDerivedStats(player);
        Bukkit.getLogger().info("[ArmorEquipListener] Stats after removal => HPstat=" + ps.healthStat
            + ", STR=" + ps.strength + ", DEF=" + ps.defenceStat + ", AGI=" + ps.agility
            + ", INT=" + ps.intelligence + ", DEX=" + ps.dexterity);
    }
}
