package me.nakilex.levelplugin.trinkets.data;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.trinkets.effects.TrinketEffectType;
import me.nakilex.levelplugin.utils.NumberRange;
import me.nakilex.levelplugin.utils.TextUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Describes the static data needed to build and roll a trinket item.
 */
public class TrinketTemplate {

    private final String id;
    private final String displayName;
    private final Material material;
    private final String nexoModelId;
    private final List<String> description;
    private final TrinketEffectType effectType;
    private final NumberRange magnitudeRange;
    private final NumberRange durationRange;
    private final int cooldownSeconds;

    public TrinketTemplate(String id,
                           String displayName,
                           Material material,
                           String nexoModelId,
                           List<String> description,
                           TrinketEffectType effectType,
                           NumberRange magnitudeRange,
                           NumberRange durationRange,
                           int cooldownSeconds) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.nexoModelId = nexoModelId;
        this.description = description == null ? List.of() : List.copyOf(description);
        this.effectType = effectType;
        this.magnitudeRange = magnitudeRange == null ? NumberRange.fixed(0.0) : magnitudeRange;
        this.durationRange = durationRange == null ? NumberRange.fixed(0.0) : durationRange;
        this.cooldownSeconds = cooldownSeconds;
    }

    public String getId() {
        return id;
    }

    public String getFormattedName() {
        return ChatColor.translateAlternateColorCodes('&', Objects.requireNonNullElse(displayName, id));
    }

    public TrinketEffectType getEffectType() {
        return effectType;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public TrinketEffectDefinition rollEffect() {
        double magnitude = magnitudeRange != null ? magnitudeRange.roll() : 0.0;
        double duration = durationRange != null ? durationRange.roll() : 0.0;
        return new TrinketEffectDefinition(effectType, magnitude, duration);
    }

    public TrinketEffectDefinition createEffect(double magnitude, double duration) {
        return new TrinketEffectDefinition(effectType, magnitude, duration);
    }

    public TrinketEffectDefinition getDefaultEffect() {
        double magnitude = magnitudeRange != null ? magnitudeRange.midpoint() : 0.0;
        double duration = durationRange != null ? durationRange.midpoint() : 0.0;
        return new TrinketEffectDefinition(effectType, magnitude, duration);
    }

    public ItemStack createItemStack(NamespacedKey idKey,
                                     NamespacedKey magnitudeKey,
                                     NamespacedKey durationKey,
                                     NamespacedKey rarityKey) {
        TrinketEffectDefinition effect = rollEffect();
        return createItemStack(idKey, magnitudeKey, durationKey, rarityKey, effect);
    }

    public ItemStack createItemStack(NamespacedKey idKey,
                                     NamespacedKey magnitudeKey,
                                     NamespacedKey durationKey,
                                     NamespacedKey rarityKey,
                                     TrinketEffectDefinition effect) {
        ItemStack stack = buildBaseItem();
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        meta.setDisplayName(getFormattedName());

        int magnitudeTier = effect.getMagnitudeTier();
        int durationTier = effect.getDurationTier();
        int combinedTier = Math.max(1, magnitudeTier + durationTier);
        ItemRarity rarity = rarityFromTierSum(combinedTier);

        List<String> lore = new ArrayList<>();
        String rarityGlyph = "<glyph:" + rarity.name().toLowerCase() + ">";
        lore.add(rarityGlyph + "<glyph:trinket>");
        lore.add("");
        if (!description.isEmpty()) {
            for (String line : description) {
                lore.add(ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', line));
            }
            lore.add("");
        }

        lore.add(ChatColor.WHITE + "Effect:");
        String magnitudeTierText = TextUtil.toRomanNumeral(magnitudeTier);
        lore.add(ChatColor.AQUA + "- " + ChatColor.GRAY + effectType.getDisplayName() + ChatColor.WHITE + " "
                + effect.formatMagnitude() + ChatColor.DARK_GRAY + " [" + magnitudeTierText + "]");
        String durationTierText = TextUtil.toRomanNumeral(durationTier);
        lore.add(ChatColor.AQUA + "- " + ChatColor.GRAY + "Duration: " + ChatColor.WHITE
                + formatSeconds(effect.getDurationSeconds()) + ChatColor.DARK_GRAY + " [" + durationTierText + "]");
        if (cooldownSeconds > 0) {
            lore.add(ChatColor.AQUA + "- " + ChatColor.GRAY + "Cooldown: " + ChatColor.WHITE
                    + cooldownSeconds + "s");
        }
        lore.add(" ");
        lore.addAll(TooltipUtil.keyInstructions("F", "to activate."));

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_DYE);
        meta.setUnbreakable(true);
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (idKey != null) {
            container.set(idKey, PersistentDataType.STRING, id);
        }
        if (magnitudeKey != null) {
            container.set(magnitudeKey, PersistentDataType.DOUBLE, effect.getMagnitude());
        }
        if (durationKey != null) {
            container.set(durationKey, PersistentDataType.DOUBLE, effect.getDurationSeconds());
        }
        if (rarityKey != null) {
            container.set(rarityKey, PersistentDataType.STRING, rarity.name());
        }
        stack.setItemMeta(meta);

        ItemUtil.applyRarityTooltipStyle(stack, rarity);
        return stack;
    }

    public static ItemRarity rarityFromTierSum(int tierSum) {
        if (tierSum <= 4) {
            return ItemRarity.COMMON;
        }
        if (tierSum <= 7) {
            return ItemRarity.UNCOMMON;
        }
        if (tierSum <= 10) {
            return ItemRarity.RARE;
        }
        if (tierSum <= 13) {
            return ItemRarity.EPIC;
        }
        if (tierSum <= 16) {
            return ItemRarity.LEGENDARY;
        }
        if (tierSum <= 19) {
            return ItemRarity.MYTHIC;
        }
        return ItemRarity.FABLED;
    }

    private ItemStack buildBaseItem() {
        if (nexoModelId != null && !nexoModelId.isBlank()) {
            ItemBuilder builder = NexoItems.itemFromId(nexoModelId);
            if (builder != null) {
                return builder.build();
            }
        }
        return new ItemStack(material);
    }

    private String formatSeconds(double seconds) {
        if (seconds == (long) seconds) {
            return (long) seconds + "s";
        }
        return String.format("%.1fs", seconds);
    }
}
