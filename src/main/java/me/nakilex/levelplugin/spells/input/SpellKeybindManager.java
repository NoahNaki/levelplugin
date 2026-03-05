package me.nakilex.levelplugin.spells.input;

import me.nakilex.levelplugin.player.classes.data.PlayerClass;

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
        if (playerId == null || playerClass == null || mode == null || slot == null) {
            return null;
        }
        EnumMap<SpellKeybindSlot, SpellInputType> map = getOrCreateBindings(playerId, playerClass, mode);
        return map.get(slot);
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
    }

    public Map<PlayerClass, EnumMap<SpellInputMode, EnumMap<SpellKeybindSlot, SpellInputType>>>
    getAllBindings(UUID playerId) {
        if (playerId == null) {
            return Map.of();
        }
        Map<PlayerClass, EnumMap<SpellInputMode, EnumMap<SpellKeybindSlot, SpellInputType>>> playerBindings =
                bindings.get(playerId);
        if (playerBindings == null) {
            return Map.of();
        }
        Map<PlayerClass, EnumMap<SpellInputMode, EnumMap<SpellKeybindSlot, SpellInputType>>> snapshot =
                new EnumMap<>(PlayerClass.class);
        for (Map.Entry<PlayerClass, EnumMap<SpellInputMode, EnumMap<SpellKeybindSlot, SpellInputType>>> classEntry
                : playerBindings.entrySet()) {
            EnumMap<SpellInputMode, EnumMap<SpellKeybindSlot, SpellInputType>> modeMap =
                    new EnumMap<>(SpellInputMode.class);
            for (Map.Entry<SpellInputMode, EnumMap<SpellKeybindSlot, SpellInputType>> modeEntry
                    : classEntry.getValue().entrySet()) {
                modeMap.put(modeEntry.getKey(), new EnumMap<>(modeEntry.getValue()));
            }
            snapshot.put(classEntry.getKey(), modeMap);
        }
        return snapshot;
    }

    public void replaceAllBindings(UUID playerId,
                                   Map<PlayerClass, EnumMap<SpellInputMode, EnumMap<SpellKeybindSlot, SpellInputType>>> loadedBindings) {
        if (playerId == null) {
            return;
        }
        if (loadedBindings == null || loadedBindings.isEmpty()) {
            bindings.remove(playerId);
            return;
        }
        Map<PlayerClass, EnumMap<SpellInputMode, EnumMap<SpellKeybindSlot, SpellInputType>>> normalized =
                new EnumMap<>(PlayerClass.class);
        for (Map.Entry<PlayerClass, EnumMap<SpellInputMode, EnumMap<SpellKeybindSlot, SpellInputType>>> classEntry
                : loadedBindings.entrySet()) {
            if (classEntry.getKey() == null || classEntry.getValue() == null) {
                continue;
            }
            EnumMap<SpellInputMode, EnumMap<SpellKeybindSlot, SpellInputType>> modeMap =
                    new EnumMap<>(SpellInputMode.class);
            for (Map.Entry<SpellInputMode, EnumMap<SpellKeybindSlot, SpellInputType>> modeEntry
                    : classEntry.getValue().entrySet()) {
                if (modeEntry.getKey() == null) {
                    continue;
                }
                EnumMap<SpellKeybindSlot, SpellInputType> slotMap = defaultBindings();
                slotMap.clear();
                if (modeEntry.getValue() != null) {
                    for (Map.Entry<SpellKeybindSlot, SpellInputType> slotEntry : modeEntry.getValue().entrySet()) {
                        if (slotEntry.getKey() != null && slotEntry.getValue() != null) {
                            slotMap.put(slotEntry.getKey(), slotEntry.getValue());
                        }
                    }
                }
                modeMap.put(modeEntry.getKey(), slotMap);
            }
            if (!modeMap.isEmpty()) {
                normalized.put(classEntry.getKey(), modeMap);
            }
        }
        if (normalized.isEmpty()) {
            bindings.remove(playerId);
            return;
        }
        bindings.put(playerId, new ConcurrentHashMap<>(normalized));
    }

    private EnumMap<SpellKeybindSlot, SpellInputType> getOrCreateBindings(UUID playerId, PlayerClass playerClass,
                                                                          SpellInputMode mode) {
        return bindings
                .computeIfAbsent(playerId, id -> new ConcurrentHashMap<>())
                .computeIfAbsent(playerClass, cls -> new EnumMap<>(SpellInputMode.class))
                .computeIfAbsent(mode, ignored -> defaultBindings());
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
