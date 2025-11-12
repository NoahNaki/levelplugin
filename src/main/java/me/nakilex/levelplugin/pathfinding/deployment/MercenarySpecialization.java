package me.nakilex.levelplugin.pathfinding.deployment;

import me.nakilex.levelplugin.pathfinding.npc.ArcherMercenary;
import me.nakilex.levelplugin.pathfinding.npc.MageMercenary;
import me.nakilex.levelplugin.pathfinding.npc.PathNpc;
import me.nakilex.levelplugin.pathfinding.npc.RogueMercenary;
import me.nakilex.levelplugin.pathfinding.npc.WarriorMercenary;
import org.bukkit.ChatColor;
import org.bukkit.Material;

/**
 * Enumeration of the default mercenary archetypes.  Each entry stores the
 * associated {@link PathNpc} implementation, a display label, and a fallback
 * icon material for GUI usage so the deployment board can present consistent
 * visuals without hard-coding strings in multiple places.
 */
public enum MercenarySpecialization {

    ROGUE("Shadow Duelist", ChatColor.DARK_PURPLE + "Shadow Duelist", Material.NETHERITE_SWORD, RogueMercenary.class),
    WARRIOR("Vanguard", ChatColor.GOLD + "Vanguard", Material.NETHERITE_AXE, WarriorMercenary.class),
    MAGE("Arcane Savant", ChatColor.AQUA + "Arcane Savant", Material.BLAZE_ROD, MageMercenary.class),
    ARCHER("Sky Stalker", ChatColor.GREEN + "Sky Stalker", Material.BOW, ArcherMercenary.class);

    private final String id;
    private final String displayName;
    private final Material icon;
    private final Class<? extends PathNpc> profileClass;

    MercenarySpecialization(String id, String displayName, Material icon, Class<? extends PathNpc> profileClass) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.profileClass = profileClass;
    }

    /** Machine readable identifier used for persistence. */
    public String id() {
        return id;
    }

    /** Human readable name shown to players. */
    public String displayName() {
        return displayName;
    }

    /** Icon material used when rendering this specialization in a GUI. */
    public Material icon() {
        return icon;
    }

    /** Pathfinding profile backing this specialization. */
    public Class<? extends PathNpc> profileClass() {
        return profileClass;
    }

    /** Resolve a specialization from a stored identifier. */
    public static MercenarySpecialization fromId(String id) {
        for (MercenarySpecialization spec : values()) {
            if (spec.id.equalsIgnoreCase(id)) {
                return spec;
            }
        }
        return null;
    }
}
