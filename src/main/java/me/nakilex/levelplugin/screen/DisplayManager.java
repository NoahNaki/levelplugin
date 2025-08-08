package me.nakilex.levelplugin.screen;

import org.bukkit.entity.Display;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic base for managers that spawn Display entities for players.
 * Implementations are responsible for creating the entity; this class
 * simply tracks them and provides cleanup utilities.
 */
public abstract class DisplayManager<T extends Display> {

    private final Map<UUID, List<T>> active = new ConcurrentHashMap<>();

    protected void track(Player player, T display) {
        active.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(display);
    }

    public void clear(Player player) {
        List<T> list = active.remove(player.getUniqueId());
        if (list != null) {
            for (T d : list) {
                if (!d.isDead()) {
                    d.remove();
                }
            }
        }
    }

    public void clearAll() {
        for (UUID id : new ArrayList<>(active.keySet())) {
            clearById(id);
        }
    }

    private void clearById(UUID id) {
        List<T> list = active.remove(id);
        if (list != null) {
            for (T d : list) {
                if (!d.isDead()) {
                    d.remove();
                }
            }
        }
    }
}
