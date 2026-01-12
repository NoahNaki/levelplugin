package me.nakilex.npc.core.persistence;

import me.nakilex.npc.core.model.Npc;
import me.nakilex.npc.core.registry.DefaultNpcRegistry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class YamlNpcRepository implements NpcRepository {
    private static final String VERSION = "1";
    private final NpcYamlAdapter adapter = new NpcYamlAdapter();

    @Override
    public void load(DefaultNpcRegistry registry, File file) {
        registry.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection npcsSection = config.getConfigurationSection("npcs");
        if (npcsSection != null) {
            for (String key : npcsSection.getKeys(false)) {
                ConfigurationSection npcSection = npcsSection.getConfigurationSection(key);
                if (npcSection == null) {
                    continue;
                }
                Npc npc = adapter.deserialize(npcSection);
                if (npc != null) {
                    registry.register(npc);
                }
            }
        }
        ConfigurationSection selectionSection = config.getConfigurationSection("selections");
        if (selectionSection != null) {
            Map<UUID, Integer> selections = selectionSection.getValues(false).entrySet().stream()
                    .filter(entry -> entry.getValue() instanceof Number)
                    .collect(Collectors.toMap(entry -> UUID.fromString(entry.getKey()),
                            entry -> ((Number) entry.getValue()).intValue()));
            registry.setSelections(selections);
        }
    }

    @Override
    public void save(DefaultNpcRegistry registry, File file) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("version", VERSION);
        ConfigurationSection npcSection = config.createSection("npcs");
        for (Npc npc : registry.list()) {
            npcSection.createSection(String.valueOf(npc.getId()), adapter.serialize(npc));
        }
        ConfigurationSection selections = config.createSection("selections");
        registry.getSelectionsSnapshot().forEach((uuid, id) -> selections.set(uuid.toString(), id));
        try {
            config.save(file);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to save NPC data", ex);
        }
    }

    @Override
    public void exportData(DefaultNpcRegistry registry, File file) {
        save(registry, file);
    }

    @Override
    public void importData(DefaultNpcRegistry registry, File file) {
        load(registry, file);
    }

    @Override
    public Collection<String> getKnownVersions() {
        return List.of(VERSION);
    }
}
