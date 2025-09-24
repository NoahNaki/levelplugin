package me.nakilex.levelplugin.trinkets.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.trinkets.data.ActiveTrinketEffect;
import me.nakilex.levelplugin.trinkets.data.TrinketEffectDefinition;
import me.nakilex.levelplugin.trinkets.data.TrinketTemplate;
import me.nakilex.levelplugin.trinkets.effects.TrinketEffectType;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.utils.NumberRange;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads trinket definitions and manages runtime activation state.
 */
public class TrinketManager {

    private final Main plugin;
    private final NamespacedKey trinketIdKey;
    private final NamespacedKey magnitudeKey;
    private final NamespacedKey durationKey;
    private final NamespacedKey rarityKey;
    private final Map<String, TrinketTemplate> templates = new ConcurrentHashMap<>();
    private final Map<UUID, ActiveTrinketEffect> activeEffects = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public TrinketManager(Main plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.trinketIdKey = new NamespacedKey(plugin, "trinket_id");
        this.magnitudeKey = new NamespacedKey(plugin, "trinket_magnitude");
        this.durationKey = new NamespacedKey(plugin, "trinket_duration");
        this.rarityKey = new NamespacedKey(plugin, "trinket_rarity");
        load(config);
    }

    private void load(FileConfiguration config) {
        templates.clear();
        if (config == null) {
            plugin.getLogger().warning("Trinket configuration is missing.");
            return;
        }
        ConfigurationSection root = config.getConfigurationSection("trinkets");
        if (root == null) {
            plugin.getLogger().warning("No trinkets section found in trinkets.yml");
            return;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) continue;
            try {
                String id = section.getString("id", key);
                String name = section.getString("name", id);
                String materialName = section.getString("material", "STONE");
                Material material = Material.matchMaterial(materialName.toUpperCase());
                if (material == null) {
                    plugin.getLogger().warning("Unknown material for trinket " + id + ": " + materialName);
                    material = Material.STONE;
                }
                String model = section.getString("model", "");
                List<String> description = section.getStringList("description");
                ConfigurationSection effectSection = section.getConfigurationSection("effect");
                if (effectSection == null) {
                    plugin.getLogger().warning("Missing effect block for trinket " + id);
                    continue;
                }
                String typeName = effectSection.getString("type", "COOLDOWN_REDUCTION");
                TrinketEffectType type = TrinketEffectType.valueOf(typeName.toUpperCase());
                NumberRange magnitudeRange = readRange(effectSection, "magnitudeRange", "magnitude", 0.0, id);
                NumberRange durationRange = readRange(effectSection, "durationRange", "duration", 3.0, id);
                int cooldownSeconds = Math.max(0, section.getInt("cooldownSeconds", 30));
                TrinketTemplate template = new TrinketTemplate(id, name, material, model, description, type,
                        magnitudeRange, durationRange, cooldownSeconds);
                templates.put(id, template);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Failed to load trinket " + key + ": " + ex.getMessage());
            }
        }
        plugin.getLogger().info("Loaded " + templates.size() + " trinket templates.");
    }

    private NumberRange readRange(ConfigurationSection section,
                                  String rangeKey,
                                  String valueKey,
                                  double defaultValue,
                                  String trinketId) {
        Object rawRange = section.get(rangeKey);
        if (rawRange != null) {
            try {
                return NumberRange.coerce(rawRange, defaultValue);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Invalid " + rangeKey + " for trinket " + trinketId + ": " + ex.getMessage());
            }
        }
        Object rawValue = section.get(valueKey);
        if (rawValue != null) {
            try {
                return NumberRange.coerce(rawValue, defaultValue);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Invalid " + valueKey + " for trinket " + trinketId + ": " + ex.getMessage());
            }
        }
        return NumberRange.fixed(defaultValue);
    }

    private TrinketEffectDefinition resolveEffect(ItemStack stack, TrinketTemplate template) {
        if (stack == null) {
            return template.getDefaultEffect();
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return template.getDefaultEffect();
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        Double magnitude = container.get(magnitudeKey, PersistentDataType.DOUBLE);
        Double duration = container.get(durationKey, PersistentDataType.DOUBLE);
        if (magnitude == null || duration == null) {
            return template.getDefaultEffect();
        }
        return template.createEffect(magnitude, duration);
    }

    public Collection<TrinketTemplate> getTemplates() {
        return Collections.unmodifiableCollection(templates.values());
    }

    public Optional<TrinketTemplate> getTemplate(String id) {
        return Optional.ofNullable(templates.get(id));
    }

    public ItemStack createItem(String id) {
        TrinketTemplate template = templates.get(id);
        if (template == null) return null;
        return template.createItemStack(trinketIdKey, magnitudeKey, durationKey, rarityKey);
    }

    public boolean isTrinket(ItemStack stack) {
        return getTrinketId(stack).isPresent();
    }

    public Optional<String> getTrinketId(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return Optional.empty();
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return Optional.empty();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(trinketIdKey, PersistentDataType.STRING)) return Optional.empty();
        return Optional.ofNullable(pdc.get(trinketIdKey, PersistentDataType.STRING));
    }

    public boolean trigger(Player player) {
        return trigger(player, player.getInventory().getItemInOffHand());
    }

    public boolean trigger(Player player, ItemStack stack) {
        Optional<String> idOpt = getTrinketId(stack);
        if (idOpt.isEmpty()) {
            return false;
        }
        TrinketTemplate template = templates.get(idOpt.get());
        if (template == null) {
            return false;
        }
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long readyAt = cooldowns.get(uuid);
        if (readyAt != null && readyAt > now) {
            long remaining = (readyAt - now + 999) / 1000;
            player.sendMessage("§cTrinket is on cooldown for " + remaining + "s.");
            return true;
        }
        endEffect(uuid, false);
        TrinketEffectDefinition effect = resolveEffect(stack, template);
        TrinketEffectType type = effect.getType();
        long durationTicks = Math.max(1L, Math.round(effect.getDurationSeconds() * 20.0));
        long expiresAt = now + (long) (effect.getDurationSeconds() * 1000.0);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> expire(uuid), durationTicks);
        ActiveTrinketEffect active = new ActiveTrinketEffect(uuid, template, effect, expiresAt, task);
        activeEffects.put(uuid, active);
        type.onActivate(player, effect, active);
        cooldowns.put(uuid, now + template.getCooldownSeconds() * 1000L);
        player.sendMessage("§aActivated " + template.getFormattedName());
        return true;
    }

    public void expire(UUID playerId) {
        ActiveTrinketEffect active = activeEffects.remove(playerId);
        if (active == null) return;
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            active.getEffect().getType().onExpire(player, active.getEffect(), active);
            player.sendMessage("§e" + active.getTemplate().getFormattedName() + " has faded.");
        }
        active.cancel();
    }

    public void endEffect(UUID playerId, boolean silent) {
        ActiveTrinketEffect active = activeEffects.remove(playerId);
        if (active == null) return;
        active.cancel();
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            active.getEffect().getType().onExpire(player, active.getEffect(), active);
            if (!silent) {
                player.sendMessage("§e" + active.getTemplate().getFormattedName() + " has ended.");
            }
        }
    }

    public void applySpellModifiers(Player player, SpellCastContext ctx) {
        ActiveTrinketEffect active = activeEffects.get(player.getUniqueId());
        if (active == null) return;
        if (System.currentTimeMillis() > active.getExpiresAt()) {
            expire(player.getUniqueId());
            return;
        }
        active.getEffect().getType().applySpellContext(ctx, active.getEffect(), player, active);
    }

    public double modifyOutgoingDamage(Player player, double damage) {
        ActiveTrinketEffect active = activeEffects.get(player.getUniqueId());
        if (active == null) return damage;
        if (System.currentTimeMillis() > active.getExpiresAt()) {
            expire(player.getUniqueId());
            return damage;
        }
        return active.getEffect().getType().modifyOutgoingDamage(damage, player, active.getEffect(), active);
    }

    public boolean shouldCancelDamage(Player player) {
        ActiveTrinketEffect active = activeEffects.get(player.getUniqueId());
        if (active == null) return false;
        if (System.currentTimeMillis() > active.getExpiresAt()) {
            expire(player.getUniqueId());
            return false;
        }
        return active.getEffect().getType().cancelIncomingDamage(player, active.getEffect(), active);
    }

    public void clear(Player player) {
        endEffect(player.getUniqueId(), true);
        cooldowns.remove(player.getUniqueId());
    }

    public long getRemainingCooldown(Player player) {
        Long ready = cooldowns.get(player.getUniqueId());
        if (ready == null) return 0L;
        long now = System.currentTimeMillis();
        return Math.max(0L, ready - now);
    }
}
