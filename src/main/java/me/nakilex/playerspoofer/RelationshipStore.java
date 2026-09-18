package me.nakilex.playerspoofer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

final class RelationshipStore {
    private static final String STORAGE_PATH = "chat-memory.relationship-records";

    private final PlayerSpooferPlugin plugin;
    private final Map<String, Relationship> relationships = new LinkedHashMap<>();
    private boolean dirty;

    RelationshipStore(PlayerSpooferPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    void load() {
        relationships.clear();
        for (String record : plugin.getConfig().getStringList(STORAGE_PATH)) {
            if (record == null || record.isBlank()) continue;
            String[] parts = record.split("\\|", -1);
            if (parts.length != 6) continue;
            try {
                String botKey = normalize(parts[0]);
                UUID uuid = UUID.fromString(parts[1]);
                String lastName = parts[2].isBlank() ? "unknown" : parts[2];
                int familiarity = Math.max(0, Math.min(100, Integer.parseInt(parts[3])));
                int messages = Math.max(0, Integer.parseInt(parts[4]));
                long lastSeen = Math.max(0L, Long.parseLong(parts[5]));
                Relationship relationship = new Relationship(botKey, uuid, lastName, familiarity, messages, lastSeen);
                relationships.put(key(botKey, uuid), relationship);
            } catch (IllegalArgumentException ignored) {
            }
        }
        dirty = false;
    }

    Relationship get(String botName, UUID playerUuid, String playerName) {
        String botKey = normalize(botName);
        String key = key(botKey, playerUuid);
        Relationship current = relationships.get(key);
        if (current != null) {
            if (playerName != null && !playerName.isBlank()) current.lastName = playerName;
            return current;
        }
        Relationship created = new Relationship(botKey, playerUuid,
                playerName == null || playerName.isBlank() ? "unknown" : playerName, 0, 0, 0L);
        relationships.put(key, created);
        return created;
    }

    void recordInteraction(String botName, UUID playerUuid, String playerName) {
        Relationship relationship = get(botName, playerUuid, playerName);
        relationship.messages++;
        relationship.familiarity = Math.min(100, relationship.familiarity + (relationship.messages <= 3 ? 4 : 2));
        relationship.lastSeen = System.currentTimeMillis();
        dirty = true;
    }

    String summary(String botName, UUID playerUuid, String playerName) {
        Relationship relationship = get(botName, playerUuid, playerName);
        if (relationship.messages == 0) return "You have not really talked with " + relationship.lastName + " before.";
        String level;
        if (relationship.familiarity >= 70) level = "you know them pretty well";
        else if (relationship.familiarity >= 35) level = "you recognize them and have chatted a few times";
        else level = "you vaguely recognize them";
        return level + "; you have exchanged about " + relationship.messages + " messages.";
    }

    Relationship findByPlayerName(String botName, String playerName) {
        String botKey = normalize(botName);
        return relationships.values().stream()
                .filter(r -> r.botKey.equals(botKey) && r.lastName.equalsIgnoreCase(playerName))
                .findFirst().orElse(null);
    }

    void flush() {
        if (!dirty) return;
        List<String> records = new ArrayList<>();
        for (Relationship relationship : relationships.values()) {
            records.add(relationship.botKey + "|" + relationship.playerUuid + "|" + sanitizeName(relationship.lastName)
                    + "|" + relationship.familiarity + "|" + relationship.messages + "|" + relationship.lastSeen);
        }
        // 1.6.1 used a nested ConfigurationSection here. Store flat records instead so the
        // plugin is ABI-safe on Paper 1.21.11 and can be compiled without treating
        // ConfigurationSection as a concrete class.
        plugin.getConfig().set("chat-memory.relationships", null);
        plugin.getConfig().set(STORAGE_PATH, records);
        plugin.saveConfig();
        dirty = false;
    }

    int size() { return relationships.size(); }

    static final class Relationship {
        final String botKey;
        final UUID playerUuid;
        String lastName;
        int familiarity;
        int messages;
        long lastSeen;

        Relationship(String botKey, UUID playerUuid, String lastName, int familiarity, int messages, long lastSeen) {
            this.botKey = botKey;
            this.playerUuid = playerUuid;
            this.lastName = lastName;
            this.familiarity = familiarity;
            this.messages = messages;
            this.lastSeen = lastSeen;
        }

        String describe() {
            long ageSeconds = lastSeen <= 0 ? -1 : Math.max(0L, (System.currentTimeMillis() - lastSeen) / 1000L);
            return lastName + ": familiarity=" + familiarity + "/100, messages=" + messages
                    + (ageSeconds < 0 ? ", never interacted" : ", last interaction=" + ageSeconds + "s ago");
        }
    }

    private static String sanitizeName(String value) {
        if (value == null || value.isBlank()) return "unknown";
        return value.replace("|", "");
    }

    private static String key(String botKey, UUID uuid) { return botKey + "|" + uuid; }
    private static String normalize(String value) { return value == null ? "" : value.toLowerCase(Locale.ROOT); }
}
