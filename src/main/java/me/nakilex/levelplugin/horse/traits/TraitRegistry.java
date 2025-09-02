package me.nakilex.levelplugin.horse.traits;

import java.util.*;

/**
 * Simple registry for available {@link HorseTrait}s.
 * Keeps things extensible so additional traits can be added later
 * without touching the activation logic.
 */
public final class TraitRegistry {
    private static final Map<String, HorseTrait> TRAITS = new HashMap<>();

    private TraitRegistry() {}

    static {
        // Register default traits here
        register(new DashTrait());
        register(new LeapTrait());
        register(new GhostTrait());
        register(new KickbackTrait());
    }

    public static void register(HorseTrait trait) {
        TRAITS.put(trait.getId(), trait);
    }

    public static HorseTrait get(String id) {
        return TRAITS.get(id);
    }

    /**
     * @return an immutable set of registered trait IDs.
     */
    public static Set<String> getTraitIds() {
        return Collections.unmodifiableSet(TRAITS.keySet());
    }

    /**
     * Picks a random trait ID using the given random instance.
     */
    public static String getRandomId(Random random) {
        if (TRAITS.isEmpty()) return null;
        int index = random.nextInt(TRAITS.size());
        return new ArrayList<>(TRAITS.keySet()).get(index);
    }
}
