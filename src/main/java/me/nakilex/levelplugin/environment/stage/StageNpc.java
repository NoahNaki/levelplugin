package me.nakilex.levelplugin.environment.stage;

import io.lumine.mythic.core.mobs.ActiveMob;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.mob.utils.MythicMobModifier;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.CurrentLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Shared representation of staged NPC spawns for towns and buildings. Handles parsing from
 * configuration, spawning either Citizens NPC clones or MythicMob entities and cleaning them up.
 */
public final class StageNpc {

    private StageNpc() {
    }

    /** Prefix used for scoreboard tags applied to spawned MythicMob NPCs. */
    public static final String SCOREBOARD_TAG_PREFIX = "stage_npc:";

    private static NamespacedKey stageNpcKey;

    private static NamespacedKey npcKey() {
        if (stageNpcKey == null) {
            JavaPlugin plugin = Main.getInstance();
            stageNpcKey = new NamespacedKey(plugin, "stage_npc_id");
        }
        return stageNpcKey;
    }

    /** Supported staged NPC types. */
    public enum Type {
        CITIZENS,
        MYTHIC_MOB;

        public static Type fromKey(String key) {
            return switch (key.toLowerCase(Locale.ROOT)) {
                case "citizen", "citizens" -> CITIZENS;
                case "mythic", "mythicmob", "mythic_mob" -> MYTHIC_MOB;
                default -> throw new IllegalArgumentException("Unknown staged NPC type: " + key);
            };
        }

        public String key() {
            return switch (this) {
                case CITIZENS -> "citizens";
                case MYTHIC_MOB -> "mythic";
            };
        }
    }

