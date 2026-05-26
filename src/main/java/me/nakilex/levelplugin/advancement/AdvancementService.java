package me.nakilex.levelplugin.advancement;

import me.nakilex.levelplugin.advancement.events.AdvancementProgressionUpdateEvent;
import me.nakilex.levelplugin.advancement.model.*;
import me.nakilex.levelplugin.advancement.persistence.AdvancementDatabase;
import me.nakilex.levelplugin.advancement.persistence.TeamProgression;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AdvancementService {
    private final AdvancementDatabase database;
    private final Map<String, AdvancementTab> tabs = new ConcurrentHashMap<>();

    public AdvancementService(AdvancementDatabase database) { this.database = database; }
    public void registerTab(AdvancementTab tab) {
        if (tabs.putIfAbsent(tab.namespace(), tab) != null) throw new IllegalArgumentException("Duplicate namespace");
    }
    public Collection<AdvancementTab> tabs() { return Collections.unmodifiableCollection(tabs.values()); }
    public Optional<Advancement> find(AdvancementKey key) { return Optional.ofNullable(tabs.get(key.namespace())).flatMap(t -> t.byKey(key)); }
    public int getProgression(UUID teamId, AdvancementKey key) { return database.loadTeam(teamId).get(key); }
    public int setProgression(UUID teamId, AdvancementKey key, int newProgress) {
        TeamProgression team = database.loadTeam(teamId);
        int old = database.updateProgression(key, team, newProgress);
        Bukkit.getPluginManager().callEvent(new AdvancementProgressionUpdateEvent(teamId, key, old, newProgress));
        return old;
    }
}
