package me.nakilex.levelplugin.potions.data;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

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
        ItemStack item;
        if (template.getNexoId() != null && !template.getNexoId().isEmpty()) {
            ItemBuilder b = NexoItems.itemFromId(template.getNexoId());
            item = b != null ? b.build() : new ItemStack(template.getMaterial());
        } else {
            item = new ItemStack(template.getMaterial());
        }
        ItemMeta meta = item.getItemMeta();

        // Set Display Name with Charges using nicer colors
        String display = template.getName() + ChatColor.DARK_GRAY + " [" + ChatColor.AQUA
                + charges + ChatColor.GRAY + "/" + ChatColor.AQUA + template.getCharges()
                + ChatColor.DARK_GRAY + "]";
        meta.setDisplayName(display);

        List<String> lore = new java.util.ArrayList<>();
        boolean mana = template.getId().startsWith("mana");
        if (mana) {
            if (template.getHealAmount() > 0) {
                lore.add(ChatColor.DARK_AQUA + "↣ " + ChatColor.GRAY + "Recover "
                        + ChatColor.WHITE + (int) template.getHealAmount() + ChatColor.AQUA + " ✨");
            } else {
                lore.add(ChatColor.DARK_AQUA + "↣ " + ChatColor.GRAY + "Recover "
                        + ChatColor.WHITE + (int) (template.getHealPercent() * 100) + "% "
                        + ChatColor.AQUA + "✨");
            }
        } else {
            if (template.getHealAmount() > 0) {
                lore.add(ChatColor.DARK_RED + "↣ " + ChatColor.GRAY + "Recover "
                        + ChatColor.WHITE + (int) template.getHealAmount() + ChatColor.RED + " ❤");
            } else if (template.getHealPercent() > 0) {
                lore.add(ChatColor.DARK_RED + "↣ " + ChatColor.GRAY + "Recover "
                        + ChatColor.WHITE + (int) (template.getHealPercent() * 100) + "% "
                        + ChatColor.RED + " ❤");
            }
        }
        lore.add(ChatColor.GRAY + "Right-click to drink");
        meta.setLore(lore);

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
        meta.setUnbreakable(true);

        // Store UUID in PersistentDataContainer
        PersistentDataContainer data = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, "potion_uuid");
        data.set(key, PersistentDataType.STRING, uuid.toString());

        item.setItemMeta(meta);
        return item;
    }
}
