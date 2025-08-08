package me.nakilex.levelplugin.screen;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High level manager coordinating sections and display managers.
 * This is a greatly simplified variant of the more feature rich cursor
 * menu system used on production servers, but exposes a similar API to
 * allow future extension.
 */
public class CursorMenuManager {

    private final SectionManager sectionManager = new SectionManager();
    private final TextDisplayManager textManager = new TextDisplayManager();
    private final ItemDisplayManager itemManager = new ItemDisplayManager();
    private final Map<UUID, String> currentMenu = new ConcurrentHashMap<>();

    public SectionManager getSectionManager() {
        return sectionManager;
    }

    /**
     * Begin showing the menu section identified by {@code key} to the player.
     */
    public void openMenu(Player player, String key) {
        Section section = sectionManager.get(key);
        if (section == null) {
            return;
        }
        currentMenu.put(player.getUniqueId(), key);
        // Teleport the player to the configured camera if present.
        player.teleport(section.getCameraLocation());
    }

    /**
     * Stop any active menu for the player and remove displays.
     */
    public void closeMenu(Player player) {
        currentMenu.remove(player.getUniqueId());
        textManager.clear(player);
        itemManager.clear(player);
    }

    public TextDisplayManager getTextManager() {
        return textManager;
    }

    public ItemDisplayManager getItemManager() {
        return itemManager;
    }
}
