package me.nakilex.levelplugin.transmog;

import me.nakilex.levelplugin.items.data.ArmorType;
import me.nakilex.levelplugin.items.data.WeaponType;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.mob.config.ModelSetManager;
import me.nakilex.levelplugin.npc.system.NpcTagUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Tracks which Nexo models a player has unlocked for weapon and armour
 * transmogs. Models are unlocked automatically when a player obtains an item
 * using that model.
 */
public class TransmogManager implements Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, Set<String>> weaponUnlocked = new HashMap<>();
    private final Map<UUID, Set<String>> armorUnlocked  = new HashMap<>();
    private final Set<String> knownWeaponModels = new HashSet<>();
    private final Set<String> knownArmorModels  = new HashSet<>();

    /** Track the type associated with each model id for validation. */
    private final Map<String, WeaponType> weaponModelTypes = new HashMap<>();
    private final Map<String, ArmorType> armorModelTypes  = new HashMap<>();

    public TransmogManager(JavaPlugin plugin, ModelSetManager modelSetManager) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        if (modelSetManager != null) {
            knownWeaponModels.addAll(modelSetManager.getAllWeaponModelIds());
            knownArmorModels.addAll(modelSetManager.getAllArmorModelIds());
        }
    }

    /**
     * Unlock a model id for the given player and remember the model's type so
     * we can validate future transmogs.
     */
    public void unlockModel(UUID uuid, String id, WeaponType weaponType, ArmorType armorType) {
        boolean weapon = weaponType != null;
        Set<String> set = weapon
                ? weaponUnlocked.computeIfAbsent(uuid, k -> new HashSet<>())
                : armorUnlocked.computeIfAbsent(uuid, k -> new HashSet<>());
        boolean added = set.add(id);
        if (weapon) {
            knownWeaponModels.add(id);
            weaponModelTypes.put(id, weaponType);
        } else {
            knownArmorModels.add(id);
            armorModelTypes.put(id, armorType);
        }
        if (added) {
            Player p = plugin.getServer().getPlayer(uuid);
            String name = p != null ? p.getName() : uuid.toString();
            Bukkit.getLogger().info("[TransmogDebug] " + name + " unlocked model " + id);
        }
    }

    public Set<String> getUnlocked(UUID uuid, boolean weapon) {
        return weapon ? weaponUnlocked.getOrDefault(uuid, Collections.emptySet())
                      : armorUnlocked.getOrDefault(uuid, Collections.emptySet());
    }

    public Set<String> getKnownModels(boolean weapon) {
        return weapon ? knownWeaponModels : knownArmorModels;
    }

    public boolean isUnlocked(UUID uuid, String id) {
        return weaponUnlocked.getOrDefault(uuid, Collections.emptySet()).contains(id)
                || armorUnlocked.getOrDefault(uuid, Collections.emptySet()).contains(id);
    }

    /** Get the weapon type associated with a model id, if known. */
    public WeaponType getWeaponType(String modelId) {
        WeaponType wt = weaponModelTypes.get(modelId);
        if (wt == null) {
            wt = inferWeaponType(modelId);
            if (wt != null) weaponModelTypes.put(modelId, wt);
        }
        return wt;
    }

    /** Get the armor type associated with a model id, if known. */
    public ArmorType getArmorType(String modelId) {
        ArmorType at = armorModelTypes.get(modelId);
        if (at == null) {
            at = inferArmorType(modelId);
            if (at != null) armorModelTypes.put(modelId, at);
        }
        return at;
    }

    private WeaponType inferWeaponType(String modelId) {
        com.nexomc.nexo.items.ItemBuilder b = com.nexomc.nexo.api.NexoItems.itemFromId(modelId);
        if (b == null) return null;
        ItemStack stack = b.build();
        return WeaponType.matchType(stack);
    }

    private ArmorType inferArmorType(String modelId) {
        com.nexomc.nexo.items.ItemBuilder b = com.nexomc.nexo.api.NexoItems.itemFromId(modelId);
        if (b == null) return null;
        ItemStack stack = b.build();
        return ArmorType.matchType(stack);
    }

    public void setUnlocked(UUID uuid, Set<String> weapons, Set<String> armors) {
        weaponUnlocked.put(uuid, new HashSet<>(weapons));
        armorUnlocked.put(uuid, new HashSet<>(armors));

        // Attempt to infer model types for validation purposes on load.
        for (String id : weapons) {
            WeaponType wt = inferWeaponType(id);
            if (wt != null) {
                weaponModelTypes.put(id, wt);
                knownWeaponModels.add(id);
            }
        }
        for (String id : armors) {
            ArmorType at = inferArmorType(id);
            if (at != null) {
                armorModelTypes.put(id, at);
                knownArmorModels.add(id);
            }
        }
    }

    private void handleItem(Player player, ItemStack stack) {
        String id = ItemUtil.getNexoModelId(stack);
        if (id == null) return;
        WeaponType w = WeaponType.matchType(stack);
        ArmorType a = ArmorType.matchType(stack);
        if (w != null) {
            unlockModel(player.getUniqueId(), id, w, null);
        } else if (a != null) {
            unlockModel(player.getUniqueId(), id, null, a);
        }
    }

    private void scanInventory(Player player) {
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null) handleItem(player, stack);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (NpcTagUtil.isNpc(e.getPlayer())) {
            return;
        }
        scanInventory(e.getPlayer());
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player p) {
            handleItem(p, e.getItem().getItemStack());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> scanInventory(p), 1L);
    }
}
