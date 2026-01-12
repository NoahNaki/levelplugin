package me.nakilex.npc.core.trait;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TraitRegistry {
    private final Map<String, Trait> traits = new HashMap<>();

    public void register(Trait trait) {
        traits.put(trait.getId().toLowerCase(), trait);
    }

    public Optional<Trait> get(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(traits.get(id.toLowerCase()));
    }

    public Collection<Trait> list() {
        return Collections.unmodifiableCollection(traits.values());
    }
}
