package me.nakilex.levelplugin.items.tools;

public enum ToolDiscipline {
    MINING("Pickaxe", "Mining"),
    FARMING("Scythe", "Farming"),
    FISHING("Rod", "Fishing");

    private final String suffix;
    private final String requirementLabel;

    ToolDiscipline(String suffix, String requirementLabel) {
        this.suffix = suffix;
        this.requirementLabel = requirementLabel;
    }

    public String getSuffix() {
        return suffix;
    }

    public String getRequirementLabel() {
        return requirementLabel;
    }

    public int getLevel(org.bukkit.entity.Player viewer) {
        if (viewer == null) {
            return 0;
        }
        return switch (this) {
            case FARMING -> me.nakilex.levelplugin.player.farming.managers.FarmingManager.getInstance().getLevel(viewer);
            case FISHING -> me.nakilex.levelplugin.player.fishing.managers.FishingSkillManager.getInstance().getLevel(viewer);
            case MINING -> me.nakilex.levelplugin.player.mining.managers.MiningManager.getInstance().getLevel(viewer);
        };
    }
}
