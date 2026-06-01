package me.nakilex.levelplugin.npc.dialog.engine;

import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

/** A dialogue sound id, including namespaced resource-pack sound ids. */
public record DialogueSound(String id, SoundCategory category, float volume, float pitch) {
    public DialogueSound {
        category = category == null ? SoundCategory.MASTER : category;
    }

    public void play(Player player) {
        if (player == null || id == null || id.isBlank()) return;
        player.playSound(player.getLocation(), id, category, volume, pitch);
    }
}
