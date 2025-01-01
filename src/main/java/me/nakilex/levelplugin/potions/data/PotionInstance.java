package me.nakilex.levelplugin.potions.data;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class PotionInstance {

    private final UUID uuid;
    private final PotionTemplate template;
    private int charges;

    public PotionInstance(PotionTemplate template) {
        this.uuid = UUID.randomUUID();
        this.template = template;
        this.charges = template.getCharges();
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getCharges() {
        return charges;
    }

    public PotionTemplate getTemplate() {
        return template;
    }


    public void consumeCharge() {
        if (charges > 0) {
            charges--;
        }
    }

    public ItemStack toItemStack(JavaPlugin plugin) {
        ItemStack item = new ItemStack(template.getMaterial());
        ItemMeta meta = item.getItemMeta();

        // Set Display Name with Charges
        if (template.getId().equals("healing_potion")) {
            meta.setDisplayName("§cHealing Potion §4[" + charges + "/" + template.getCharges() + "]");
            List<String> lore = Collections.emptyList();
            System.out.println("Setting lore: " + lore); // Check lore before setting
            meta.setLore(lore);
            meta.setLore(Arrays.asList("§4- §7Recover §f10% §c❤"));
        } else if (template.getId().equals("mana_potion")) {
            meta.setDisplayName("§bMana Potion §3[" + charges + "/" + template.getCharges() + "]");
            List<String> lore = Collections.emptyList();
            System.out.println("Setting lore: " + lore); // Check lore before setting
            meta.setLore(lore);
            meta.setLore(Arrays.asList("§3- §7Recover §f10% §b✨"));
        }

        // Store UUID in PersistentDataContainer
        PersistentDataContainer data = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, "potion_uuid");
        data.set(key, PersistentDataType.STRING, uuid.toString());

        item.setItemMeta(meta);
        return item;
    }
}
