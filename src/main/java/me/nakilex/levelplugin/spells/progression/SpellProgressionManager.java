package me.nakilex.levelplugin.spells.progression;

import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.managers.PlayerClassManager;
import me.nakilex.levelplugin.player.profile.ProfileManager;
import me.nakilex.levelplugin.spells.SpellProgression;
import me.nakilex.levelplugin.spells.SpellRegistry;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class SpellProgressionManager {
    private static final SpellProgressionManager INSTANCE = new SpellProgressionManager();

    public static SpellProgressionManager getInstance() {
        return INSTANCE;
    }

    private record ProfileKey(UUID playerId, int slot) {
    }

    private final Map<ProfileKey, Integer> spellPoints = new HashMap<>();
    private final Map<ProfileKey, Map<String, Integer>> spellLevels = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> temporarySpellLevels = new HashMap<>();

    private SpellProgressionManager() {
    }

    public int getSpellPoints(UUID playerId) {
        return spellPoints.getOrDefault(resolveKey(playerId), 0);
    }

    public void setSpellPoints(UUID playerId, int amount) {
        spellPoints.put(resolveKey(playerId), Math.max(0, amount));
    }

    public void addSpellPoints(UUID playerId, int amount) {
        if (amount == 0) {
            return;
        }
        setSpellPoints(playerId, getSpellPoints(playerId) + amount);
    }

    public int getSpellLevel(UUID playerId, String baseSpellId) {
        if (playerId == null || baseSpellId == null) {
            return 0;
        }
        int permanent = getPermanentSpellLevel(playerId, baseSpellId);
        int temporary = temporarySpellLevels.getOrDefault(playerId, Map.of()).getOrDefault(normalize(baseSpellId), 0);
        return Math.max(0, Math.min(permanent + temporary, getMaxLevel(baseSpellId)));
    }

    private int getPermanentSpellLevel(UUID playerId, String baseSpellId) {
        if (playerId == null || baseSpellId == null) {
            return 0;
        }
        return spellLevels.getOrDefault(resolveKey(playerId), Map.of()).getOrDefault(normalize(baseSpellId), 0);
    }

    public int getTemporarySpellLevel(UUID playerId, String baseSpellId) {
        if (playerId == null || baseSpellId == null) {
            return 0;
        }
        return temporarySpellLevels.getOrDefault(playerId, Map.of()).getOrDefault(normalize(baseSpellId), 0);
    }

    public boolean addTemporarySpellLevel(UUID playerId, String baseSpellId, int amount) {
        if (playerId == null || baseSpellId == null || amount <= 0) {
            return false;
        }
        int current = getSpellLevel(playerId, baseSpellId);
        int max = getMaxLevel(baseSpellId);
        if (current >= max) {
            return false;
        }
        String normalized = normalize(baseSpellId);
        int add = Math.min(amount, max - current);
        temporarySpellLevels.computeIfAbsent(playerId, ignored -> new HashMap<>())
                .merge(normalized, add, Integer::sum);
        return true;
    }

    public void clearTemporarySpellLevels(UUID playerId) {
        if (playerId != null) {
            temporarySpellLevels.remove(playerId);
        }
    }

    public int getMaxLevel(String baseSpellId) {
        SpellProgression progression = SpellRegistry.getInstance().getProgression(baseSpellId);
        return progression == null ? 0 : progression.upgradeSpellIds().size();
    }

    public boolean investPoint(UUID playerId, String baseSpellId) {
        if (playerId == null || baseSpellId == null) {
            return false;
        }
        int points = getSpellPoints(playerId);
        if (points <= 0) {
            return false;
        }
        int level = getPermanentSpellLevel(playerId, baseSpellId);
        int max = getMaxLevel(baseSpellId);
        if (level >= max) {
            return false;
        }
        ProfileKey key = resolveKey(playerId);
        Map<String, Integer> levels = spellLevels.computeIfAbsent(key, id -> new HashMap<>());
        levels.put(normalize(baseSpellId), level + 1);
        spellPoints.put(key, points - 1);
        return true;
    }

    public boolean refundPoint(UUID playerId, String baseSpellId) {
        if (playerId == null || baseSpellId == null) {
            return false;
        }
        int level = getPermanentSpellLevel(playerId, baseSpellId);
        if (level <= 0) {
            return false;
        }
        ProfileKey key = resolveKey(playerId);
        Map<String, Integer> levels = spellLevels.computeIfAbsent(key, id -> new HashMap<>());
        if (level == 1) {
            levels.remove(normalize(baseSpellId));
        } else {
            levels.put(normalize(baseSpellId), level - 1);
        }
        spellPoints.put(key, getSpellPoints(playerId) + 1);
        return true;
    }

    public void clearProfile(UUID playerId, int slot) {
        if (playerId == null || slot < 0) {
            return;
        }
        ProfileKey key = new ProfileKey(playerId, slot);
        spellPoints.remove(key);
        spellLevels.remove(key);
    }

    public void clearPlayer(UUID playerId) {
        if (playerId == null) {
            return;
        }
        spellPoints.keySet().removeIf(key -> key.playerId().equals(playerId));
        spellLevels.keySet().removeIf(key -> key.playerId().equals(playerId));
        temporarySpellLevels.remove(playerId);
    }

    public void loadProfileData(UUID playerId, int slot, int points, List<String> levelEntries) {
        clearProfile(playerId, slot);
        ProfileKey key = new ProfileKey(playerId, slot);
        spellPoints.put(key, Math.max(0, points));
        if (levelEntries == null) {
            return;
        }
        for (String line : levelEntries) {
            if (line == null || !line.contains(":")) {
                continue;
            }
            String[] parts = line.split(":", 2);
            if (parts.length != 2) {
                continue;
            }
            int level;
            try {
                level = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ex) {
                continue;
            }
            int capped = Math.max(0, Math.min(level, getMaxLevel(parts[0])));
            if (capped > 0) {
                spellLevels.computeIfAbsent(key, id -> new HashMap<>()).put(normalize(parts[0]), capped);
            }
        }
    }

    public int getSpellPoints(UUID playerId, int slot) {
        return spellPoints.getOrDefault(new ProfileKey(playerId, slot), 0);
    }

    public List<String> serializeSpellLevels(UUID playerId, int slot) {
        ProfileKey key = new ProfileKey(playerId, slot);
        Map<String, Integer> levels = spellLevels.getOrDefault(key, Map.of());
        List<String> values = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : levels.entrySet()) {
            if (entry.getValue() > 0) {
                values.add(entry.getKey() + ":" + entry.getValue());
            }
        }
        return values;
    }

    public String getEffectiveSpellId(UUID playerId, String baseSpellId) {
        if (baseSpellId == null) {
            return null;
        }
        SpellProgression progression = SpellRegistry.getInstance().getProgression(baseSpellId);
        if (progression == null || progression.upgradeSpellIds().isEmpty()) {
            return normalize(baseSpellId);
        }
        int level = Math.max(0, Math.min(getSpellLevel(playerId, baseSpellId), progression.upgradeSpellIds().size()));
        if (level == 0) {
            return normalize(baseSpellId);
        }
        return normalize(progression.upgradeSpellIds().get(level - 1));
    }

    public List<String> getClassBaseSpells(Player player) {
        if (player == null) {
            return List.of();
        }
        PlayerClass playerClass = PlayerClassManager.getInstance().getPlayerClass(player);
        if (playerClass == null) {
            return List.of();
        }
        List<String> spellIds = new ArrayList<>();
        for (SpellProgression progression : SpellRegistry.getInstance().getAllProgressions()) {
            String id = progression.baseSpellId();
            SpellRegistry.SpellEntry entry = SpellRegistry.getInstance().getSpell(id);
            if (entry == null) {
                continue;
            }
            if (isBoundForClass(playerClass, id)) {
                spellIds.add(id);
            }
        }
        spellIds.sort(String::compareToIgnoreCase);
        return spellIds;
    }

    private boolean isBoundForClass(PlayerClass playerClass, String spellId) {
        return SpellRegistry.getInstance().isSpellBoundForClass(spellId, playerClass);
    }

    private String normalize(String spellId) {
        return spellId.toLowerCase(Locale.ROOT);
    }

    private ProfileKey resolveKey(UUID playerId) {
        int slot = 0;
        if (playerId != null) {
            Integer active = ProfileManager.getInstance().getActiveSlot(playerId);
            if (active != null && active >= 0) {
                slot = active;
            }
        }
        return new ProfileKey(playerId, slot);
    }
}
