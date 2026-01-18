package me.nakilex.levelplugin.utils;

import net.citizensnpcs.api.persistence.Persist;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@TraitName("MetadataTrait")
public class MetadataTrait extends Trait {
    @Persist("metadata")
    private final Map<String, String> metadata = new HashMap<>();

    public MetadataTrait() {
        super("MetadataTrait");
    }

    public void set(String key, String value) {
        if (value == null) {
            metadata.remove(key);
            return;
        }
        metadata.put(key, value);
    }

    public String get(String key) {
        return metadata.get(key);
    }

    public void remove(String key) {
        metadata.remove(key);
    }

    public Map<String, String> getAll() {
        return Collections.unmodifiableMap(metadata);
    }
}
