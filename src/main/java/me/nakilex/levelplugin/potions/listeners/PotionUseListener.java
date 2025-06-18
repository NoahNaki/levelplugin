package me.nakilex.levelplugin.potions.listeners;

import me.nakilex.levelplugin.potions.managers.PotionManager;
import me.nakilex.levelplugin.potions.data.PotionInstance;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
import java.util.ArrayList;
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

        if (item == null || !item.hasItemMeta()) {
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
        if (instance == null) {
            return;
        }

        if (potionManager.isOnCooldown(uuid)) {
            long remain = potionManager.getRemainingCooldown(uuid);
            String baseName = ChatColor.translateAlternateColorCodes('&', instance.getTemplate().getName());
            player.sendMessage(ChatColor.RED + "You must wait " + ChatColor.GOLD + remain + "s "
                    + ChatColor.RED + "before using " + ChatColor.YELLOW + baseName + ChatColor.RED + " again.");
            return;
        }

        // Cancel default animation
        event.setCancelled(true);

        String potionId = instance.getTemplate().getId();
        String baseName = ChatColor.translateAlternateColorCodes('&', instance.getTemplate().getName());

        // Prevent using healing potions at full health or mana potions at full mana
        double restored = 0;
        if (potionId.startsWith("mana")) {
            int currentMana = StatsManager.getInstance().getPlayerStats(player.getUniqueId()).getCurrentMana();
            int maxMana = StatsManager.getInstance().getPlayerStats(player.getUniqueId()).getMaxMana();
            if (currentMana >= maxMana) {
                player.sendMessage(ChatColor.AQUA + "Your mana is already full!");
                return;
            }
        } else {
            if (player.getHealth() >= player.getMaxHealth()) {
                player.sendMessage(ChatColor.RED + "Your health is already full!");
                return;
            }
        }

        // Consume potion charge and start cooldown only if the potion can be used
        instance.consumeCharge();
        potionManager.startCooldown(uuid, instance.getTemplate().getCooldownSeconds());

        // Apply effects based on potion type
        if (potionId.startsWith("mana")) {
            int currentMana = StatsManager.getInstance().getPlayerStats(player.getUniqueId()).getCurrentMana();
            int maxMana = StatsManager.getInstance().getPlayerStats(player.getUniqueId()).getMaxMana();

            int manaRestore = (instance.getTemplate().getHealAmount() > 0)
                    ? (int) instance.getTemplate().getHealAmount()
                    : (int) (maxMana * instance.getTemplate().getHealPercent());

            int newMana = Math.min(currentMana + manaRestore, maxMana);
            StatsManager.getInstance().getPlayerStats(player.getUniqueId()).setCurrentMana(newMana);
            restored = newMana - currentMana;

            meta.setDisplayName(baseName + " " + ChatColor.DARK_GRAY + "[" + ChatColor.GRAY + instance.getCharges()
                    + ChatColor.WHITE + "/" + ChatColor.GRAY + instance.getTemplate().getCharges() + ChatColor.DARK_GRAY + "]");

            List<String> lore = new ArrayList<>();
            lore.add(" ");
            lore.add(ChatColor.WHITE + "Effect:");
            String amount = instance.getTemplate().getHealAmount() > 0
                    ? String.valueOf((int) instance.getTemplate().getHealAmount())
                    : (int) (instance.getTemplate().getHealPercent() * 100) + "%";
            lore.add(ChatColor.AQUA + "- " + ChatColor.GRAY + "Restore " + ChatColor.WHITE + amount + ChatColor.AQUA + " ✨");
            lore.add(ChatColor.AQUA + "- " + ChatColor.GRAY + "Cooldown: " + ChatColor.GRAY + instance.getTemplate().getCooldownSeconds() + " seconds");
            lore.add(" ");
            lore.add(ChatColor.WHITE + "Right-click " + ChatColor.GRAY + "to consume");
            meta.setLore(lore);
        } else {
            double healAmt = instance.getTemplate().getHealAmount();
            double healPct = instance.getTemplate().getHealPercent();
            double heal = healAmt > 0 ? healAmt : player.getMaxHealth() * healPct;
            double newHealth = Math.min(player.getHealth() + heal, player.getMaxHealth());
            restored = newHealth - player.getHealth();
            player.setHealth(newHealth);
            meta.setDisplayName(baseName + " " + ChatColor.DARK_GRAY + "[" + ChatColor.GRAY + instance.getCharges()
                    + ChatColor.WHITE + "/" + ChatColor.GRAY + instance.getTemplate().getCharges() + ChatColor.DARK_GRAY + "]");

            String amount = healAmt > 0 ? String.valueOf((int) healAmt) : (int) (healPct * 100) + "%";
            List<String> lore = new ArrayList<>();
            lore.add(" ");
            lore.add(ChatColor.WHITE + "Effect:");
            lore.add(ChatColor.RED + "- " + ChatColor.GRAY + "Heal " + ChatColor.WHITE + amount + ChatColor.RED + " ❤");
            lore.add(ChatColor.RED + "- " + ChatColor.GRAY + "Cooldown: " + ChatColor.GRAY + instance.getTemplate().getCooldownSeconds() + " seconds");
            lore.add(" ");
            lore.add(ChatColor.WHITE + "Right-click " + ChatColor.GRAY + "to consume");
            meta.setLore(lore);
        }

        String symbol = potionId.startsWith("mana") ? "\u2728" : "\u2764"; // ✨ or ❤
        ChatColor symColor = potionId.startsWith("mana") ? ChatColor.AQUA : ChatColor.RED;
        player.sendMessage(ChatColor.GREEN + "+" + ChatColor.WHITE + (int) restored + " "
                + symColor + symbol + ChatColor.GRAY + " (" + instance.getCharges() + " left)");
        item.setItemMeta(meta);

        me.nakilex.levelplugin.Main.getInstance().getQuestManager().handleConsumePotion(player, potionId);

        if (instance.getCharges() <= 0) {
            player.getInventory().remove(item);
        }
    }

    @EventHandler
    public void onPotionConsume(PlayerItemConsumeEvent event) {
        event.setCancelled(true); // Always cancel consume animation
    }

    private String toRoman(int number) {
        String[] numerals = {"I","II","III","IV","V","VI","VII","VIII","IX","X"};
        return (number >= 1 && number <= 10) ? numerals[number-1] : String.valueOf(number);
    }
}
