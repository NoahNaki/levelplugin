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
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Check if item is valid and is a potion
        if (item == null || !item.hasItemMeta() ||
            !(item.getType() == Material.POTION || item.getType() == Material.SPLASH_POTION || item.getType() == Material.LINGERING_POTION)) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, "potion_uuid");

        if (!data.has(key, PersistentDataType.STRING)) {
            return;
        }

        UUID uuid;
        try {
            uuid = UUID.fromString(data.get(key, PersistentDataType.STRING));
        } catch (IllegalArgumentException e) {
            return;
        }

        PotionInstance instance = potionManager.getPotionInstance(uuid);
        if (instance == null || potionManager.isOnCooldown(uuid)) {
            player.sendMessage("Potion is on cooldown!");
            return;
        }

        // Cancel default animation
        event.setCancelled(true);

        String potionId = instance.getTemplate().getId();

        // Check if it's a healing potion and if the player's health is already full.
        if (potionId.equals("healing_potion")) {
            if (player.getHealth() >= player.getMaxHealth()) {
                player.sendMessage("Your health is already full!");
                return;
            }
        }

        // Consume potion charge and start cooldown only if the potion can be used
        instance.consumeCharge();
        potionManager.startCooldown(uuid, instance.getTemplate().getCooldownSeconds());

        // Apply effects based on potion type
        if (potionId.equals("healing_potion")) {
            int healAmount = (int) (player.getMaxHealth() * 0.1); // Restore 10% HP
            player.setHealth(Math.min(player.getHealth() + healAmount, player.getMaxHealth()));
            meta.setDisplayName("§cHealing Potion §4[" + instance.getCharges() + "/3]");
            meta.setLore(Arrays.asList("§4- §7Recover §f10% §c❤"));
        } else if (potionId.equals("mana_potion")) {
            int currentMana = StatsManager.getInstance().getPlayerStats(player.getUniqueId()).getCurrentMana();
            int maxMana = StatsManager.getInstance().getPlayerStats(player.getUniqueId()).getMaxMana();

            int manaRestore = (int) (maxMana * 0.1); // Restore 10% of max mana
            int newMana = Math.min(currentMana + manaRestore, maxMana);

            StatsManager.getInstance().getPlayerStats(player.getUniqueId()).setCurrentMana(newMana);
            meta.setDisplayName("§bMana Potion §3[" + instance.getCharges() + "/3]");
            meta.setLore(Arrays.asList("§3- §7Recover §f10% §b✨"));
        }

        player.sendMessage("Potion consumed! Remaining charges: " + instance.getCharges());
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
