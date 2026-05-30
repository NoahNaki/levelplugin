package me.nakilex.levelplugin.npc.dialog.model;

/** Common typed values shared by dialogue messengers. */
public final class ContextKeys {
    public static final ContextKey<Integer> SELECTED_OPTION = new ContextKey<>("selected_option", Integer.class);
    public static final ContextKey<String> INPUT_TEXT = new ContextKey<>("input_text", String.class);
    public static final ContextKey<Number> INPUT_NUMBER = new ContextKey<>("input_number", Number.class);

    private ContextKeys() {
    }
}
