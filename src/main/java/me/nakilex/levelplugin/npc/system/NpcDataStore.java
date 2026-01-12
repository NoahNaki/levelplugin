package me.nakilex.levelplugin.npc.system;

import java.util.HashMap;
import java.util.Map;

public class NpcDataStore {
    private final Map<String, String> data = new HashMap<>();

    public String get(String key) {
        return data.get(key);
    }

    public void set(String key, String value) {
        if (value == null) {
            data.remove(key);
        } else {
            data.put(key, value);
        }
    }

    public Map<String, String> snapshot() {
        return new HashMap<>(data);
    }
}
