package me.nakilex.levelplugin.luxdialogues;

/**
 * Small value object for a LuxDialogues answer option created by LevelPlugin.
 */
public final class LuxNpcDialogueChoice {
    private final String id;
    private final String text;
    private final Runnable callback;

    public LuxNpcDialogueChoice(String id, String text, Runnable callback) {
        this.id = id;
        this.text = text;
        this.callback = callback;
    }

    public String id() {
        return id;
    }

    public String text() {
        return text;
    }

    public Runnable callback() {
        return callback;
    }
}
