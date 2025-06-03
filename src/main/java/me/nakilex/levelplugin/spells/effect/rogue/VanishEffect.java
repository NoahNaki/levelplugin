package me.nakilex.levelplugin.spells.effect.rogue;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Simplified vanish effect that hides the player for a few seconds and applies
 * agility based speed/jump buffs.
 */
public class VanishEffect implements SpellEffect {
    private static final Set<UUID> vanished = new HashSet<>();
    private static final Map<UUID, BukkitRunnable> tasks = new HashMap<>();

    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        Location origin = player.getLocation();
        World world = origin.getWorld();

        final int BASE_DURATION = 100;
        final int EXTEND = 20;
        int duration = BASE_DURATION;
        if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            PotionEffect old = player.getPotionEffect(PotionEffectType.INVISIBILITY);
            duration = old.getDuration() + EXTEND;
        }

        int agility = StatsManager.getInstance().getStatValue(player, StatsManager.StatType.AGI);
        boolean speed = agility > 100;
        boolean jump = agility > 250;
        int speedAmp = agility > 500 ? 1 : 0;
        int jumpAmp = agility > 500 ? 1 : 0;

        BukkitRunnable end = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                vanished.remove(player.getUniqueId());
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.equals(player)) p.showPlayer(Main.getInstance(), player);
                }
            }
        };
        end.runTaskLater(Main.getInstance(), duration);
        tasks.put(player.getUniqueId(), end);

        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration, 0, false, false));
        if (speed) player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, speedAmp, false, false));
        if (jump) player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, duration, jumpAmp, false, false));

        vanished.add(player.getUniqueId());
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(player)) p.hidePlayer(Main.getInstance(), player);
        }

        world.playSound(origin, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1f);
        world.playSound(origin, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 1.2f);
        world.spawnParticle(Particle.FIREWORK, origin, 30, 1, 1, 1, 0.05);
        world.spawnParticle(Particle.SMOKE, origin, 20, 0.5, 1, 0.5, 0.02);
    }
}
