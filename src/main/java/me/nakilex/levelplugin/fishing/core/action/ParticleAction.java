package me.nakilex.levelplugin.fishing.core.action;

import me.nakilex.levelplugin.fishing.api.FishingContext;
import me.nakilex.levelplugin.fishing.api.action.Action;
import me.nakilex.levelplugin.fishing.core.FishingArgs;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.Map;

public class ParticleAction implements Action {
    @Override
    public void execute(FishingContext ctx, Map<String, Object> args) {
        Player player = ctx.getPlayer();
        if (player == null) {
            return;
        }
        String particleName = FishingArgs.getString(args, "particle");
        if (particleName == null) {
            return;
        }
        Particle particle;
        try {
            particle = Particle.valueOf(particleName.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return;
        }
        int count = Math.max(1, FishingArgs.getInt(args, "count", 1));
        double offsetX = FishingArgs.getDouble(args, "offset_x", 0.0);
        double offsetY = FishingArgs.getDouble(args, "offset_y", 0.0);
        double offsetZ = FishingArgs.getDouble(args, "offset_z", 0.0);
        double extra = FishingArgs.getDouble(args, "extra", 0.0);
        player.getWorld().spawnParticle(particle, player.getLocation(), count, offsetX, offsetY, offsetZ, extra);
    }
}
