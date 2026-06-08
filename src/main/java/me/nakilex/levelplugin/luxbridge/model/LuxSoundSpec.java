package me.nakilex.levelplugin.luxbridge.model;

public record LuxSoundSpec(String id, String source, float volume, float pitch) {
    public static final LuxSoundSpec EMPTY = new LuxSoundSpec("", "MASTER", 1.0f, 1.0f);
}
