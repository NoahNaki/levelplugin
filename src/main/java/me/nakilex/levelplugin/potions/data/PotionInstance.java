package me.nakilex.levelplugin.potions.data;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import me.nakilex.levelplugin.utils.TooltipUtil;
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

        String baseName = ChatColor.translateAlternateColorCodes('&', template.getName());
        String display = baseName + " " + ChatColor.DARK_GRAY + "[" + ChatColor.GRAY + charges
                + ChatColor.WHITE + "/" + ChatColor.GRAY + template.getCharges() + ChatColor.DARK_GRAY + "]";
        meta.setDisplayName(display);

        List<String> lore = new java.util.ArrayList<>();
        boolean mana = template.getId().startsWith("mana");

        me.nakilex.levelplugin.items.data.ItemRarity rarity;
        switch (template.getTier()) {
            case 1 -> rarity = me.nakilex.levelplugin.items.data.ItemRarity.COMMON;
            case 2 -> rarity = me.nakilex.levelplugin.items.data.ItemRarity.UNCOMMON;
            case 3 -> rarity = me.nakilex.levelplugin.items.data.ItemRarity.RARE;
            default -> rarity = me.nakilex.levelplugin.items.data.ItemRarity.COMMON;
        }

        String rarityGlyph = "<glyph:" + rarity.name().toLowerCase() + ">";
        lore.add(rarityGlyph + "<glyph:potion>");
        lore.add("");
        lore.add(ChatColor.WHITE + "Effect:");
        String bulletColor = mana ? ChatColor.AQUA.toString() : ChatColor.RED.toString();
        String amountStr;
        if (template.getHealAmount() > 0) {
            amountStr = String.valueOf((int) template.getHealAmount());
        } else {
            amountStr = (int) (template.getHealPercent() * 100) + "%";
        }
        String symbol = mana ? " ✨" : " ❤";
        String action = mana ? "Restore " : "Heal ";
        lore.add(bulletColor + "- " + ChatColor.GRAY + action + ChatColor.WHITE + amountStr + bulletColor + symbol);
        lore.add(bulletColor + "- " + ChatColor.GRAY + "Cooldown: " + ChatColor.GRAY + template.getCooldownSeconds() + " seconds");
        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions(null, "to consume"));
        meta.setLore(lore);

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_DYE);
        meta.setUnbreakable(true);

        // Store UUID in PersistentDataContainer
        PersistentDataContainer data = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, "potion_uuid");
        data.set(key, PersistentDataType.STRING, uuid.toString());

        item.setItemMeta(meta);
        return item;
    }

    private String toRoman(int number) {
        String[] numerals = {"I","II","III","IV","V","VI","VII","VIII","IX","X"};
        return (number >= 1 && number <= 10) ? numerals[number-1] : String.valueOf(number);
    }
}
