package me.nakilex.levelplugin.potions.listeners;

import me.nakilex.levelplugin.potions.managers.PotionManager;
import me.nakilex.levelplugin.potions.data.PotionInstance;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class PotionUseListener implements Listener {

    private final PotionManager potionManager;
    private final JavaPlugin plugin;

    public PotionUseListener(PotionManager potionManager, JavaPlugin plugin) {
        this.potionManager = potionManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerUsePotion(PlayerInteractEvent event) {
        // Debug log to check event triggering
        System.out.println("PlayerInteractEvent triggered");

        if (event.getHand() != EquipmentSlot.HAND) {
            System.out.println("Event skipped: not main hand");
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Check if item is valid and is a potion
        if (item == null || !item.hasItemMeta() || !(item.getType() == Material.POTION || item.getType() == Material.SPLASH_POTION || item.getType() == Material.LINGERING_POTION)) {
            System.out.println("Event skipped: invalid item or not a potion");
            return;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, "potion_uuid");

        if (!data.has(key, PersistentDataType.STRING)) {
            System.out.println("No UUID found in PersistentDataContainer");
            return;
        }

        UUID uuid;
        try {
            uuid = UUID.fromString(data.get(key, PersistentDataType.STRING));
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid UUID in PersistentDataContainer");
            return;
        }

        PotionInstance instance = potionManager.getPotionInstance(uuid);
        if (instance == null || potionManager.isOnCooldown(uuid)) {
            player.sendMessage("Potion is on cooldown!");
            System.out.println("Cooldown active or instance not found");
            return;
        }

        // Cancel default animation if potion
        event.setCancelled(true);

        instance.consumeCharge();
        potionManager.startCooldown(uuid, instance.getTemplate().getCooldownSeconds());

        // Apply effects based on potion type
        String potionId = instance.getTemplate().getId();

        if (potionId.equals("healing_potion")) {
            int healAmount = (int) (player.getMaxHealth() * 0.1); // Restore 10% HP
            player.setHealth(Math.min(player.getHealth() + healAmount, player.getMaxHealth()));
            meta.setDisplayName("§cHealing Potion §4[" + instance.getCharges() + "/3]");
            List<String> lore = Collections.emptyList();
            System.out.println("Setting lore: " + lore); // Check lore before setting
            meta.setLore(lore);
            meta.setLore(Arrays.asList(
                "§4- §7Recover §f10% §c❤"
            ));
        } else if (potionId.equals("mana_potion")) {
            int currentMana = StatsManager.getInstance().getPlayerStats(player.getUniqueId()).getCurrentMana();
            int maxMana = StatsManager.getInstance().getPlayerStats(player.getUniqueId()).getMaxMana();

            int manaRestore = (int) (maxMana * 0.1); // Restore 10% of max mana
            int newMana = Math.min(currentMana + manaRestore, maxMana);

            StatsManager.getInstance().getPlayerStats(player.getUniqueId()).setCurrentMana(newMana);
            meta.setDisplayName("§bMana Potion §3[" + instance.getCharges() + "/3]");
            List<String> lore = Collections.emptyList();
            System.out.println("Setting lore: " + lore); // Check lore before setting
            meta.setLore(lore);
            meta.setLore(Arrays.asList(
                "§3- §7Recover §f10% §b✨"
            ));
        }

        player.sendMessage("Potion consumed! Remaining charges: " + instance.getCharges());

        // Update item meta
        item.setItemMeta(meta);

        if (instance.getCharges() <= 0) {
            player.getInventory().remove(item);
        }
    }

    @EventHandler
    public void onPotionConsume(PlayerItemConsumeEvent event) {
        // Debug log for consume event
        System.out.println("PlayerItemConsumeEvent triggered");
        event.setCancelled(true); // Always cancel consume animation
    }
}
