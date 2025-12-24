package me.nakilex.levelplugin.fishing.core.action;

import me.nakilex.levelplugin.fishing.api.FishingContext;
import me.nakilex.levelplugin.fishing.api.action.Action;
import me.nakilex.levelplugin.fishing.core.FishingArgs;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Map;

public class SoundAction implements Action {
    @Override
    public void execute(FishingContext ctx, Map<String, Object> args) {
        Player player = ctx.getPlayer();
        if (player == null) {
            return;
        }
        String soundName = FishingArgs.getString(args, "sound");
        if (soundName == null) {
            return;
        }
        Sound sound;
        try {
            sound = Sound.valueOf(soundName.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return;
        }
        float volume = (float) FishingArgs.getDouble(args, "volume", 1.0);
        float pitch = (float) FishingArgs.getDouble(args, "pitch", 1.0);
        player.playSound(player.getLocation(), sound, volume, pitch);
    }
}
