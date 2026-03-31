package me.nakilex.levelplugin.party.synergy;

public record PartySynergyProfile(double multiplier, String summary) {
    public static PartySynergyProfile neutral() {
        return new PartySynergyProfile(1.0, "No synergy bonus");
    }
}
