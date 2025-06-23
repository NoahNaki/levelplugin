package me.nakilex.levelplugin.ego;

public enum EgoRarity {
    COMMON(1.0),
    UNCOMMON(1.1),
    RARE(1.25),
    EPIC(1.5),
    LEGENDARY(1.75),
    MYTHIC(2.0);

    private final double scale;

    EgoRarity(double scale) {
        this.scale = scale;
    }

    public double getScale() {
        return scale;
    }

    /** Returns the next higher rarity or this if at max. */
    public EgoRarity next() {
        int ord = this.ordinal();
        EgoRarity[] vals = values();
        if (ord + 1 >= vals.length) return this;
        return vals[ord + 1];
    }
}
