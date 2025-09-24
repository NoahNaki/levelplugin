package me.nakilex.levelplugin.cutscene.effects;

import me.nakilex.levelplugin.Main;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Utility helpers for playing {@link EffectSettings} targets. */
public final class CutsceneEffects {
    private CutsceneEffects() {}

    public static void play(Player player, EffectSettings settings, Main plugin) {
        play(player, settings, player != null ? player.getLocation() : null, plugin);
    }

    public static void play(Player player, EffectSettings settings, Location origin, Main plugin) {
        if (player == null || settings == null || settings.isEmpty()) {
            return;
        }
        if (settings.title() != null || settings.subtitle() != null) {
            String title = settings.title() == null ? "" : translate(settings.title());
            String subtitle = settings.subtitle() == null ? "" : translate(settings.subtitle());
            player.sendTitle(title, subtitle, 10, 40, 10);
        }
        if (settings.actionBar() != null) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent(translate(settings.actionBar())));
        }
        if (!settings.sounds().isEmpty()) {
            Location loc = origin != null ? origin : player.getLocation();
            for (EffectSettings.SoundCue cue : settings.sounds()) {
                player.playSound(loc, cue.sound(), cue.volume(), cue.pitch());
            }
        }
        if (!settings.particles().isEmpty() && origin != null && origin.getWorld() != null) {
            for (EffectSettings.ParticleCue cue : settings.particles()) {
                origin.getWorld().spawnParticle(cue.particle(), origin,
                        cue.count(), cue.offsetX(), cue.offsetY(), cue.offsetZ());
            }
        }
        if (!settings.commands().isEmpty()) {
            for (String command : settings.commands()) {
                String parsed = command.replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
            }
        }
    }

    private static String translate(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
