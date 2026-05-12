package me.nakilex.levelplugin.spells.input;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.config.PlayerConfig;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpellKeybindManager {
    private static final SpellKeybindManager instance = new SpellKeybindManager();

    public static SpellKeybindManager getInstance() {
        return instance;
    }

    private final Map<UUID, Map<PlayerClass, EnumMap<SpellInputMode, EnumMap<SpellKeybindSlot, SpellInputType>>>> bindings =
            new ConcurrentHashMap<>();

    public EnumMap<SpellKeybindSlot, SpellInputType> getBindings(UUID playerId, PlayerClass playerClass,
                                                                 SpellInputMode mode) {
        EnumMap<SpellKeybindSlot, SpellInputType> existing = getOrCreateBindings(playerId, playerClass, mode);
        return new EnumMap<>(existing);
    }

    public SpellInputType getBinding(UUID playerId, PlayerClass playerClass, SpellInputMode mode,
                                     SpellKeybindSlot slot) {
        if (playerId == null || mode == null || slot == null) {
            return null;
        }
        if (playerClass == null) {
            return defaultBindings().get(slot);
        }
        EnumMap<SpellKeybindSlot, SpellInputType> map = getOrCreateBindings(playerId, playerClass, mode);
        return map.getOrDefault(slot, defaultBindings().get(slot));
    }

    public void setBinding(UUID playerId, PlayerClass playerClass, SpellInputMode mode,
                           SpellKeybindSlot slot, SpellInputType type) {
        if (playerId == null || playerClass == null || mode == null || slot == null) {
            return;
        }
        EnumMap<SpellKeybindSlot, SpellInputType> map = getOrCreateBindings(playerId, playerClass, mode);
        if (type == null) {
            map.remove(slot);
        } else {
            map.put(slot, type);
        }
    }

    public void setBindings(UUID playerId, PlayerClass playerClass, SpellInputMode mode,
                            Map<SpellKeybindSlot, SpellInputType> newBindings) {
        if (playerId == null || playerClass == null || mode == null) {
            return;
        }
        EnumMap<SpellKeybindSlot, SpellInputType> target = getOrCreateBindings(playerId, playerClass, mode);
        target.clear();
        if (newBindings != null) {
            target.putAll(newBindings);
        }
        ensureDefaultBindings(target);
    }


    public void saveProfileBindings(UUID playerId, int slot) {
        if (playerId == null || slot < 0) {
            return;
        }
        PlayerConfig config = Main.getInstance().getPlayerConfig();
        for (PlayerClass playerClass : PlayerClass.values()) {
            for (SpellInputMode mode : SpellInputMode.values()) {
                config.setProfileSpellKeybinds(playerId, slot, playerClass, mode,
                        getBindings(playerId, playerClass, mode));
            }
        }
        Bukkit.getLogger().info("[LevelPlugin][SpellKeybindManager] Saved spell keybinds for player="
                + playerId + " slot=" + slot);
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            SpellInputHudManager.getInstance().sync(player);
        }
    }

    public void loadProfileBindings(UUID playerId, int slot) {
        if (playerId == null || slot < 0) {
            return;
        }
        for (PlayerClass playerClass : PlayerClass.values()) {
            for (SpellInputMode mode : SpellInputMode.values()) {
                setBindings(playerId, playerClass, mode,
                        Main.getInstance().getPlayerConfig().getProfileSpellKeybinds(playerId, slot, playerClass, mode));
            }
        }
        Bukkit.getLogger().info("[LevelPlugin][SpellKeybindManager] Loaded spell keybinds for player="
                + playerId + " slot=" + slot);
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            SpellInputHudManager.getInstance().sync(player);
        }
    }

    private EnumMap<SpellKeybindSlot, SpellInputType> getOrCreateBindings(UUID playerId, PlayerClass playerClass,
                                                                          SpellInputMode mode) {
        EnumMap<SpellKeybindSlot, SpellInputType> map = bindings
                .computeIfAbsent(playerId, id -> new ConcurrentHashMap<>())
                .computeIfAbsent(playerClass, cls -> new EnumMap<>(SpellInputMode.class))
                .computeIfAbsent(mode, ignored -> defaultBindings());
        ensureDefaultBindings(map);
        return map;
    }

    private static void ensureDefaultBindings(EnumMap<SpellKeybindSlot, SpellInputType> map) {
        if (map == null) {
            return;
        }
        EnumMap<SpellKeybindSlot, SpellInputType> defaults = defaultBindings();
        for (Map.Entry<SpellKeybindSlot, SpellInputType> entry : defaults.entrySet()) {
            map.putIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    private static EnumMap<SpellKeybindSlot, SpellInputType> defaultBindings() {
        EnumMap<SpellKeybindSlot, SpellInputType> defaults = new EnumMap<>(SpellKeybindSlot.class);
        defaults.put(SpellKeybindSlot.SLOT_1, SpellInputType.SPELL_1);
        defaults.put(SpellKeybindSlot.SLOT_2, SpellInputType.SPELL_2);
        defaults.put(SpellKeybindSlot.SLOT_3, SpellInputType.SPELL_3);
        defaults.put(SpellKeybindSlot.SLOT_4, SpellInputType.SPELL_4);
        return defaults;
    }
}
