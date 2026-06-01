package me.nakilex.levelplugin.npc.dialog.engine;

/** A parsed dialogue source line. Speaker may be null when the NPC default should be used. */
public record DialogueLine(String speaker, String text) {
    public static DialogueLine parse(String raw) {
        String safe = raw == null ? "" : raw;
        int separator = safe.indexOf('|');
        if (separator < 0) return new DialogueLine(null, safe);
        return new DialogueLine(safe.substring(0, separator), safe.substring(separator + 1));
    }
}
