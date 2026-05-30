package me.nakilex.levelplugin.npc.dialog;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.nakilex.levelplugin.npc.dialog.entry.DialogueEntry;
import me.nakilex.levelplugin.npc.dialog.model.DialogNpcRef;
import me.nakilex.levelplugin.npc.dialog.model.DialogueCriteria;
import me.nakilex.levelplugin.npc.dialog.model.InteractionContext;

/** Immutable dialogue graph with an ordered entry list retained for linear backwards compatibility. */
public final class DialogueDefinition {
    private final String id;
    private final String name;
    private final DialogNpcRef npc;
    private final int priority;
    private final List<DialogueCriteria> criteria;
    private final List<DialogueEntry> entries;
    private final Map<String, DialogueEntry> entriesById;
    private final Map<DialogueEntry, String> idsByEntry;
    private final String startEntryId;

    public DialogueDefinition(String id, String name, DialogNpcRef npc, int priority,
                              List<DialogueCriteria> criteria, List<DialogueEntry> entries) {
        this(id, name, npc, priority, criteria, entries, null);
    }

    public DialogueDefinition(String id, String name, DialogNpcRef npc, int priority,
                              List<DialogueCriteria> criteria, List<DialogueEntry> entries,
                              String startEntryId) {
        this.id = id;
        this.name = name;
        this.npc = npc;
        this.priority = priority;
        this.criteria = List.copyOf(criteria);
        this.entries = List.copyOf(entries);
        this.entriesById = buildEntryMap(this.entries);
        this.idsByEntry = buildReverseEntryMap(this.entriesById);
        this.startEntryId = normalizeStartEntryId(startEntryId);
    }

    private Map<String, DialogueEntry> buildEntryMap(List<DialogueEntry> orderedEntries) {
        Map<String, DialogueEntry> mappedEntries = new LinkedHashMap<>();
        for (int index = 0; index < orderedEntries.size(); index++) {
            DialogueEntry entry = orderedEntries.get(index);
            String entryId = isValidId(entry.getId()) ? entry.getId() : "entry_" + index;
            if (mappedEntries.putIfAbsent(entryId, entry) != null) {
                throw new IllegalArgumentException("Dialogue definition '" + id + "' contains duplicate entry ID '" + entryId + "'");
            }
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(mappedEntries));
    }

    private Map<DialogueEntry, String> buildReverseEntryMap(Map<String, DialogueEntry> mappedEntries) {
        Map<DialogueEntry, String> reverseEntries = new IdentityHashMap<>();
        mappedEntries.forEach((entryId, entry) -> reverseEntries.put(entry, entryId));
        return reverseEntries;
    }

    private String normalizeStartEntryId(String requestedStartEntryId) {
        if (!isValidId(requestedStartEntryId)) return entriesById.keySet().stream().findFirst().orElse(null);
        if (!entriesById.containsKey(requestedStartEntryId)) {
            throw new IllegalArgumentException("Dialogue definition '" + id + "' has unknown start entry ID '" + requestedStartEntryId + "'");
        }
        return requestedStartEntryId;
    }

    private boolean isValidId(String entryId) { return entryId != null && !entryId.isBlank(); }

    public String id() { return id; }
    public String name() { return name; }
    public DialogNpcRef npc() { return npc; }
    public int priority() { return priority; }
    public List<DialogueCriteria> criteria() { return criteria; }
    public List<DialogueEntry> entries() { return entries; }
    public Map<String, DialogueEntry> entriesById() { return entriesById; }
    public String startEntryId() { return startEntryId; }

    public Optional<DialogueEntry> getEntry(String entryId) { return Optional.ofNullable(entriesById.get(entryId)); }
    public Optional<DialogueEntry> getStartEntry() { return getEntry(startEntryId); }

    public Optional<DialogueEntry> getEntryAfter(String currentEntryId) {
        boolean foundCurrent = false;
        for (Map.Entry<String, DialogueEntry> entry : entriesById.entrySet()) {
            if (foundCurrent) return Optional.of(entry.getValue());
            foundCurrent = entry.getKey().equals(currentEntryId);
        }
        return Optional.empty();
    }

    public Optional<String> getEntryId(DialogueEntry entry) { return Optional.ofNullable(idsByEntry.get(entry)); }

    public boolean matches(InteractionContext context) {
        return criteria.stream().allMatch(criteria -> criteria.matches(context))
                && entries.stream().anyMatch(entry -> entry.matches(context));
    }
}
