package me.nakilex.levelplugin.cursormenu;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.cursormenu.display.ItemDisplayManager;
import me.nakilex.levelplugin.cursormenu.display.TextDisplayManager;
import me.nakilex.levelplugin.cursormenu.layout.Section;
import me.nakilex.levelplugin.cursormenu.layout.SectionManager;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central access point for the cursor menu system. This is deliberately kept
 * lightweight; most heavy lifting such as entity handling and animation is
 * delegated to the specialised managers.
 */
public class CursorMenuManager {
    private final Main plugin;
    private final SectionManager sectionManager = new SectionManager();
    private final ItemDisplayManager itemDisplayManager = new ItemDisplayManager();
    private final TextDisplayManager textDisplayManager = new TextDisplayManager();
    private final Map<UUID, Section> activeMenus = new ConcurrentHashMap<>();

    public CursorMenuManager(Main plugin) {
        this.plugin = plugin;
    }

    /** Start showing the menu identified by the given key to the player. */
    public void setupCursor(Player player, String menuKey) {
        Section section = sectionManager.get(menuKey);
        if (section == null) return;
        if (section.getPermission() != null && !player.hasPermission(section.getPermission())) return;
        activeMenus.put(player.getUniqueId(), section);
        // For simplicity, just show a placeholder text display listing number of layouts
        textDisplayManager.show(player, "Layouts: " + section.getLayouts().size());
    }

    /** Stop any active cursor menu for the player. */
    public void stopCursor(Player player) {
        activeMenus.remove(player.getUniqueId());
        itemDisplayManager.hide(player);
        textDisplayManager.hide(player);
    }

    public SectionManager getSectionManager() {
        return sectionManager;
    }
}
