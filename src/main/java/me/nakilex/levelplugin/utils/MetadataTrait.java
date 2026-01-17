package me.nakilex.levelplugin.utils;

import net.citizensnpcs.api.persistence.DataKey;
import net.citizensnpcs.api.trait.Trait;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class MetadataTrait extends Trait {
    private static final String STORAGE_KEY = "metadata";
    private final Map<String, String> metadata = new HashMap<>();

    public MetadataTrait() {
        super("MetadataTrait");
    }

    public Map<String, String> getAll() {
        return Collections.unmodifiableMap(metadata);
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(metadata.get(key));
    }

    public void set(String key, String value) {
        metadata.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
    }

    public void remove(String key) {
        metadata.remove(key);
    }

    public void clear() {
        metadata.clear();
    }

    @Override
    public void load(DataKey key) {
        metadata.clear();
        DataKey storage = key.getRelative(STORAGE_KEY);
        for (DataKey entryKey : storage.getSubKeys()) {
            String entryName = entryKey.name();
            metadata.put(entryName, storage.getString(entryName));
        }
    }

    @Override
    public void save(DataKey key) {
        DataKey storage = key.getRelative(STORAGE_KEY);
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            storage.setString(entry.getKey(), entry.getValue());
        }
    }
}
