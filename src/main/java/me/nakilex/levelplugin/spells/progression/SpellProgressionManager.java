package me.nakilex.levelplugin.spells.progression;

import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.managers.PlayerClassManager;
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

    private final Map<UUID, Integer> spellPoints = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> spellLevels = new HashMap<>();

    private SpellProgressionManager() {
    }

    public int getSpellPoints(UUID playerId) {
        return spellPoints.getOrDefault(playerId, 0);
    }

    public void setSpellPoints(UUID playerId, int amount) {
        spellPoints.put(playerId, Math.max(0, amount));
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
        return spellLevels.getOrDefault(playerId, Map.of()).getOrDefault(normalize(baseSpellId), 0);
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
        int level = getSpellLevel(playerId, baseSpellId);
        int max = getMaxLevel(baseSpellId);
        if (level >= max) {
            return false;
        }
        Map<String, Integer> levels = spellLevels.computeIfAbsent(playerId, id -> new HashMap<>());
        levels.put(normalize(baseSpellId), level + 1);
        spellPoints.put(playerId, points - 1);
        return true;
    }

    public boolean refundPoint(UUID playerId, String baseSpellId) {
        if (playerId == null || baseSpellId == null) {
            return false;
        }
        int level = getSpellLevel(playerId, baseSpellId);
        if (level <= 0) {
            return false;
        }
        Map<String, Integer> levels = spellLevels.computeIfAbsent(playerId, id -> new HashMap<>());
        if (level == 1) {
            levels.remove(normalize(baseSpellId));
        } else {
            levels.put(normalize(baseSpellId), level - 1);
        }
        spellPoints.put(playerId, getSpellPoints(playerId) + 1);
        return true;
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
}
