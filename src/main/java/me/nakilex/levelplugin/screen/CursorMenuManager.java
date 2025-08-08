package me.nakilex.levelplugin.screen;

import me.nakilex.levelplugin.utils.PlayerState;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Pig;
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
    private final Map<UUID, PlayerState> states = new ConcurrentHashMap<>();
    private final Map<UUID, Entity> mounts = new ConcurrentHashMap<>();
    private final Map<UUID, Location> previousLocations = new ConcurrentHashMap<>();

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

        // Remember current state and location so we can restore later
        states.put(player.getUniqueId(), PlayerState.capture(player));
        previousLocations.put(player.getUniqueId(), player.getLocation());

        // Teleport the player to the configured camera if present and show a simple title.
        var camera = section.getCameraLocation();
        player.teleport(camera);

        // Mount the player on an invisible pig so movement is locked while
        // still allowing free look with the cursor
        Pig pig = camera.getWorld().spawn(camera, Pig.class, e -> {
            e.setInvisible(true);
            e.setAI(false);
            e.setSilent(true);
            e.setCollidable(false);
            e.setInvulnerable(true);
        });
        pig.addPassenger(player);
        mounts.put(player.getUniqueId(), pig);

        textManager.show(player, camera.clone().add(0, 2, 0), net.kyori.adventure.text.Component.text("Menu: " + key));
    }

    /**
     * Stop any active menu for the player and remove displays.
     *
     * @return {@code true} if a menu was active and got closed
     */
    public boolean closeMenu(Player player) {
        boolean hadMenu = currentMenu.remove(player.getUniqueId()) != null;
        textManager.clear(player);
        itemManager.clear(player);
        Entity mount = mounts.remove(player.getUniqueId());
        if (mount != null) {
            mount.remove();
        }
        Location prev = previousLocations.remove(player.getUniqueId());
        if (prev != null) {
            player.teleport(prev);
        }
        PlayerState state = states.remove(player.getUniqueId());
        if (state != null) {
            state.restore(player);
        }
        return hadMenu;
    }

    /**
     * @return the key of the currently open menu for {@code player} or {@code null}
     */
    public String getCurrentMenu(Player player) {
        return currentMenu.get(player.getUniqueId());
    }

    public TextDisplayManager getTextManager() {
        return textManager;
    }

    public ItemDisplayManager getItemManager() {
        return itemManager;
    }
}
