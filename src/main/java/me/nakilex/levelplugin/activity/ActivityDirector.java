package me.nakilex.levelplugin.activity;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Aggregates cross-system activity hints for HUD/placeholders.
 */
public final class ActivityDirector {
    private final Main plugin;

    public ActivityDirector(Main plugin) {
        this.plugin = plugin;
    }

    public String nextActivity(Player player) {
        if (player == null) {
            return "";
        }
        List<ActivitySignal> signals = new ArrayList<>();

        QuestManager qm = plugin.getQuestManager();
        if (qm != null) {
            String tracked = qm.getTrackedQuest(player.getUniqueId());
            if (tracked != null && !tracked.isBlank()) {
                signals.add(new ActivitySignal("tracked_quest", 100, "Track: " + tracked));
            }
        }

        if (plugin.getMercenaryExpeditionManager() != null
                && plugin.getMercenaryExpeditionManager().getPendingRewards(player.getUniqueId()) != null) {
            signals.add(new ActivitySignal("expedition_claim", 120, "Claim mercenary expedition rewards"));
        }

        if (plugin.getGuildSiegeManager() != null && plugin.getGuildSiegeManager().getQueueSize() > 0) {
            signals.add(new ActivitySignal("siege_queue", 80, "Guild siege signup is active"));
        }

        if (signals.isEmpty()) {
            return "Explore quests and dungeons";
        }
        signals.sort(Comparator.comparingInt(ActivitySignal::priority).reversed());
        return signals.get(0).text();
    }
}
