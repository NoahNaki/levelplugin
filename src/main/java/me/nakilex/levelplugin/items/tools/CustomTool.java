package me.nakilex.levelplugin.items.tools;

import java.util.UUID;

import org.bukkit.Material;

public class CustomTool {
    private final UUID uuid;
    private final String name;
    private final Material material;
    private final ToolTier tier;

    public CustomTool(UUID uuid, String name, Material material, ToolTier tier) {
        this.uuid = uuid;
        this.name = name;
        this.material = material;
        this.tier = tier;
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public Material getMaterial() { return material; }
    public ToolTier getTier() { return tier; }
}
