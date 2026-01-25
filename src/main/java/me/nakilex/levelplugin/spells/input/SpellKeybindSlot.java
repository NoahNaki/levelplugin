package me.nakilex.levelplugin.spells.input;

public enum SpellKeybindSlot {
    SLOT_1(1),
    SLOT_2(2),
    SLOT_3(3),
    SLOT_4(4);

    private final int index;

    SpellKeybindSlot(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public String getDisplayName() {
        return "Spell " + index;
    }
}
