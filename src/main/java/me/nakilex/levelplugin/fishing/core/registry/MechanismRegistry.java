package me.nakilex.levelplugin.fishing.core.registry;

import me.nakilex.levelplugin.fishing.api.FishingMechanism;

import java.util.HashMap;
import java.util.Map;

public class MechanismRegistry {
    private final Map<String, FishingMechanism> mechanisms = new HashMap<>();

    public MechanismRegistry() {
        for (FishingMechanism mechanism : FishingMechanism.values()) {
            register(mechanism.name(), mechanism);
        }
    }

    public void register(String key, FishingMechanism mechanism) {
        if (key == null || mechanism == null) {
            return;
        }
        mechanisms.put(key.toLowerCase(), mechanism);
    }

    public FishingMechanism get(String key) {
        if (key == null) {
            return null;
        }
        return mechanisms.get(key.toLowerCase());
    }
}
