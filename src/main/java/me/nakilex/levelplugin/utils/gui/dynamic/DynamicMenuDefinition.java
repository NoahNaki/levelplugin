package me.nakilex.levelplugin.utils.gui.dynamic;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public record DynamicMenuDefinition(
        String id,
        String title,
        int size,
        Material filler,
        boolean border,
        boolean fillEmptySlots,
        List<DynamicMenuItemDefinition> items
) {
    public static DynamicMenuDefinition fromSection(String id, ConfigurationSection section) {
        String title = section.getString("title", id);
        int size = section.getInt("size", 54);
        Material filler = parseMaterial(section.getString("filler", "GRAY_STAINED_GLASS_PANE"));
        boolean border = section.getBoolean("border", true);
        boolean fillEmptySlots = section.getBoolean("fill-empty-slots", true);

        List<DynamicMenuItemDefinition> items = new ArrayList<>();
        ConfigurationSection itemsSection = section.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection == null) {
                    continue;
                }
                DynamicMenuItemDefinition item = DynamicMenuItemDefinition.fromSection(itemSection);
                if (item != null) {
                    items.add(item);
                }
            }
        }

        return new DynamicMenuDefinition(id, title, size, filler, border, fillEmptySlots, items);
    }

    private static Material parseMaterial(String value) {
        if (value == null || value.isBlank()) {
            return Material.GRAY_STAINED_GLASS_PANE;
        }
        Material material = Material.matchMaterial(value);
        return material == null ? Material.GRAY_STAINED_GLASS_PANE : material;
    }
}

