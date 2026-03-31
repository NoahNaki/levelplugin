package me.nakilex.levelplugin.party.synergy;

import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.managers.PlayerClassManager;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Computes composition-based party synergy in a reusable way.
 */
public final class PartySynergyUtil {
    private PartySynergyUtil() {
    }

    public static PartySynergyProfile profile(Collection<Player> members) {
        if (members == null || members.isEmpty()) {
            return PartySynergyProfile.neutral();
        }
        Set<PlayerClass> classes = new HashSet<>();
        int size = 0;
        for (Player p : members) {
            if (p == null) {
                continue;
            }
            size++;
            classes.add(PlayerClassManager.getInstance().getPlayerClass(p));
        }
        if (size <= 1) {
            return PartySynergyProfile.neutral();
        }

        int diversity = classes.size();
        double multiplier = 1.0 + Math.min(0.12, diversity * 0.03);
        if (size >= 4 && diversity >= 3) {
            multiplier += 0.04;
        }
        String summary = "Synergy " + Math.round((multiplier - 1.0) * 100.0) + "%";
        return new PartySynergyProfile(multiplier, summary);
    }
}
