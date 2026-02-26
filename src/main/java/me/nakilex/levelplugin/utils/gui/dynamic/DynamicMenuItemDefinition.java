package me.nakilex.levelplugin.utils.gui.dynamic;

import me.nakilex.levelplugin.utils.GuiUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record DynamicMenuItemDefinition(
        int slot,
        String type,
        String id,
        Material material,
        String name,
        List<String> lore,
        List<String> leftClickCommands,
        List<String> rightClickCommands,
        boolean closeOnClick
) {
    public static DynamicMenuItemDefinition fromSection(ConfigurationSection section) {
        int slot = section.getInt("slot", -1);
        if (slot < 0) {
            return null;
        }
        String type = section.getString("type", "NEXO").toUpperCase();
        String id = section.getString("id", "barrier");
        Material material = parseMaterial(section.getString("material", "STONE"));
        String name = section.getString("name", " ");
        List<String> lore = section.getStringList("lore");
        List<String> leftClick = normalizeCommands(section.getStringList("left-click-commands"));
        List<String> rightClick = normalizeCommands(section.getStringList("right-click-commands"));
        boolean closeOnClick = section.getBoolean("close-on-click", false);

        return new DynamicMenuItemDefinition(slot, type, id, material, name, lore, leftClick, rightClick, closeOnClick);
    }

    public ItemStack toItem(Player player) {
        String renderedName = applyPlaceholders(name, player);
        List<String> renderedLore = lore.stream().map(line -> applyPlaceholders(line, player)).toList();
        if ("MATERIAL".equalsIgnoreCase(type)) {
            return GuiUtil.createGuiItem(material, renderedName, renderedLore);
        }
        return GuiUtil.getNexoItem(id, renderedName, renderedLore);
    }

    public boolean hasActions() {
        return !leftClickCommands.isEmpty() || !rightClickCommands.isEmpty();
    }

    private String applyPlaceholders(String input, Player player) {
        if (input == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', input)
                .replace("%player_name%", player.getName())
                .replace("%player_display_name%", player.getDisplayName());
    }

    private static Material parseMaterial(String value) {
        Material material = Material.matchMaterial(value);
        return material == null ? Material.STONE : material;
    }

    private static List<String> normalizeCommands(List<String> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        List<String> copy = new ArrayList<>();
        for (String entry : list) {
            if (entry != null && !entry.isBlank()) {
                copy.add(entry);
            }
        }
        return List.copyOf(copy);
    }
}
