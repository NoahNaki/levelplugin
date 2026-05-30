package me.nakilex.levelplugin.npc.dialog.model;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.npc.dialog.DialogueInteraction;
import me.nakilex.levelplugin.quests.data.Quest;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Shared state for one NPC dialogue interaction. */
public final class InteractionContext {
    private final Main plugin;
    private final Player player;
    private final DialogNpcRef npc;
    private final Quest quest;
    private final Runnable finish;
    private final Map<String, Object> variables = new HashMap<>();
    private DialogueInteraction interaction;

    public InteractionContext(Main plugin, Player player, DialogNpcRef npc, Quest quest, Runnable finish) {
        this.plugin = plugin;
        this.player = player;
        this.npc = npc;
        this.quest = quest;
        this.finish = finish;
    }

    public Main plugin() { return plugin; }
    public Player player() { return player; }
    public DialogNpcRef npc() { return npc; }
    public Quest quest() { return quest; }
    public Runnable finish() { return finish; }
    public Optional<DialogueInteraction> interaction() { return Optional.ofNullable(interaction); }
    public void attachInteraction(DialogueInteraction interaction) { this.interaction = interaction; }

    public void set(String key, Object value) {
        if (key == null || key.isBlank()) return;
        if (value == null) variables.remove(key);
        else variables.put(key, value);
    }

    public Optional<Object> get(String key) {
        return Optional.ofNullable(variables.get(key));
    }

    public Optional<Integer> getInt(String key) {
        Object value = variables.get(key);
        return value instanceof Number number ? Optional.of(number.intValue()) : Optional.empty();
    }

    public Optional<String> getString(String key) {
        Object value = variables.get(key);
        return value == null ? Optional.empty() : Optional.of(String.valueOf(value));
    }

    public <T> void set(ContextKey<T> key, T value) {
        if (key == null) return;
        set(key.id(), value);
    }

    public <T> Optional<T> get(ContextKey<T> key) {
        if (key == null) return Optional.empty();
        Object value = variables.get(key.id());
        return key.type().isInstance(value) ? Optional.of(key.type().cast(value)) : Optional.empty();
    }

    public Map<String, Object> variables() {
        return Collections.unmodifiableMap(variables);
    }
}
