package me.nakilex.levelplugin.pet.data;

public enum PetVisibility {
    ALL,
    OWN,
    NONE;

    public PetVisibility next(boolean forward) {
        PetVisibility[] values = values();
        int index = ordinal() + (forward ? 1 : -1);
        if (index >= values.length) {
            index = 0;
        }
        if (index < 0) {
            index = values.length - 1;
        }
        return values[index];
    }
}
