package me.nakilex.npc.core.persistence;

import me.nakilex.npc.core.registry.DefaultNpcRegistry;

import java.io.File;
import java.util.Collection;

public interface NpcRepository {
    void load(DefaultNpcRegistry registry, File file);

    void save(DefaultNpcRegistry registry, File file);

    void exportData(DefaultNpcRegistry registry, File file);

    void importData(DefaultNpcRegistry registry, File file);

    Collection<String> getKnownVersions();
}
