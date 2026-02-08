package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.spells.input.SpellInputMode;
import me.nakilex.levelplugin.spells.input.SpellInputType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SpellRegistry {
    private static final SpellRegistry INSTANCE = new SpellRegistry();

    public static SpellRegistry getInstance() {
        return INSTANCE;
    }

    public record SpellEntry(SpellDefinition definition, SpellHandler handler) {
    }

    private final Map<String, SpellEntry> spells = new HashMap<>();
    private final List<SpellBinding> bindings = new ArrayList<>();
    private final Map<String, SpellProgression> progressions = new HashMap<>();

    private SpellRegistry() {
    }

    public void registerSpell(SpellDefinition definition, SpellHandler handler) {
        if (definition == null || handler == null) {
            return;
        }
        spells.put(definition.id().toLowerCase(Locale.ROOT), new SpellEntry(definition, handler));
    }

    public void registerBinding(SpellBinding binding) {
        if (binding == null) {
            return;
        }
        bindings.add(binding);
    }

    public void registerProgression(SpellProgression progression) {
        if (progression == null) {
            return;
        }
        progressions.put(progression.baseSpellId().toLowerCase(Locale.ROOT), progression);
    }

    public SpellEntry resolveSpell(PlayerClass playerClass,
                                   SpellInputMode mode,
                                   String sequence,
                                   SpellInputType inputType) {
        for (SpellBinding binding : bindings) {
            if (!binding.matches(playerClass, mode, sequence, inputType)) {
                continue;
            }
            SpellEntry entry = spells.get(binding.spellId().toLowerCase(Locale.ROOT));
            if (entry != null) {
                return entry;
            }
        }
        return null;
    }

    public SpellProgression getProgression(String spellId) {
        if (spellId == null) {
            return null;
        }
        return progressions.get(spellId.toLowerCase(Locale.ROOT));
    }
}
