package me.nakilex.levelplugin.cursormenu;

import me.nakilex.levelplugin.cursormenu.display.ItemDisplayManager;
import me.nakilex.levelplugin.cursormenu.display.TextDisplayManager;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central manager for the cursor menu system. This class coordinates the
 * different display managers and keeps track of which menu a player currently
 * has open.
 */
public class CursorMenuManager {

    private final Map<UUID, String> currentMenu = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> selectedLayout = new ConcurrentHashMap<>();
    private final SectionManager sectionManager;
    private final ItemDisplayManager itemDisplayManager;
    private final TextDisplayManager textDisplayManager;

    public CursorMenuManager(SectionManager sectionManager,
                             ItemDisplayManager itemDisplayManager,
                             TextDisplayManager textDisplayManager) {
        this.sectionManager = sectionManager;
        this.itemDisplayManager = itemDisplayManager;
        this.textDisplayManager = textDisplayManager;
    }

    /**
     * Start displaying the cursor menu for the player. This method simply stores
     * the menu key and resets the selected layout index.
     */
    public void setupCursor(Player player, String menuKey) {
        currentMenu.put(player.getUniqueId(), menuKey);
        selectedLayout.put(player.getUniqueId(), 0);
        textDisplayManager.show(player, menuKey);
    }

    /**
     * Stop the cursor for the player and remove any displayed items or text.
     */
    public void stopCursor(Player player, boolean cleanLocation) {
        currentMenu.remove(player.getUniqueId());
        selectedLayout.remove(player.getUniqueId());
        itemDisplayManager.hide(player);
        textDisplayManager.hide(player);
    }

    /**
     * Update the cursor position for the player. This simplified implementation
     * only records the selected index based on yaw; sophisticated calculations
     * should be implemented on top.
     */
    public void updateCursorPosition(Player player, float yaw, float pitch) {
        int index = Math.round(yaw) % 10; // dummy example
        selectedLayout.put(player.getUniqueId(), index);
    }

    public MenuLayout getSelectedLayout(Player player) {
        String key = currentMenu.get(player.getUniqueId());
        if (key == null) {
            return null;
        }
        int index = selectedLayout.getOrDefault(player.getUniqueId(), 0);
        return sectionManager.getLayout(key, index);
    }

    public String getCurrentMenu(Player player) {
        return currentMenu.get(player.getUniqueId());
    }
}
