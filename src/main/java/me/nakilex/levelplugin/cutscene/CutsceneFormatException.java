package me.nakilex.levelplugin.cutscene;

/** Exception thrown when a cutscene YAML file is invalid. */
public class CutsceneFormatException extends Exception {
    public CutsceneFormatException(String message) {
        super(message);
    }

    public CutsceneFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