    /**
     * Immutable configuration for a staged NPC spawn.
     * <p>
     * {@code interactionId} represents the logical Citizens NPC id that should be used when the
     * mob is interacted with. For Citizens clones this defaults to the original NPC id. For
     * MythicMobs it can be set using the {@code @id} suffix in the configuration entry.
     */
    public record Definition(
            Type type,
            Integer citizenId,
            String mythicMobId,
            int interactionId,
            int x,
            int y,
            int z,
            float yaw,
            float pitch
    ) {

        public Definition {
            Objects.requireNonNull(type, "type");
        }

        /**
         * Parse a configuration string into a {@link Definition}.
         * <p>
         * Supports the historical plain Citizens id format as well as entries prefixed with a
         * type (e.g. {@code mythic:mob_id}). When a non-numeric identifier is supplied without a
         * prefix it is treated as a MythicMob id for convenience.
         */
        public static Definition parse(String raw) {
            if (raw == null || raw.isEmpty()) {
                throw new IllegalArgumentException("NPC definition cannot be empty");
            }
            String[] parts = raw.split(";");
            if (parts.length < 6) {
                throw new IllegalArgumentException("NPC definition requires 6 parts: " + raw);
            }

            int offsetX = parseInt(parts[1], "x");
            int offsetY = parseInt(parts[2], "y");
            int offsetZ = parseInt(parts[3], "z");
            float yaw = parseFloat(parts[4], "yaw");
            float pitch = parseFloat(parts[5], "pitch");

            String idPart = parts[0].trim();
            if (idPart.isEmpty()) {
                throw new IllegalArgumentException("NPC definition is missing an identifier");
            }

            Integer citizenId = null;
            String mythicMobId = null;
            int interactionId = -1;
            Type type;

            if (isInteger(idPart)) {
                citizenId = Integer.parseInt(idPart);
                interactionId = citizenId;
                type = Type.CITIZENS;
            } else {
                int atIdx = idPart.lastIndexOf('@');
                if (atIdx >= 0) {
                    String interaction = idPart.substring(atIdx + 1);
                    if (!interaction.isEmpty()) {
                        interactionId = parseInt(interaction, "interaction");
                    }
                    idPart = idPart.substring(0, atIdx);
                }

                int colonIdx = idPart.indexOf(':');
                if (colonIdx < 0) {
                    type = Type.MYTHIC_MOB;
                    mythicMobId = idPart;
                } else {
                    String typeKey = idPart.substring(0, colonIdx);
                    String identifier = idPart.substring(colonIdx + 1);
                    type = Type.fromKey(typeKey);
                    if (type == Type.CITIZENS) {
                        citizenId = parseInt(identifier, "citizenId");
                        if (interactionId < 0) interactionId = citizenId;
                    } else if (type == Type.MYTHIC_MOB) {
                        if (identifier.isEmpty()) {
                            throw new IllegalArgumentException("MythicMob identifier cannot be empty: " + raw);
                        }
                        mythicMobId = identifier;
                    }
                }
            }

            return new Definition(type, citizenId, mythicMobId, interactionId, offsetX, offsetY, offsetZ, yaw, pitch);
        }

        /** Serialize the definition into a configuration string. */
        public String serialize() {
            String idPart;
            if (type == Type.CITIZENS && mythicMobId == null) {
                idPart = String.valueOf(citizenId);
            } else {
                String identifier = switch (type) {
                    case CITIZENS -> String.valueOf(citizenId);
                    case MYTHIC_MOB -> mythicMobId;
                };
                idPart = type.key() + ":" + identifier;
                if (interactionId >= 0) {
                    idPart += "@" + interactionId;
                }
            }
            return idPart + ";" + x + ";" + y + ";" + z + ";" + yaw + ";" + pitch;
        }

        /** Spawn this definition at the given location. */
        public Instance spawn(Location location, Logger logger, String context) {
            return switch (type) {
                case CITIZENS -> spawnCitizens(location, logger, context);
                case MYTHIC_MOB -> spawnMythic(location, logger, context);
            };
        }

        private Instance spawnCitizens(Location location, Logger logger, String context) {
            if (citizenId == null) {
                warn(logger, "Citizens NPC id missing for staged spawn", context);
                return null;
            }
            NPC template = CitizensAPI.getNPCRegistry().getById(citizenId);
            if (template == null) {
                warn(logger, "NPC template with id " + citizenId + " not found", context);
                return null;
            }

            NPC clone = template.copy();
            clone.getOrAddTrait(CurrentLocation.class).setLocation(location);
            clone.spawn(location);
            Entity entity = clone.getEntity();
            if (entity != null) {
                entity.teleport(location, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
                entity.setGravity(false);
                if (entity instanceof LivingEntity living) {
                    living.setRemoveWhenFarAway(false);
                }
            }
            return new Instance(type, clone, null, entity != null ? entity.getUniqueId() : null, interactionId);
        }

        private Instance spawnMythic(Location location, Logger logger, String context) {
            if (mythicMobId == null || mythicMobId.isEmpty()) {
                warn(logger, "MythicMob id missing for staged spawn", context);
                return null;
            }
            ActiveMob mob = MythicMobModifier.spawnModifiedMob(mythicMobId, location, null, null, null, null);
            if (mob == null) {
                warn(logger, "Failed to spawn MythicMob '" + mythicMobId + "'", context);
                return null;
            }
            Entity entity = MythicMobModifier.toBukkitEntity(mob.getEntity());
            if (entity != null) {
                entity.teleport(location, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
                entity.setGravity(false);
                if (entity instanceof LivingEntity living) {
                    living.setRemoveWhenFarAway(false);
                    living.setAI(false);
                    living.setCollidable(false);
                }
                applyMetadata(entity, interactionId);
            }
            return new Instance(type, null, mob, entity != null ? entity.getUniqueId() : null, interactionId);
        }
    }

    /**
     * Runtime handle for a staged NPC. Keeps track of the underlying entity so it can be destroyed
     * when the stage is unloaded.
     */
    public record Instance(Type type, NPC citizensNpc, ActiveMob mythicMob, UUID entityId, int interactionId) {

        public void despawn() {
            if (type == Type.CITIZENS) {
                if (citizensNpc != null) {
                    if (citizensNpc.isSpawned()) citizensNpc.despawn();
                    citizensNpc.destroy();
                }
                return;
            }

            if (mythicMob != null) {
                mythicMob.despawn();
            }
            if (entityId != null) {
                Entity entity = Bukkit.getEntity(entityId);
                if (entity != null) entity.remove();
            }
        }
    }

    private static boolean isInteger(String value) {
        if (value.isEmpty()) return false;
        int start = value.charAt(0) == '-' ? 1 : 0;
        if (start == value.length()) return false;
        for (int i = start; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) return false;
        }
        return true;
    }

    private static int parseInt(String value, String label) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid " + label + " value: " + value, ex);
        }
    }

    private static float parseFloat(String value, String label) {
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid " + label + " value: " + value, ex);
        }
    }

    private static void applyMetadata(Entity entity, int interactionId) {
        if (entity == null || interactionId < 0) return;
        entity.addScoreboardTag(SCOREBOARD_TAG_PREFIX + interactionId);
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(npcKey(), PersistentDataType.INTEGER, interactionId);
        entity.setInvulnerable(true);
    }

    /**
     * Attempt to resolve the logical interaction id stored on a staged NPC entity.
     */
    public static Integer resolveInteractionId(Entity entity) {
        if (entity == null) return null;
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        Integer id = pdc.get(npcKey(), PersistentDataType.INTEGER);
        if (id != null) {
            return id;
        }
        for (String tag : entity.getScoreboardTags()) {
            if (tag.startsWith(SCOREBOARD_TAG_PREFIX)) {
                String raw = tag.substring(SCOREBOARD_TAG_PREFIX.length());
                try {
                    return Integer.parseInt(raw);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    private static void warn(Logger logger, String message, String context) {
        if (logger == null) return;
        if (context != null && !context.isEmpty()) {
            logger.warning("[StageNPC] " + message + " (" + context + ")");
        } else {
            logger.warning("[StageNPC] " + message);
        }
    }
}

